import app.retra.core.achievements.AchievementEngine
import app.retra.core.achievements.AchievementEvent
import app.retra.core.achievements.AchievementEventType
import app.retra.core.achievements.AchievementIntegrity
import app.retra.core.achievements.RetraAchievements
import app.retra.core.multiplayer.MultiplayerCompatibility
import app.retra.core.multiplayer.MultiplayerCompatibilityGate
import app.retra.core.multiplayer.MultiplayerPacket
import app.retra.core.multiplayer.MultiplayerLanHost
import app.retra.core.multiplayer.MultiplayerSocketConnection
import app.retra.core.multiplayer.MultiplayerPacketCodec
import app.retra.core.multiplayer.MultiplayerPacketType
import app.retra.core.multiplayer.OrderedPacketBuffer
import app.retra.core.multiplayer.RoomCode
import app.retra.core.social.FriendCode
import app.retra.core.social.PlayerProfile
import app.retra.core.social.SharePrivacy
import app.retra.core.social.SocialShareFactory
import app.retra.core.emulation.AtomicSaveStore
import app.retra.core.catalog.CatalogManifestJson
import app.retra.core.catalog.InvalidCatalogManifestException
import app.retra.core.download.CatalogDownloadPolicy
import app.retra.core.download.DownloadResponseMetadata
import app.retra.core.download.UnsafeDownloadException
import app.retra.core.cheats.CheatConflictAnalyzer
import app.retra.core.cheats.CheatProfile
import app.retra.core.cheats.InvalidCheatCatalogException
import app.retra.core.cheats.InvalidCheatPackException
import app.retra.core.cheats.RetraCheatCatalogParser
import app.retra.core.cheats.RetraCodesParser
import app.retra.core.cheats.RetroArchCheatParser
import app.retra.core.emulation.EmulatorButton
import app.retra.core.emulation.InputSnapshot
import app.retra.core.emulation.SaveEnvelope
import app.retra.core.emulation.RewindBuffer
import app.retra.core.emulation.SaveKind
import app.retra.core.emulation.SessionCommand
import app.retra.core.emulation.SessionPhase
import app.retra.core.emulation.SessionReducer
import app.retra.core.emulation.SessionSnapshot
import app.retra.core.model.CatalogEntry
import app.retra.core.model.CatalogManifest
import app.retra.core.model.CompatibilityStatus
import app.retra.core.rom.CatalogValidationResult
import app.retra.core.rom.CatalogValidator
import app.retra.core.rom.DuplicateDetector
import app.retra.core.rom.GbaRomParser
import app.retra.core.rom.InvalidRomException
import app.retra.core.rom.Sha256
import app.retra.core.rom.Sha1
import app.retra.core.rom.LibretroDatParser
import app.retra.core.patching.InvalidPatchException
import app.retra.core.patching.PatchEngine
import java.io.ByteArrayOutputStream
import java.util.zip.CRC32
import java.nio.file.Files

private var passed = 0
private var failed = 0

fun main() {
    test("valid GBA header parsing") {
        val rom = syntheticRom("RETRA TEST", "RTRE")
        val header = GbaRomParser.parse(rom)
        check(header.title == "RETRA TEST")
        check(header.gameCode == "RTRE")
        check(header.makerCode == "01")
        check(header.fixedValueValid)
        check(header.headerChecksumValid)
    }

    test("invalid fixed byte rejection") {
        val rom = syntheticRom("BROKEN", "BRKN")
        rom[0xB2] = 0
        val rejected = runCatching { GbaRomParser.parse(rom) }.exceptionOrNull()
        check(rejected is InvalidRomException)
    }

    test("SHA-256 known vector") {
        check(Sha256.of("abc".encodeToByteArray()) == "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
    }

    test("SHA-1 known vector") {
        check(Sha1.of("abc".encodeToByteArray()) == "a9993e364706816aba3e25717850c26c9cd0d89d")
    }

    test("Libretro DAT exact checksum match") {
        val dat = """
            clrmamepro ( name "Retra fixture" )
            game (
              name "Fixture Quest (USA)"
              rom ( name "Fixture Quest (USA).gba" size 1024 crc A1B2C3D4 md5 0123456789ABCDEF0123456789ABCDEF sha1 0123456789ABCDEF0123456789ABCDEF01234567 status verified )
            )
        """.trimIndent()
        val index = LibretroDatParser.parse(dat)
        val match = index.match("0123456789abcdef0123456789abcdef01234567", 0xA1B2C3D4, 1024)
        check(match?.canonicalTitle == "Fixture Quest (USA)")
        check(index.match("f".repeat(40), 0xA1B2C3D4, 2048) == null)
    }

    test("RetroArch cheat import is converted and ROM-bound") {
        val source = """
            cheats = 2
            cheat0_desc = "Infinite money"
            cheat0_code = "82000000 03E7"
            cheat0_enable = false
            cheat1_desc = "Walk through walls"
            cheat1_code = "12345678 9ABCDEF0+11112222 33334444"
            cheat1_enable = false
        """.trimIndent().encodeToByteArray()
        val pack = RetroArchCheatParser.parseForGame(source, "Fixture provider", "a".repeat(64), "RTRE", 0)
        check(pack.gameSha256 == "a".repeat(64))
        check(pack.cheats.size == 2)
        check(pack.cheats.first().name == "Infinite money")
        val reparsed = RetraCodesParser.parse(RetroArchCheatParser.encodeRetraCodes(pack))
        check(reparsed.gameSha256 == "a".repeat(64))
        check(reparsed.cheats.size == 2)
    }

    test("RetroArch import skips placeholder cheats but keeps concrete codes") {
        val source = """
            cheats = 2
            cheat0_desc = "Runtime-selected encounter"
            cheat0_code = "D0000000 ????"
            cheat0_enable = false
            cheat1_desc = "Concrete code"
            cheat1_code = "82000000 03E7"
            cheat1_enable = false
        """.trimIndent().encodeToByteArray()
        val pack = RetroArchCheatParser.parseForGame(source, "Fixture provider", "b".repeat(64))
        check(pack.cheats.size == 1)
        check(pack.cheats.single().name == "Concrete code")
    }

    test("case-insensitive duplicate detection") {
        val hash = "A".repeat(64)
        check(DuplicateDetector.isDuplicate(hash, listOf("a".repeat(64))))
        check(!DuplicateDetector.isDuplicate(hash, listOf("b".repeat(64))))
    }

    test("authorized HTTPS catalog validation") {
        check(CatalogValidator.validate(validCatalog()) is CatalogValidationResult.Valid)
    }

    test("unsafe HTTP catalog rejection") {
        val bad = validCatalog().copy(
            games = validCatalog().games.map { it.copy(downloadUrl = "http://example.com/test.gba") }
        )
        val result = CatalogValidator.validate(bad)
        check(result is CatalogValidationResult.Invalid)
        check(result.reasons.any { it.contains("HTTPS") })
    }

    test("missing distribution permission rejection") {
        val bad = validCatalog().copy(
            games = validCatalog().games.map { it.copy(distributionPermission = "") }
        )
        check(CatalogValidator.validate(bad) is CatalogValidationResult.Invalid)
    }

    test("input bitmask press and release") {
        val pressed = InputSnapshot().with(EmulatorButton.A, true).with(EmulatorButton.LEFT, true)
        check(pressed.isPressed(EmulatorButton.A))
        check(pressed.isPressed(EmulatorButton.LEFT))
        check(!pressed.isPressed(EmulatorButton.B))
        check(!pressed.with(EmulatorButton.A, false).isPressed(EmulatorButton.A))
    }

    test("session reducer legal lifecycle") {
        var state = SessionSnapshot()
        state = SessionReducer.reduce(state, SessionCommand.BeginLoad("a".repeat(64)))
        check(state.phase == SessionPhase.LOADING)
        state = SessionReducer.reduce(state, SessionCommand.LoadSucceeded)
        check(state.phase == SessionPhase.READY)
        state = SessionReducer.reduce(state, SessionCommand.Start)
        check(state.phase == SessionPhase.RUNNING)
        state = SessionReducer.reduce(state, SessionCommand.Suspend)
        check(state.phase == SessionPhase.SUSPENDED)
        state = SessionReducer.reduce(state, SessionCommand.Resume)
        check(state.phase == SessionPhase.RUNNING)
    }

    test("save envelope round trip") {
        val payload = "deterministic-state".encodeToByteArray()
        val original = SaveEnvelope(SaveKind.STATE, "a".repeat(64), "test-core", "1.0", 3, 123456789L, payload)
        val decoded = SaveEnvelope.decode(original.encode())
        check(decoded.kind == SaveKind.STATE)
        check(decoded.slot == 3)
        check(decoded.createdAtEpochMillis == 123456789L)
        check(decoded.payload.contentEquals(payload))
    }

    test("save envelope corruption rejection") {
        val encoded = SaveEnvelope(SaveKind.SUSPEND, "b".repeat(64), "test-core", "1.0", -1, 1L, byteArrayOf(1, 2, 3)).encode()
        encoded[encoded.lastIndex] = 9
        check(runCatching { SaveEnvelope.decode(encoded) }.isFailure)
    }

    test("atomic save and rotating backup") {
        val root = Files.createTempDirectory("retra-save-test").toFile()
        try {
            val store = AtomicSaveStore(root, backupCount = 3)
            store.write("hash/states/slot-0.rsv", byteArrayOf(1))
            store.write("hash/states/slot-0.rsv", byteArrayOf(2))
            store.write("hash/states/slot-0.rsv", byteArrayOf(3))
            check(store.read("hash/states/slot-0.rsv")!!.contentEquals(byteArrayOf(3)))
            check(root.resolve("hash/states/slot-0.rsv.bak1").readBytes().contentEquals(byteArrayOf(2)))
            check(root.resolve("hash/states/slot-0.rsv.bak2").readBytes().contentEquals(byteArrayOf(1)))
        } finally {
            root.deleteRecursively()
        }
    }

    test("save path traversal rejection") {
        val root = Files.createTempDirectory("retra-path-test").toFile()
        try {
            val store = AtomicSaveStore(root)
            check(runCatching { store.write("../escape.bin", byteArrayOf(1)) }.isFailure)
        } finally {
            root.deleteRecursively()
        }
    }

    test("rewind buffer is bounded and steps backward") {
        val buffer = RewindBuffer(maximumBytes = 9)
        check(buffer.push(byteArrayOf(1, 1, 1)))
        check(buffer.push(byteArrayOf(2, 2, 2)))
        check(buffer.push(byteArrayOf(3, 3, 3)))
        check(buffer.push(byteArrayOf(4, 4, 4)))
        check(buffer.byteCount <= 9)
        check(buffer.snapshotCount == 3)
        val restored = buffer.rewind(1)
        check(restored.contentEquals(byteArrayOf(3, 3, 3)))
        restored[0] = 99
        check(buffer.rewind(1).contentEquals(byteArrayOf(2, 2, 2)))
        buffer.clear()
        check(buffer.snapshotCount == 0 && buffer.byteCount == 0)
    }


    test("IPS literal and RLE patch application") {
        val source = syntheticRom("PATCH BASE", "PTCH")
        val patch = ipsPatch(
            literalOffset = 0x200,
            literal = byteArrayOf(1, 2, 3),
            rleOffset = 0x300,
            rleCount = 4,
            rleValue = 0x7F
        )
        val output = PatchEngine.apply(source, patch).output
        check(output.copyOfRange(0x200, 0x203).contentEquals(byteArrayOf(1, 2, 3)))
        check(output.copyOfRange(0x300, 0x304).contentEquals(ByteArray(4) { 0x7F }))
    }

    test("UPS patch CRC and XOR application") {
        val source = syntheticRom("UPS BASE", "UPSB")
        val target = source.copyOf().also {
            it[0x240] = (it[0x240].toInt() xor 0x11).toByte()
            it[0x241] = (it[0x241].toInt() xor 0x22).toByte()
        }
        check(PatchEngine.apply(source, upsPatch(source, target, 0x240, 2)).output.contentEquals(target))
    }

    test("BPS TargetRead patch application") {
        val source = syntheticRom("BPS BASE", "BPSB")
        val target = source.copyOf().also { it[0x260] = 0x55; it[0x261] = 0x66 }
        check(PatchEngine.apply(source, bpsTargetReadPatch(source, target)).output.contentEquals(target))
    }

    test("patch corruption rejection") {
        val source = syntheticRom("CRC BASE", "CRCB")
        val target = source.copyOf().also { it[0x280] = 0x44 }
        val patch = bpsTargetReadPatch(source, target)
        patch[patch.lastIndex] = (patch.last().toInt() xor 0x01).toByte()
        check(runCatching { PatchEngine.apply(source, patch) }.exceptionOrNull() is InvalidPatchException)
    }


    test("Retra Codes declarative pack parsing") {
        val pack = RetraCodesParser.parse(validCheatPack().encodeToByteArray())
        check(pack.provider == "Retra Verification")
        check(pack.cheats.size == 2)
        check(pack.cheats.first().codeLines == listOf("02000000:4:00000064"))
    }

    test("Retra Codes exact ROM matching") {
        val pack = RetraCodesParser.parse(validCheatPack().encodeToByteArray())
        check(RetraCodesParser.match(pack, "a".repeat(64), "RTRE", 0).compatible)
        check(!RetraCodesParser.match(pack, "b".repeat(64), "RTRE", 0).compatible)
    }

    test("Retra Codes raw-write conflict detection") {
        val pack = RetraCodesParser.parse(validCheatPack().encodeToByteArray())
        val validation = CheatConflictAnalyzer.validate(pack, CheatProfile("Conflict", setOf("money-max", "money-low")))
        check(!validation.valid)
        check(validation.errors.any { it.contains("write different values") })
    }

    test("Retra Codes rejects executable text") {
        val unsafe = validCheatPack().replace("02000000:4:00000064", "javascript:alert(1)")
        check(runCatching { RetraCodesParser.parse(unsafe.encodeToByteArray()) }.exceptionOrNull() is InvalidCheatPackException)
    }

    test("Retra cheat index parsing and exact match") {
        val catalog = RetraCheatCatalogParser.parse(validCheatIndex().encodeToByteArray())
        check(catalog.catalogId == "verification-index")
        check(catalog.entries.size == 1)
        val entry = catalog.entries.single()
        check(RetraCheatCatalogParser.matches(entry, "a".repeat(64), "RTRE", 0))
        check(!RetraCheatCatalogParser.matches(entry, "b".repeat(64), "RTRE", 0))
    }

    test("Retra cheat index rejects insecure pack URL") {
        val unsafe = validCheatIndex().replace("https://example.com/retra-verification.rcc", "http://example.com/retra-verification.rcc")
        check(runCatching { RetraCheatCatalogParser.parse(unsafe.encodeToByteArray()) }.exceptionOrNull() is InvalidCheatCatalogException)
    }

    test("Retra cheat index rejects duplicate fields") {
        val duplicate = validCheatIndex().replace("name=Retra Verification Index", "name=Retra Verification Index\nname=Duplicate")
        check(runCatching { RetraCheatCatalogParser.parse(duplicate.encodeToByteArray()) }.exceptionOrNull() is InvalidCheatCatalogException)
    }


    test("catalog download HTTPS and response policy") {
        val entry = validCatalog().games.single()
        CatalogDownloadPolicy.validateEntry(entry)
        CatalogDownloadPolicy.validateResponse(
            entry,
            DownloadResponseMetadata("application/octet-stream", entry.fileSize, "identity")
        )
        CatalogDownloadPolicy.validateCompletedSize(entry, entry.fileSize)
    }

    test("catalog download rejects cross-host redirects") {
        val origin = CatalogDownloadPolicy.validateEntry(validCatalog().games.single())
        val error = runCatching {
            CatalogDownloadPolicy.validateRedirect(origin, origin, "https://cdn.invalid/test.gba", setOf(origin))
        }.exceptionOrNull()
        check(error is UnsafeDownloadException)
    }

    test("catalog download rejects metadata length mismatch") {
        val entry = validCatalog().games.single()
        val error = runCatching {
            CatalogDownloadPolicy.validateResponse(
                entry,
                DownloadResponseMetadata("application/octet-stream", entry.fileSize + 1, "identity")
            )
        }.exceptionOrNull()
        check(error is UnsafeDownloadException)
    }

    test("restricted JSON catalog manifest parsing") {
        val manifest = CatalogManifestJson.parse(validCatalogJson().encodeToByteArray())
        check(manifest.catalogId == "retra-verification")
        check(manifest.games.single().title == "Synthetic Test")
        check(manifest.games.single().compatibility == CompatibilityStatus.UNKNOWN)
    }

    test("catalog JSON rejects unknown executable fields") {
        val unsafe = validCatalogJson().replace(
            "\"compatibility\": \"UNKNOWN\"",
            "\"compatibility\": \"UNKNOWN\", \"script\": \"run()\""
        )
        val error = runCatching { CatalogManifestJson.parse(unsafe.encodeToByteArray()) }.exceptionOrNull()
        check(error is InvalidCatalogManifestException)
    }

    test("catalog JSON rejects duplicate keys") {
        val unsafe = validCatalogJson().replace(
            "\"catalogId\": \"retra-verification\"",
            "\"catalogId\": \"retra-verification\", \"catalogId\": \"shadow\""
        )
        val error = runCatching { CatalogManifestJson.parse(unsafe.encodeToByteArray()) }.exceptionOrNull()
        check(error is InvalidCatalogManifestException)
    }

    test("catalog download blocks local network targets") {
        val entry = validCatalog().games.single().copy(downloadUrl = "https://127.0.0.1/test.gba")
        val error = runCatching { CatalogDownloadPolicy.validateEntry(entry) }.exceptionOrNull()
        check(error is UnsafeDownloadException)
    }

    test("catalog download rejects unsafe entry IDs") {
        val entry = validCatalog().games.single().copy(id = "../escape")
        val error = runCatching { CatalogDownloadPolicy.validateEntry(entry) }.exceptionOrNull()
        check(error is UnsafeDownloadException)
    }

    test("achievement counter unlock and integrity gate") {
        val first = RetraAchievements.builtIns.first { it.id == "library.first-memory" }
        val unlocked = AchievementEngine.evaluate(
            first,
            null,
            AchievementEvent(AchievementEventType.GAME_IMPORTED, uniqueKey = "a".repeat(64), occurredAtEpochMillis = 100L),
            AchievementIntegrity()
        )
        check(unlocked.newlyUnlocked)
        check(unlocked.progress.unlockedAtEpochMillis == 100L)
        val oldFriend = RetraAchievements.builtIns.first { it.id == "playtime.old-friend" }
        val blocked = AchievementEngine.evaluate(
            oldFriend,
            null,
            AchievementEvent(AchievementEventType.PLAY_SECONDS, amount = 36_000, occurredAtEpochMillis = 200L),
            AchievementIntegrity(cheatsActive = true)
        )
        check(!blocked.eligible && !blocked.newlyUnlocked)
    }

    test("achievement unique progress ignores duplicates") {
        val curator = RetraAchievements.builtIns.first { it.id == "library.curator" }
        val first = AchievementEngine.evaluate(curator, null, AchievementEvent(AchievementEventType.GAME_IMPORTED, uniqueKey = "same", occurredAtEpochMillis = 1), AchievementIntegrity())
        val second = AchievementEngine.evaluate(curator, first.progress, AchievementEvent(AchievementEventType.GAME_IMPORTED, uniqueKey = "same", occurredAtEpochMillis = 2), AchievementIntegrity())
        check(second.progress.uniqueKeys.size == 1)
    }

    test("social friend code and privacy-safe share") {
        val friendCode = FriendCode.fromProfileId("local-player")
        check(FriendCode.isValid(friendCode))
        val profile = PlayerProfile("local-player", "Seven", friendCode = friendCode)
        val card = SocialShareFactory.achievement(profile, "First Memory", "Imported a game.", 10, SharePrivacy.SUMMARY, "retra://achievement/library.first-memory")
        check(card.body.contains("First Memory"))
        check(!card.body.contains(friendCode))
    }

    test("multiplayer compatibility exact identity") {
        val host = MultiplayerCompatibility(1, "a".repeat(64), "mgba-libretro", "0.10.5", maxPlayers = 2)
        check(MultiplayerCompatibilityGate.compare(host, host.copy()).compatible)
        check(!MultiplayerCompatibilityGate.compare(host, host.copy(romSha256 = "b".repeat(64))).compatible)
    }

    test("multiplayer packet CRC and ordered buffer") {
        val code = RoomCode.normalize("ABC234")
        val packet0 = MultiplayerPacket(MultiplayerPacketType.LINK_DATA, code, 0, 0, byteArrayOf(1, 2))
        val packet1 = MultiplayerPacket(MultiplayerPacketType.LINK_DATA, code, 1, 1, byteArrayOf(3, 4))
        check(MultiplayerPacketCodec.decode(MultiplayerPacketCodec.encode(packet0)).payload.contentEquals(packet0.payload))
        val buffer = OrderedPacketBuffer()
        check(buffer.offer(packet1).isEmpty())
        val ready = buffer.offer(packet0)
        check(ready.map { it.sequence } == listOf(0L, 1L))
        val corrupted = MultiplayerPacketCodec.encode(packet0).also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
        check(runCatching { MultiplayerPacketCodec.decode(corrupted) }.isFailure)
    }


    test("multiplayer LAN transport loopback framing") {
        val host = MultiplayerLanHost()
        try {
            val code = "ABC234"
            val expected = MultiplayerPacket(MultiplayerPacketType.HELLO, code, 0, 0, "hello".encodeToByteArray())
            var serverReceived: MultiplayerPacket? = null
            val serverThread = Thread {
                host.accept().use { connection ->
                    serverReceived = connection.receive()
                    connection.send(MultiplayerPacket(MultiplayerPacketType.ACCEPT, code, 1, 0, "ok".encodeToByteArray()))
                }
            }
            serverThread.start()
            MultiplayerSocketConnection.connect("127.0.0.1", host.localPort).use { client ->
                client.send(expected)
                val response = client.receive()
                check(response.type == MultiplayerPacketType.ACCEPT)
                check(response.payload.decodeToString() == "ok")
            }
            serverThread.join(5_000)
            check(!serverThread.isAlive)
            check(serverReceived?.payload?.decodeToString() == "hello")
        } finally {
            host.close()
        }
    }

    println("RESULT: $passed passed, $failed failed")
    if (failed > 0) error("Core verification failed")
}

private fun test(name: String, block: () -> Unit) {
    try {
        block()
        passed++
        println("PASS: $name")
    } catch (error: Throwable) {
        failed++
        println("FAIL: $name -> ${error.message}")
    }
}

private fun syntheticRom(title: String, gameCode: String): ByteArray {
    val bytes = ByteArray(1024 * 1024)
    title.encodeToByteArray().copyInto(bytes, destinationOffset = 0xA0, endIndex = title.length.coerceAtMost(12))
    gameCode.encodeToByteArray().copyInto(bytes, destinationOffset = 0xAC, endIndex = gameCode.length.coerceAtMost(4))
    "01".encodeToByteArray().copyInto(bytes, destinationOffset = 0xB0)
    bytes[0xB2] = 0x96.toByte()
    bytes[0xBC] = 0
    bytes[0xBD] = GbaRomParser.calculateHeaderChecksum(bytes).toByte()
    return bytes
}

private fun validCatalog(): CatalogManifest = CatalogManifest(
    catalogVersion = 1,
    catalogId = "retra-verification",
    name = "Retra Verification",
    description = "Synthetic test catalog.",
    owner = "Retra",
    sourceUrl = "https://example.com/catalog.json",
    contentPolicy = "AUTHORIZED_ONLY",
    games = listOf(
        CatalogEntry(
            id = "test",
            title = "Synthetic Test",
            description = "Not a commercial game.",
            creator = "Retra",
            version = "1.0",
            downloadUrl = "https://example.com/test.gba",
            sha256 = "a".repeat(64),
            fileSize = 1024,
            license = "CC0-1.0",
            distributionPermission = "Redistribution permitted by creator.",
            tags = listOf("test"),
            compatibility = CompatibilityStatus.UNKNOWN
        )
    )
)

private fun validCatalogJson(): String = """
    {
      "catalogVersion": 1,
      "catalogId": "retra-verification",
      "name": "Retra Verification",
      "description": "Synthetic test catalog.",
      "owner": "Retra",
      "sourceUrl": "https://example.com/catalog.json",
      "contentPolicy": "AUTHORIZED_ONLY",
      "games": [
        {
          "id": "test",
          "title": "Synthetic Test",
          "description": "Not a commercial game.",
          "creator": "Retra",
          "version": "1.0",
          "downloadUrl": "https://example.com/test.gba",
          "sha256": "${"a".repeat(64)}",
          "fileSize": 1024,
          "license": "CC0-1.0",
          "distributionPermission": "Redistribution permitted by creator.",
          "tags": ["test"],
          "compatibility": "UNKNOWN"
        }
      ]
    }
""".trimIndent()

private fun ipsPatch(
    literalOffset: Int,
    literal: ByteArray,
    rleOffset: Int,
    rleCount: Int,
    rleValue: Int
): ByteArray = ByteArrayOutputStream().apply {
    write("PATCH".encodeToByteArray())
    writeBig24(literalOffset)
    writeBig16(literal.size)
    write(literal)
    writeBig24(rleOffset)
    writeBig16(0)
    writeBig16(rleCount)
    write(rleValue)
    write("EOF".encodeToByteArray())
}.toByteArray()

private fun upsPatch(source: ByteArray, target: ByteArray, offset: Int, length: Int): ByteArray {
    val body = ByteArrayOutputStream().apply {
        write("UPS1".encodeToByteArray())
        writeVariable(source.size.toLong())
        writeVariable(target.size.toLong())
        writeVariable(offset.toLong())
        repeat(length) { index ->
            val xor = (source[offset + index].toInt() and 0xFF) xor (target[offset + index].toInt() and 0xFF)
            check(xor != 0)
            write(xor)
        }
        write(0)
        writeLittle32(crc32(source))
        writeLittle32(crc32(target))
    }.toByteArray()
    return body + little32(crc32(body))
}

private fun bpsTargetReadPatch(source: ByteArray, target: ByteArray): ByteArray {
    val body = ByteArrayOutputStream().apply {
        write("BPS1".encodeToByteArray())
        writeVariable(source.size.toLong())
        writeVariable(target.size.toLong())
        writeVariable(0)
        writeVariable(((target.size.toLong() - 1) shl 2) or 1)
        write(target)
        writeLittle32(crc32(source))
        writeLittle32(crc32(target))
    }.toByteArray()
    return body + little32(crc32(body))
}

private fun ByteArrayOutputStream.writeVariable(initial: Long) {
    var value = initial
    while (true) {
        val current = (value and 0x7F).toInt()
        value = value ushr 7
        if (value == 0L) {
            write(current or 0x80)
            return
        }
        write(current)
        value -= 1
    }
}

private fun ByteArrayOutputStream.writeBig16(value: Int) {
    write((value ushr 8) and 0xFF)
    write(value and 0xFF)
}

private fun ByteArrayOutputStream.writeBig24(value: Int) {
    write((value ushr 16) and 0xFF)
    write((value ushr 8) and 0xFF)
    write(value and 0xFF)
}

private fun ByteArrayOutputStream.writeLittle32(value: Long) = write(little32(value))
private fun little32(value: Long): ByteArray = byteArrayOf(
    value.toByte(),
    (value ushr 8).toByte(),
    (value ushr 16).toByte(),
    (value ushr 24).toByte()
)
private fun crc32(bytes: ByteArray): Long = CRC32().apply { update(bytes) }.value

private fun validCheatPack(): String = """
RETRA-CODES-1
provider=Retra Verification
gameSha256=${"a".repeat(64)}
gameCode=RTRE
revision=0
region=Test

[cheat]
id=money-max
name=Maximum Money
description=Verification-only raw write.
category=CURRENCY
format=RAW
risk=CAUTION
code=02000000:4:00000064
[/cheat]

[cheat]
id=money-low
name=Low Money
category=CURRENCY
format=RAW
risk=EXPERIMENTAL
code=02000000:4:00000032
[/cheat]
""".trimIndent()

private fun validCheatIndex(): String = """
RETRA-CHEAT-INDEX-1
catalogId=verification-index
name=Retra Verification Index
provider=Retra Verification
sourcePageUrl=https://example.com/retra

[pack]
id=verification-pack
title=Verification Pack
description=Checksum-pinned verification-only cheat pack.
provider=Retra Verification
gameSha256=${"a".repeat(64)}
gameCode=RTRE
revision=0
downloadUrl=https://example.com/retra-verification.rcc
packSha256=${"c".repeat(64)}
license=CC0-1.0
distributionPermission=Verification fixture distributed for test use.
sourcePageUrl=https://example.com/retra/verification-pack
[/pack]
""".trimIndent()

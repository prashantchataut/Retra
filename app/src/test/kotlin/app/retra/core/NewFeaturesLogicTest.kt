package app.retra.core

import app.retra.link.RoomCode
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NewFeaturesLogicTest {
    // ---- challenges
    private fun run(mode: ChallengeMode = ChallengeMode.NUZLOCKE) = ChallengeRun(mode, startedAt = 1)

    @Test fun honest_run_locks_every_tool_until_it_ends() {
        val r = run(ChallengeMode.HONEST)
        PlayerTool.entries.forEach { assertFalse(ChallengePolicy.allows(r, it)) }
        PlayerTool.entries.forEach { assertTrue(ChallengePolicy.allows(r.copy(state = RunState.COMPLETED), it)) }
        PlayerTool.entries.forEach { assertTrue(ChallengePolicy.allows(null, it)) }
    }

    @Test fun nuzlocke_only_for_pokemon() {
        assertEquals(listOf(ChallengeMode.HONEST), ChallengePolicy.available(false))
        assertTrue(ChallengeMode.NUZLOCKE in ChallengePolicy.available(true))
    }

    @Test fun one_encounter_per_area_and_dupes_clause() {
        var r = run()
        r = Nuzlocke.add(r, Encounter("1", "Route 101", "Zigzagoon", "Zig"))
        assertNotNull(Nuzlocke.blocker(r, "route 101", "Poochyena"))
        assertNotNull(Nuzlocke.blocker(r, "Route 102", "zigzagoon"))
        assertNull(Nuzlocke.blocker(r, "Route 102", "Ralts"))
        assertNull(Nuzlocke.blocker(r, "Route 101", "Poochyena", shiny = true))
        assertNotNull(Nuzlocke.blocker(r, " ", "Ralts"))
    }

    @Test fun wipe_ends_the_run_only_when_everything_caught_has_fainted() {
        var r = run()
        r = Nuzlocke.add(r, Encounter("1", "A", "X"))
        r = Nuzlocke.add(r, Encounter("2", "B", "Y"))
        assertFalse(Nuzlocke.isWipe(Nuzlocke.setStatus(r, "1", EncounterStatus.DEAD)))
        val wiped = Nuzlocke.applyWipeRule(Nuzlocke.setStatus(Nuzlocke.setStatus(r, "1", EncounterStatus.DEAD), "2", EncounterStatus.DEAD), 99)
        assertEquals(RunState.FAILED, wiped.state)
        assertEquals(99L, wiped.endedAt)
        assertEquals(0, Nuzlocke.survivalRate(wiped))
    }

    @Test fun route_suggestions_skip_used_areas() {
        val r = Nuzlocke.add(run(), Encounter("1", "Starter", "Mudkip"))
        assertEquals("Route 101", RouteSuggestions.next("AXVE", r))
    }

    // ---- gamepad
    @Test fun binding_a_key_moves_it_off_its_old_action() {
        val m = GamepadBindings.bind(GamepadBindings.defaults, 96, PadAction.START)
        assertEquals(PadAction.START, m[96])
        assertFalse(GamepadBindings.keysFor(m, PadAction.A).contains(96))
        assertEquals(PadAction.UP, GamepadBindings.action(19, m))
    }

    @Test fun position_preset_swaps_face_buttons_and_stored_maps_round_trip() {
        val pos = GamepadBindings.preset(GamepadBindings.Preset.POSITION)
        assertEquals(PadAction.B, pos[96]); assertEquals(PadAction.A, pos[97])
        assertEquals(pos, GamepadBindings.resolve(GamepadBindings.encode(pos)))
        assertEquals(GamepadBindings.defaults, GamepadBindings.resolve(emptyMap()))
    }

    // ---- saves
    @Test fun saves_are_fitted_to_the_game() {
        val sram = ByteArray(32 * 1024) { 7 }
        assertTrue(SaveFiles.normalize(sram, "SRAM") is SaveFiles.Result.Ok)
        val padded = sram + ByteArray(32 * 1024) { 0xFF.toByte() }
        assertEquals(32 * 1024, (SaveFiles.normalize(padded, "SRAM") as SaveFiles.Result.Ok).data.size)
        val withRtc = ByteArray(128 * 1024) + ByteArray(16)
        assertEquals(128 * 1024, (SaveFiles.normalize(withRtc, null) as SaveFiles.Result.Ok).data.size)
        assertEquals(64 * 1024, (SaveFiles.normalize(sram, "FLASH_64K") as SaveFiles.Result.Ok).data.size)
        assertTrue(SaveFiles.normalize(ByteArray(1234), null) is SaveFiles.Result.Error)
        assertTrue(SaveFiles.normalize(ByteArray(0), "SRAM") is SaveFiles.Result.Error)
        assertTrue(SaveFiles.normalize(sram, "NONE") is SaveFiles.Result.Error)
        val different = ByteArray(128 * 1024) { (it % 251).toByte() }
        assertTrue(SaveFiles.normalize(different, "SRAM") is SaveFiles.Result.Error)
    }

    @Test fun folder_plan_pairs_saves_with_their_games() {
        fun e(n: String) = FolderPlan.Entry(n, "uri/$n", 1000)
        val plan = FolderPlan.plan(listOf(e("Pokemon Emerald (USA).gba"), e("Pokemon Emerald (USA).sav"), e("Kirby.zip"), e("kirby.srm"), e("lonely.sav"), e("hack.ips"), e("notes.txt")))
        assertEquals(2, plan.roms.size); assertEquals(1, plan.patches.size); assertEquals(2, plan.pairs.size)
        assertEquals("lonely.sav", plan.unpairedSaves.single().name)
    }

    // ---- drive
    @Test fun drive_plan_uploads_downloads_and_skips() {
        val local = mapOf("aaaaaaaaaaaaaaaa" to DriveSyncPlanner.Local("m1", 100), "bbbbbbbbbbbbbbbb" to DriveSyncPlanner.Local("m2", 100), "cccccccccccccccc" to DriveSyncPlanner.Local("m3", 100))
        val remote = mapOf(
            "bbbbbbbbbbbbbbbb" to DriveSyncPlanner.Remote("f2", "M2", 50),
            "cccccccccccccccc" to DriveSyncPlanner.Remote("f3", "other", 500),
            "dddddddddddddddd" to DriveSyncPlanner.Remote("f4", "x", 1),
            "eeeeeeeeeeeeeeee" to DriveSyncPlanner.Remote("f5", "x", 1),
        )
        val plan = DriveSyncPlanner.plan(local, remote, known = setOf("aaaaaaaaaaaaaaaa", "bbbbbbbbbbbbbbbb", "cccccccccccccccc", "dddddddddddddddd"))
        assertTrue(DriveSyncPlanner.Action.Upload("aaaaaaaaaaaaaaaa", null) in plan)
        assertTrue(DriveSyncPlanner.Action.Download("cccccccccccccccc", "f3", true) in plan)
        assertTrue(DriveSyncPlanner.Action.Download("dddddddddddddddd", "f4", false) in plan)
        assertEquals(3, plan.size)
        assertEquals("abcdef0123456789", DriveSyncPlanner.gameId("abcdef0123456789.sav"))
        assertNull(DriveSyncPlanner.gameId("notes.txt"))
    }

    // ---- openrouter
    @Test fun openrouter_reply_is_validated() {
        val reply = """```json
            {"cheats":[{"name":"Master code","code":"000084F5 000A","note":"USA"},
            {"name":"Infinite money","code":"82025838 0000","note":""},
            {"name":"Nonsense","code":"hello world"},
            {"name":"Infinite money","code":"82025838 0000"}]}
            ```"""
        val p = OpenRouterCheats.parse(reply)
        assertEquals(2, p.cheats.size)
        assertEquals(2, p.dropped)
        assertEquals(0, OpenRouterCheats.parse("no json here").cheats.size)
        assertTrue(OpenRouterCheats.messages("Pokemon Emerald", "BPEE", "infinite money")[1].content.contains("BPEE"))
    }

    // ---- link rooms
    @Test fun room_codes_round_trip() {
        val code = RoomCode.encode(byteArrayOf(192.toByte(), 168.toByte(), 1, 23), 47123)
        assertEquals(11, code.length)
        assertEquals("192.168.1.23" to 47123, RoomCode.decode(code))
        assertEquals("192.168.1.23" to 47123, RoomCode.decode(code.lowercase().replace("-", " ")))
        assertNull(RoomCode.decode("nonsense"))
        assertNull(RoomCode.decode("ZZZZZ-ZZZZZ"))
    }
}

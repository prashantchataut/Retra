# Retra 4 — rebuild plan (shared contract for all contributors)

Repo: /workspace/Retra (branch `rebuild/retra-4`). Old app snapshot for reference only: /tmp/old/Retra.

## Product
Retra is a GBA emulator app for Android, built for its author and community, focused on playing
Pokémon games, Pokémon fan games (ROM-hack patches) and homebrew. Everything is built from scratch:
UI/UX, logo, mascot, emulation engine, patcher, and an original bundled game.

Content rules (hard): never bundle or download commercial ROMs or Nintendo BIOS. Fan games are
distributed as patches (IPS/UPS/BPS) that the user applies to their own base ROM. Direct downloads
are allowed only for content whose license permits redistribution (homebrew, our own game).

## Repository layout
```
settings.gradle.kts / build.gradle.kts / gradle/libs.versions.toml / gradlew
app/        Android app, Kotlin + Jetpack Compose, package app.retra
engine/     Android library :engine, namespace app.retra.engine
  src/main/cpp/core/     pure C++17 GBA core, no Android/JNI includes
  src/main/cpp/jni/      JNI bridge (retra_jni.cpp)
  src/main/cpp/CMakeLists.txt  -> shared lib "retra_engine"
  src/main/kotlin/app/retra/engine/   Kotlin API (GbaCore, GbaKey, ...)
  host/      CMake host build: retra-headless runner + tests (Linux, g++)
patcher/    Kotlin/JVM library :patcher, package app.retra.patcher (IPS/UPS/BPS, CRC32)
game/       Original GBA game "Mochi Hop" (C, arm-none-eabi-gcc), build.sh
            output ROM -> app/src/main/assets/games/mochi_hop.gba
catalog/    manifest.json — online Game Library, fetched from
            https://raw.githubusercontent.com/prashantchataut/Retra/main/catalog/manifest.json
docs/
```

## Engine Kotlin API (package app.retra.engine) — the app codes against exactly this
```kotlin
object GbaKey { const val A=1; B=2; SELECT=4; START=8; RIGHT=16; LEFT=32; UP=64; DOWN=128; R=256; L=512 }

enum class SaveType { NONE, SRAM, FLASH_64K, FLASH_128K, EEPROM_512, EEPROM_8K, AUTO }

data class RomInfo(val title: String, val gameCode: String, val makerCode: String,
                   val version: Int, val saveType: SaveType, val hasRtc: Boolean)

sealed interface LoadResult { data class Ok(val info: RomInfo): LoadResult; data class Error(val message: String): LoadResult }

class GbaCore : AutoCloseable {           // NOT thread-safe: call from one emulation thread
  companion object { const val WIDTH = 240; const val HEIGHT = 160; const val SAMPLE_RATE = 32768
                     const val FRAME_RATE = 59.7275 ; const val MAX_AUDIO_FRAMES = 2048 }
  fun loadRom(rom: ByteArray, bios: ByteArray? = null): LoadResult   // bios null -> built-in replacement/HLE BIOS
  fun reset()
  fun setKeys(mask: Int)                       // GbaKey bits, 1 = pressed
  fun runFrame(video: IntArray, audio: ShortArray): Int
      // video: 240*160 ARGB_8888 (0xFFRRGGBB) ; audio: interleaved stereo s16, size >= 2*MAX_AUDIO_FRAMES
      // returns number of stereo frames written
  fun batterySave(): ByteArray?                // null when SaveType.NONE
  fun loadBatterySave(data: ByteArray): Boolean
  fun consumeSaveDirty(): Boolean              // true once after the game wrote to its save memory
  fun saveState(): ByteArray
  fun loadState(state: ByteArray): Boolean
  fun setCheats(codes: List<String>): Int      // returns number of accepted codes (raw "AAAAAAAA:VV" + GameShark/CodeBreaker/AR where supported)
  fun setColorCorrection(enabled: Boolean)     // GBA LCD gamma/colour correction
  fun setRtcOffsetSeconds(offset: Long)
  override fun close()
}
```
Native library name: `retra_engine` (System.loadLibrary("retra_engine")).

## Design direction (app)
- Tone: Playful-soft, cute 2D, with a liquid-glass material. Signature: the mascot **Mochi**
  (a round, squishy rice-cake creature) + glossy liquid-glass surfaces with a specular rim and a
  floating glass dock whose indicator morphs like liquid.
- Type: Fredoka (display, rounded) + Nunito (body). Both OFL, bundled.
- Palette: neutrals tinted warm (light "cloud cream") / cool indigo (dark "night"), one accent
  "Mochi pink" plus semantic colours. Pastel ambient sky behind glass.
- Motion: spring-based, restrained (no bounce on everything), respects reduced motion.
- The original game **Mochi Hop** stars the same mascot.

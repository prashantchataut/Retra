# RETRA

## Autonomous One-Shot Development Prompt for a Premium Android GBA Emulator

You are the autonomous founding team responsible for designing and building a premium Android Game Boy Advance emulator named:

# Retra

**Primary tagline:** Relive the games that made you.

**Secondary tagline:** Your memories. Your library. Your adventure.

Your role combines:

* Senior Android engineer.
* Emulator-core integration engineer.
* Kotlin and Jetpack Compose specialist.
* Product designer.
* UI/UX designer.
* Backend architect.
* Security engineer.
* Performance engineer.
* Accessibility specialist.
* QA engineer.
* Release manager.

Your job is not to provide only a concept, development plan, mockup, or architecture document.

Your job is to create the real application, implement the largest stable vertical slice possible, compile it, test it, fix errors, document its current state, and leave it ready for continued autonomous development.

---

# 1. Core Product Vision

Build **Retra**, a highly advanced Android GBA emulator that combines accurate emulation, premium presentation, intelligent automation, extensive personalization, and nostalgia-focused design.

Retra should feel like a modern premium handheld gaming operating system built around the user’s personal retro-game collection.

The product should provide:

* Accurate Game Boy Advance emulation.
* Excellent performance on modern Android devices.
* Fast-forward and slow motion.
* High-refresh-rate display presentation.
* Optional visual frame smoothing.
* Low-latency input.
* Rewind.
* Save states.
* Automatic suspend and resume.
* Online cheat-code discovery.
* One-tap cheat installation.
* Legal online game downloads.
* User-configurable game catalogs.
* ROM-hack patch installation.
* Custom game libraries and collections.
* Game-specific themes and artwork.
* Controller customization.
* Touch-control customization.
* Hardcore challenge modes.
* Achievements.
* Optional cloud synchronization.
* Extensive appearance and behavior settings.
* Premium Material 3 UI.
* Restrained liquid-glass effects.
* Strong tablet, foldable, and gaming-handheld support.

Retra must feel emotionally nostalgic without looking outdated.

The app should remind users of discovering games during childhood, while presenting those memories through a modern, polished, responsive interface.

---

# 2. Autonomous One-Shot Execution

Start implementation immediately.

Do not stop after producing:

* Product requirements.
* Architecture diagrams.
* Wireframes.
* Empty Compose screens.
* A nonfunctional emulator shell.
* Placeholder buttons.
* Pseudocode.
* A list of suggested libraries.
* A roadmap without code.

Planning should be brief and followed immediately by implementation.

## 2.1 Decision-Making Authority

Make sensible product and engineering decisions without repeatedly asking for confirmation.

Choose appropriate defaults for:

* Architecture.
* Navigation.
* Screen hierarchy.
* Theme values.
* Animation timing.
* Database structure.
* Module names.
* File names.
* Dependency choices.
* Emulator-core abstraction.
* Settings organization.
* Error handling.
* Responsive layouts.

Only ask a question if implementation is truly impossible without information that cannot be represented by:

* A placeholder.
* An interface.
* A mock provider.
* An environment variable.
* A documented configuration value.

## 2.2 Build Behavior

When command-line and filesystem access are available:

1. Inspect the environment.
2. Create the Android project.
3. Configure Gradle.
4. Add required dependencies.
5. Create modules.
6. Implement features.
7. Run builds.
8. Run tests.
9. Inspect errors.
10. Fix errors.
11. Repeat until the project builds or a genuine external blocker is reached.

Do not merely describe commands that can be executed directly.

## 2.3 Honesty Requirements

Never claim a feature is complete unless it actually works.

Label every major feature as one of:

* Implemented and tested.
* Implemented but not hardware-tested.
* Partially implemented.
* Interface prepared.
* Mock provider only.
* Not started.
* Blocked by external credentials.
* Blocked by unavailable native tooling.

Do not create buttons that perform no action unless they are clearly labeled as unavailable or experimental.

---

# 3. Continuation System

Retra will continue to be developed over multiple AI sessions.

Create and maintain:

* `PROJECT_STATE.md`
* `IMPLEMENTATION_STATUS.md`
* `ARCHITECTURE_DECISIONS.md`
* `CURRENT_TASKS.md`
* `NEXT_ACTIONS.md`
* `KNOWN_ISSUES.md`
* `TEST_RESULTS.md`
* `CHANGELOG.md`
* `THREAT_MODEL.md`
* `README.md`

## 3.1 When the Next Prompt Is “Continue”

When instructed to continue:

1. Read all project-state documents.
2. Inspect the existing codebase.
3. Run the current build.
4. Run relevant tests.
5. Fix existing build failures first.
6. Resume the highest-priority incomplete task.
7. Preserve working functionality.
8. Update all state documents before finishing.

Do not restart the application from scratch.

Do not replace working architecture unless there is a documented technical reason.

## 3.2 Context-Limit Procedure

Before reaching a context or execution limit:

1. Finish the current edit safely.
2. Keep the project buildable when possible.
3. Record all unresolved errors.
4. Update `PROJECT_STATE.md`.
5. Update `NEXT_ACTIONS.md`.
6. Identify the exact file and action from which development should resume.
7. Do not falsely claim unfinished work is complete.

---

# 4. Brand Identity

## 4.1 Product Name

**Retra**

The name communicates:

* Retro gaming.
* Returning to past experiences.
* Replaying memories.
* A simple, modern technology brand.

Use Retra as a working name until trademark, domain, package-name, and app-store availability checks are completed.

Suggested package identifier:

`app.retra.emulator`

Keep the identifier configurable before release.

## 4.2 Taglines

Primary:

**Relive the games that made you.**

Alternatives:

* Your memories. Your library. Your adventure.
* Play the past, perfected.
* Return to your favorite worlds.
* Every save holds a memory.
* Yesterday’s adventures, beautifully restored.
* Pick up where childhood left off.

## 4.3 Brand Personality

Retra should feel:

* Nostalgic.
* Premium.
* Personal.
* Welcoming.
* Technically advanced.
* Calm.
* Modern.
* Highly polished.
* Customizable.
* Trustworthy.

Avoid making Retra look like:

* A generic file manager.
* A piracy-focused ROM downloader.
* A childish toy.
* A hacker utility.
* A copy of an existing console interface.
* A clone of another emulator.

## 4.4 Design Language

Name the visual system:

# Retra Prism

Retra Prism combines:

* Material 3 structure.
* Translucent glass layers.
* Soft environmental blur.
* Aurora-inspired highlights.
* Pixel-era visual references.
* Smooth spatial motion.
* High readability.
* User-controlled personalization.

The visual metaphor is a childhood memory preserved inside a modern piece of glass.

Liquid-glass effects should be subtle and purposeful.

Do not make every surface transparent.

Dense settings screens, long descriptions, cheat-code warnings, and technical information should use more opaque surfaces for readability.

---

# 5. Visual Identity

## 5.1 Default Dark Palette

* **Void Black:** `#07080D`
* **Midnight Navy:** `#101424`
* **Retra Indigo:** `#7567FF`
* **Prism Cyan:** `#45D9FF`
* **Memory Violet:** `#B38BFF`
* **Save Mint:** `#59E1AF`
* **Adventure Gold:** `#FFC766`
* **Warning Amber:** `#FFB34E`
* **Error Coral:** `#FF687C`
* **Cloud White:** `#F5F6FF`

## 5.2 Light Palette

* **Soft Cloud:** `#F6F6FC`
* **Lavender Mist:** `#ECE9FF`
* **Prism Ice:** `#DFF7FF`
* **Deep Ink:** `#181925`
* **Muted Ink:** `#646675`

## 5.3 Optional User Palettes

Include preset themes such as:

* Retra Prism.
* Indigo Night.
* Aurora.
* Emerald Cartridge.
* Sunset Gold.
* Atomic Purple.
* Glacier.
* OLED Black.
* Classic Gray.
* User Dynamic Color.

Do not copy protected console branding, logos, or proprietary visual assets.

## 5.4 Logo Direction

Create an original icon featuring:

* A rounded abstract cartridge form.
* A subtle letter R constructed using negative space.
* A light prism or playback trail.
* A small sparkle representing memory.
* A clean silhouette at small launcher sizes.
* No copyrighted game character.
* No Poké Ball.
* No recognizable Nintendo console shape.
* No commercial-game imagery.

## 5.5 Typography

Use:

* A clean geometric font for titles.
* A highly readable system or open-source font for body copy.
* A monospaced font for hashes, cheat codes, and diagnostics.

Suggested direction:

* Manrope for titles.
* Inter or Android system typography for body text.
* JetBrains Mono for technical information.

Pixel fonts may only be used as optional decorative accents.

---

# 6. Copyright and Content Rules

These rules are mandatory.

## 6.1 Retra Must Not

Retra must not:

* Bundle copyrighted commercial ROMs.
* Operate a catalog of unauthorized commercial ROMs.
* Scrape known piracy websites.
* Provide unauthorized direct download links.
* Bundle proprietary BIOS files.
* Bypass copyright protection.
* Hide the origin of downloaded content.
* Automatically upload a user’s games.
* Present copyrighted games as free legal downloads.

## 6.2 What Retra May Download

Retra may provide in-app downloads for:

* Public-domain games.
* Open-source games.
* Developer-authorized homebrew.
* Free demos.
* Legally redistributable test ROMs.
* Game-jam releases whose creators permit redistribution.
* Fan-game patches when legally distributed.
* ROM-hack patches without copyrighted base-game data.
* Cheat packs.
* Metadata.
* Licensed artwork.
* Achievement definitions.
* Controller profiles.
* Themes.
* Shader presets.

Every downloadable game must display:

* Creator.
* Source.
* License.
* Version.
* File size.
* Checksum.
* Distribution permission.
* Last update.
* Compatibility status.

## 6.3 Commercial Games

Commercial games may appear as metadata entries with:

* Name.
* Description.
* Release information.
* Compatibility rating.
* Region information.
* Required ROM revision.
* Cheat availability.
* Patch availability.
* Achievement availability.
* Import button.

Commercial entries must use actions such as:

* Import My Copy.
* Match Existing File.
* Verify Game File.
* Select My Backup.
* View Compatible Patches.

They must not provide unauthorized ROM downloads.

---

# 7. Custom Online Game Library

Retra should contain an advanced custom game-library system that supports both local games and legal remote catalogs.

## 7.1 Library Sources

Support these source types:

### Local Sources

* User-selected folders.
* Individual imported files.
* Removable storage.
* Android Storage Access Framework locations.
* User-owned cloud folders where supported.

### Official Retra Catalog

Contains only:

* Licensed homebrew.
* Public-domain games.
* Free demos.
* Open-source games.
* Legally distributable utilities.
* Test ROMs.

### Community Catalogs

Allow users to add catalog manifests that contain legally hosted content.

Community catalogs must use a restricted, validated JSON format.

### Private User Catalogs

Allow advanced users to connect their own personally controlled storage, such as:

* WebDAV.
* S3-compatible storage.
* Personal HTTP directory with a valid manifest.
* Self-hosted server.
* Personal NAS.
* Supported cloud-storage provider.
* Local network share where Android permits it.

Retra should treat private sources as user-controlled storage, not as public content directories.

## 7.2 Catalog Manifest

Create a safe manifest format such as:

```json
{
  "catalogVersion": 1,
  "catalogId": "string",
  "name": "string",
  "description": "string",
  "owner": "string",
  "sourceUrl": "https://example.com/catalog.json",
  "contentPolicy": "AUTHORIZED_ONLY",
  "games": [
    {
      "id": "string",
      "title": "string",
      "description": "string",
      "creator": "string",
      "version": "string",
      "downloadUrl": "https://example.com/game.gba",
      "sha256": "string",
      "fileSize": 0,
      "license": "string",
      "distributionPermission": "string",
      "artworkUrl": "string",
      "tags": ["homebrew"],
      "compatibility": "PLAYABLE"
    }
  ]
}
```

Validate:

* HTTPS.
* File size.
* MIME type.
* Hash.
* Manifest schema.
* Archive structure.
* Download redirects.
* Domain changes.
* Distribution fields.
* Duplicate content.
* Unsafe paths.

Never execute downloaded code outside the emulated game environment.

## 7.3 Download Manager

Implement an in-app download manager with:

* Pause.
* Resume.
* Retry.
* Cancel.
* Queue.
* Wi-Fi-only option.
* Storage-location selection.
* Download progress.
* Estimated remaining size.
* Checksum verification.
* Automatic library import.
* Duplicate detection.
* Update detection.
* Download history.
* Failed-download cleanup.

After a legal game is downloaded:

1. Validate its checksum.
2. Verify that it is a supported ROM.
3. Scan its header.
4. Add it to the library.
5. Retrieve permitted metadata.
6. Offer to launch it.
7. Preserve source and licensing details.

## 7.4 User-Provided Direct Downloads

Allow users to import a direct URL only through explicit action.

Before downloading:

* Show the full domain.
* Show the detected file type.
* Show the estimated size.
* Warn that the user is responsible for having permission.
* Require confirmation.
* Scan and validate the file.
* Reject web pages, executables, APKs, and unsupported content.
* Never invisibly scrape websites for ROM links.

---

# 8. Custom Library Experience

Retra’s game library must be highly customizable.

## 8.1 Library Layouts

Support:

* Large cover grid.
* Compact cover grid.
* Detailed list.
* Shelf layout.
* Horizontal console-style carousel.
* Recently played dashboard.
* Minimal title-only mode.
* Custom mixed dashboard.

## 8.2 User-Created Home Screen

Allow users to choose and reorder home sections such as:

* Continue Playing.
* Recently Added.
* Recently Played.
* Favorites.
* Childhood Favorites.
* Pokémon Collection.
* ROM Hacks.
* Homebrew.
* IronRun Challenges.
* Achievement Progress.
* Most Played.
* Completed Games.
* Never Played.
* Downloaded Games.
* Cloud Games.
* Custom Collection.

Allow users to:

* Add a section.
* Remove a section.
* Reorder sections.
* Change section layout.
* Change section size.
* Choose how many games appear.
* Hide empty sections.

## 8.3 Collections

Support:

* Manual collections.
* Smart collections.
* Nested collections.
* Tags.
* Collection artwork.
* Collection color.
* Collection icon.
* Custom description.
* Sorting rules.
* Filter rules.

Example smart collections:

* Games played for more than ten hours.
* Games with active save files.
* Games with achievements.
* Pokémon ROM hacks.
* Games added this month.
* Games never launched.
* Games with verified cheats.
* Games compatible with IronRun.
* Homebrew downloaded from the official catalog.

## 8.4 Game Customization

For every game, users may customize:

* Title.
* Sort title.
* Cover art.
* Hero art.
* Background.
* Icon.
* Description.
* Developer.
* Release year.
* Region.
* Tags.
* Collection membership.
* Display badge.
* Controller profile.
* Touch layout.
* Visual filter.
* Color theme.
* Launch animation.
* Custom notes.

Never modify the ROM contents when changing metadata.

---

# 9. Android Technology Stack

Use:

* Kotlin.
* Jetpack Compose.
* Material 3.
* Gradle Kotlin DSL.
* Coroutines.
* Flow.
* Room.
* DataStore.
* WorkManager.
* Hilt or an equivalent mature dependency-injection solution.
* Android Storage Access Framework.
* Android Game Controller APIs.
* Android NDK.
* CMake.
* Baseline Profiles.
* Macrobenchmark.
* JUnit.
* Compose UI tests.

Target modern Android while maintaining sensible compatibility.

Suggested baseline:

* Minimum Android 8.
* Full optimization for Android 12 and later.
* Phones.
* Tablets.
* Foldables.
* Android TV where practical.
* Gaming handhelds.
* Chromebooks where supported.

---

# 10. Project Architecture

Use modular clean architecture with unidirectional data flow.

Suggested modules:

```text
:app

:core:common
:core:model
:core:database
:core:datastore
:core:network
:core:storage
:core:security
:core:designsystem
:core:catalog
:core:download
:core:testing

:emulation:api
:emulation:native
:emulation:controller
:emulation:saves
:emulation:cheats
:emulation:patching
:emulation:performance

:feature:onboarding
:feature:home
:feature:library
:feature:collections
:feature:game-details
:feature:player
:feature:downloads
:feature:catalogs
:feature:cheats
:feature:saves
:feature:patches
:feature:achievements
:feature:controllers
:feature:appearance
:feature:settings
:feature:diagnostics
:feature:cloud

:benchmark
```

Consolidate modules temporarily if required to keep the first build manageable.

Document future module boundaries clearly.

---

# 11. Emulator Core

Integrate a mature, actively maintained, legally compatible GBA emulator core.

Before selecting the core, review:

* License.
* Source-code obligations.
* Android support.
* ARM64 support.
* Save-state support.
* Cheat support.
* RTC support.
* Audio quality.
* Rendering architecture.
* Accuracy.
* Dynamic recompiler support.
* Sensor support.
* Link-cable potential.
* Upstream maintenance.

Do not attempt a complete emulator-core rewrite during the first development pass unless no appropriate core is available.

Create a stable interface:

```kotlin
interface EmulationCore {
    suspend fun loadGame(game: GameFile): LoadGameResult
    fun start()
    fun pause()
    fun resume()
    fun reset()
    fun stop()

    fun setInputState(input: EmulatorInputState)
    fun setEmulationSpeed(multiplier: Float)
    fun setPerformanceProfile(profile: PerformanceProfile)

    fun saveBattery(): Result<Unit>
    fun saveState(slot: SaveSlot): Result<SaveStateMetadata>
    fun loadState(slot: SaveSlot): Result<Unit>

    fun applyCheats(cheats: List<ActiveCheat>): Result<Unit>
    fun clearCheats(): Result<Unit>

    fun getRuntimeMetrics(): RuntimeMetrics
}
```

Keep the native emulator core independent from:

* Compose.
* Accounts.
* Online catalogs.
* Artwork.
* Navigation.
* Cloud storage.
* Analytics.

---

# 12. Retra Boost Performance System

Call the performance suite:

# Retra Boost

Retra Boost must clearly separate:

* Normal emulation accuracy.
* Fast-forward speed.
* Display refresh rate.
* Visual frame smoothing.
* Frame duplication.
* Input latency.
* Audio latency.
* Frameskip.

Do not falsely advertise fast-forward as native FPS enhancement.

## 12.1 Performance Profiles

### Authentic

* Original timing.
* High accuracy.
* Stable audio.
* No interpolation.
* Minimal frameskip.
* Intended to resemble original hardware behavior.

### Balanced

* Default profile.
* Stable frame pacing.
* Low input delay.
* Efficient renderer.
* Moderate battery usage.

### Boosted

* More aggressive native optimization.
* Reduced render buffering.
* Optional run-ahead.
* High-refresh presentation.
* Higher power consumption.

### Extreme

* Maximum fast-forward potential.
* Reduced visual effects.
* Aggressive thermal monitoring.
* Intended for grinding and repetitive game sections.
* Must not be the default.

### Battery Saver

* Reduced UI blur.
* Conservative refresh behavior.
* Disabled interpolation.
* Efficient shaders.
* Lower background activity.

## 12.2 Fast-Forward

Support:

* 1.25×.
* 1.5×.
* 2×.
* 3×.
* 4×.
* 6×.
* 8×.
* 12×.
* 16×.
* Unlimited.

Users can configure:

* Hold to fast-forward.
* Toggle fast-forward.
* Default speed.
* Per-game speed.
* Audio during fast-forward.
* Pitch-preserved audio.
* Mute above a selected speed.
* Physical-controller shortcut.
* Touchscreen shortcut.
* Gesture shortcut.

## 12.3 High-Refresh Presentation

Support 90 Hz, 120 Hz, and 144 Hz displays.

Provide:

* Correct display synchronization.
* Stable frame pacing.
* Optional frame duplication.
* Optional frame blending.
* Experimental visual interpolation.
* Per-game enablement.
* Thermal fallback.

Explain clearly:

* The original game logic remains at its intended timing.
* High-refresh presentation can improve perceived smoothness.
* Interpolation may create visual artifacts.

## 12.4 Low-Latency Options

Implement where possible:

* Reduced input buffering.
* Immediate controller-state updates.
* Low-latency renderer mode.
* Low-latency audio mode.
* One-frame run-ahead.
* Two-frame run-ahead.
* Latency test screen.
* Frame-time diagnostics.

Disable run-ahead when:

* Determinism is unavailable.
* Multiplayer is active.
* Device performance is insufficient.
* A cheat causes unstable state.
* Hardcore rules prohibit it.

## 12.5 Frameskip

Frameskip must:

* Be disabled by default at normal speed.
* Be clearly explained.
* Have a maximum limit.
* Activate automatically only when configured.
* Avoid breaking audio timing.
* Show dropped-frame diagnostics.
* Never be marketed as an accuracy improvement.

---

# 13. Time Controls

Create a time-control system with:

* Fast-forward.
* Slow motion.
* Rewind.
* Pause.
* Frame advance.
* Quick save.
* Quick load.
* Timeline scrubbing.

## 13.1 Rewind

Support:

* 10 seconds.
* 30 seconds.
* 60 seconds.
* 2 minutes.
* 5 minutes where hardware permits.
* Custom memory budget.

Requirements:

* Circular state buffer.
* Compressed state storage.
* Automatic memory-pressure reduction.
* Timeline preview.
* Haptic markers.
* Per-game configuration.
* Estimated RAM usage.
* No impact on battery-save integrity.

---

# 14. Cheat System

Name the cheat system:

# Retra Codes

Retra Codes must allow users to discover, import, organize, and activate cheats without manually copying every code.

## 14.1 Automatic Cheat Discovery

When a game is imported:

1. Calculate its SHA-256 hash.
2. Detect its game code.
3. Detect region.
4. Detect revision.
5. Search configured cheat providers.
6. Retrieve only exact or clearly compatible matches.
7. Cache cheat metadata.
8. Display available cheat packs on the game-detail screen.

## 14.2 Supported Formats

Where supported by the emulator core:

* GameShark.
* CodeBreaker.
* Action Replay.
* Raw memory writes.
* Conditional codes.
* Multi-line codes.
* Master codes.
* Revision-specific codes.

## 14.3 Cheat Categories

* Currency.
* Health.
* Experience.
* Encounters.
* Inventory.
* Unlockables.
* Movement.
* Difficulty.
* Quality of life.
* Visual changes.
* Debugging.
* Experimental.

## 14.4 Cheat Profiles

Allow profiles such as:

* Relaxed Story.
* Fast Grinding.
* Completion Run.
* Randomizer Setup.
* Testing.
* No Encounters.
* User Custom.

Each profile stores:

* Enabled cheats.
* Dependencies.
* Conflict information.
* Activation order.
* Associated ROM hash.
* Save-backup state.

## 14.5 Safety

Never execute downloaded scripts or code.

Cheat packs must use a declarative format.

Before enabling risky cheats:

* Create a battery-save backup.
* Create a protected save state.
* Label it “Before cheats.”
* Warn about possible progression or save issues.

Detect:

* Address conflicts.
* Duplicate writes.
* Wrong region.
* Wrong revision.
* Incompatible master codes.
* Achievement disqualification.
* Hardcore-mode restrictions.

---

# 15. ROM-Hack and Patch Manager

Name this system:

# Retra Patch

Support:

* IPS.
* UPS.
* BPS.
* XDELTA where practical.

Workflow:

1. Select or download a legal patch.
2. Inspect patch metadata.
3. Identify the required base ROM.
4. Ask the user to select their base file.
5. Verify hashes.
6. Warn about revision mismatch.
7. Back up relevant saves.
8. Apply the patch locally.
9. Validate the result.
10. Create a separate game-library entry.
11. Preserve the original file.
12. Record patch version and creator.

For ROM hacks such as AshGray:

* Retra may display metadata.
* Retra may provide a legally distributed patch.
* Retra must require the user’s compatible base ROM.
* Retra must apply the patch locally.
* Retra must not distribute a pre-patched copyrighted game.

---

# 16. Save System

Name the save center:

# Retra Vault

Support:

## Battery Saves

* Automatic detection.
* Automatic write.
* Atomic replacement.
* Rotating backups.
* Import.
* Export.
* Integrity hashes.
* Conflict detection.

## Save States

* Multiple slots.
* Quick-save slot.
* Quick-load slot.
* Protected slots.
* Screenshot thumbnail.
* User label.
* User note.
* Timestamp.
* Playtime.
* ROM hash.
* Core version.
* Patch version.
* Active cheats.

## Suspend State

When the app enters the background:

* Pause emulation.
* Flush battery save.
* Optionally create a suspend state.
* Release unnecessary resources.
* Resume reliably.

## Save Timeline

Create a visual timeline that displays:

* Screenshot.
* Date.
* Playtime.
* Save type.
* Cheat status.
* Patch status.
* Cloud status.
* Protected status.

---

# 17. Hardcore Mode

Name the challenge system:

# Pure Run

Pure Run may restrict:

* Cheats.
* Save states.
* Rewind.
* Slow motion.
* Frame advance.
* Quick load.
* Memory editing.
* Gameplay-changing patches.
* Certain fast-forward options.

Presets:

* Standard Hardcore.
* No Cheats.
* No Save States.
* No Rewind.
* One Save Only.
* Permadeath.
* Original Hardware.
* No Fast-Forward.
* Custom Rules.

Track:

* Start date.
* Playtime.
* ROM hash.
* Patch hash.
* Core version.
* Rules.
* Resets.
* Loads.
* Restricted-feature attempts.
* Achievement eligibility.
* Completion status.

Do not represent integrity tracking as impossible to bypass.

---

# 18. Touch Controls

Name the touchscreen editor:

# Retra Deck

Allow users to modify:

* Position.
* Size.
* Opacity.
* Shape.
* Label.
* Spacing.
* Rotation.
* Haptic strength.
* Touch sensitivity.
* D-pad diagonals.
* Dead zones.
* Hold behavior.
* Turbo behavior.
* Auto-hide delay.
* Adaptive contrast.

Components:

* D-pad.
* A.
* B.
* L.
* R.
* Start.
* Select.
* Menu.
* Fast-forward.
* Rewind.
* Quick save.
* Quick load.
* Turbo buttons.
* Custom shortcuts.

Presets:

* Classic.
* Minimal.
* Compact.
* Large Accessibility.
* Landscape Pro.
* Portrait Comfort.
* One-Handed Left.
* One-Handed Right.
* Controller Companion.
* Gesture Mode.

Support separate:

* Portrait layouts.
* Landscape layouts.
* Global layouts.
* Per-game layouts.

---

# 19. Physical Controllers

Support:

* Bluetooth gamepads.
* USB gamepads.
* Xbox-style controllers.
* PlayStation-style controllers.
* Generic Android controllers.
* Android gaming handhelds.
* Keyboard controls.

Provide:

* Automatic detection.
* Input tester.
* Per-controller mapping.
* Per-game mapping.
* Analog dead-zone settings.
* Trigger thresholds.
* Turbo.
* Button combinations.
* Long-press actions.
* Menu navigation.
* Reconnection handling.
* Multiple-controller architecture.

---

# 20. UI and UX Requirements

Retra’s interface must feel premium, coherent, fast, and emotionally engaging.

## 20.1 UX Principles

Follow these principles:

* Gameplay is always one or two actions away.
* The latest save is always easy to resume.
* Advanced options remain discoverable without overwhelming beginners.
* Every destructive action is reversible where possible.
* Important settings explain their effect.
* The user should never wonder whether a save completed.
* Loading states should feel intentional.
* Empty states should teach the user what to do.
* Online failure must never block local play.
* Visual polish must never reduce readability.

## 20.2 Main Navigation

Recommended phone navigation:

* Home.
* Library.
* Discover.
* Vault.
* Settings.

Allow the user to customize navigation by:

* Reordering tabs.
* Hiding optional tabs.
* Selecting four or five tabs.
* Choosing icon labels or icons only.
* Selecting bottom navigation or compact navigation.
* Choosing whether the app opens to Home, Library, or Continue Playing.

On tablets and foldables:

* Use a navigation rail.
* Support two-pane layouts.
* Keep game details visible beside the library.
* Use larger artwork responsibly.

## 20.3 Home Screen

The home screen should feel personal.

Suggested structure:

* Continue Playing hero card.
* Latest save.
* Recently played games.
* Custom shelves.
* Download progress.
* Achievement progress.
* Newly available cheat packs.
* Patch updates.
* Recommended legal homebrew.

Allow every home section to be reordered or disabled.

## 20.4 Game Launch Experience

When a user taps a game:

* Use a short shared-element transition.
* Show a polished loading state.
* Display save status.
* Resume automatically when configured.
* Avoid unnecessary confirmation screens.
* Restore the selected performance profile.
* Restore controller and touch profiles.
* Restore active cheat profile.
* Show a subtle game-title overlay.
* Fade quickly into gameplay.

## 20.5 In-Game Menu

Create a responsive translucent control center with:

* Resume.
* Save.
* Load.
* Quick Save.
* Quick Load.
* Rewind.
* Speed.
* Cheats.
* Controls.
* Achievements.
* Screenshot.
* Display.
* Audio.
* Reset.
* Exit.

The most common actions must appear first.

Advanced options should appear in expandable sections.

The menu must be fully usable with a controller.

## 20.6 Microinteractions

Use:

* Small haptic feedback.
* Gentle save confirmation.
* Short download-complete animation.
* Smooth card expansion.
* Shared artwork transitions.
* Subtle glass refraction.
* Responsive button press states.
* Animated progress indicators.

Avoid:

* Excessive bounce.
* Long page transitions.
* Constant background motion.
* Strong glow everywhere.
* Heavy blur over gameplay.
* Animations that delay actions.

---

# 21. Highly Customizable Settings

The settings experience must be one of Retra’s strongest features.

Create:

* Settings search.
* Recently changed settings.
* Favorite settings.
* Reset controls.
* Per-setting explanations.
* Global versus per-game indicators.
* Settings import and export.
* Settings profiles.
* Experimental-feature section.
* Developer diagnostics section.

## 21.1 Appearance Settings

Allow users to customize:

* Light theme.
* Dark theme.
* OLED black theme.
* System theme.
* Dynamic color.
* Accent color.
* Secondary accent.
* Background color.
* Glass tint.
* Glass transparency.
* Blur intensity.
* Edge-light intensity.
* Corner radius.
* Card elevation.
* Card border.
* Content density.
* Font scale.
* Title typography.
* Artwork saturation.
* Background dimming.
* Motion intensity.
* Animation speed.
* Reduce motion.
* Reduce transparency.
* High contrast.

## 21.2 Library Settings

Allow users to customize:

* Default layout.
* Cover size.
* Card aspect ratio.
* Card corners.
* Visible metadata.
* Badge visibility.
* Title-line limit.
* Artwork crop.
* Sorting.
* Grouping.
* Shelf spacing.
* Number of columns.
* Continue Playing behavior.
* Hidden games.
* Duplicate handling.
* Missing-file behavior.

## 21.3 Home Settings

Allow users to:

* Reorder sections.
* Add sections.
* Remove sections.
* Change section layout.
* Select hero-card style.
* Select background behavior.
* Show or hide statistics.
* Show or hide online recommendations.
* Show or hide downloads.
* Choose startup destination.

## 21.4 Player Settings

Allow:

* Aspect ratio.
* Integer scaling.
* Crop.
* Stretch.
* Rotation lock.
* Immersive mode.
* Cutout handling.
* Shader.
* Color correction.
* LCD simulation.
* Scanlines.
* Frame blending.
* Interpolation.
* Retra Boost profile.
* Fast-forward speed.
* Rewind memory.
* Audio buffer.
* Run-ahead.
* Frameskip.
* Screenshot format.

## 21.5 Control Settings

Allow:

* Global touch profile.
* Per-game touch profile.
* Touch opacity.
* Haptic strength.
* Controller priority.
* Controller mapping.
* Gesture controls.
* Menu shortcut.
* Fast-forward shortcut.
* Save shortcuts.
* Analog dead zones.
* Turbo rate.

## 21.6 Audio Settings

Allow:

* Master volume.
* Game volume.
* Interface sounds.
* Achievement sounds.
* Fast-forward audio.
* Pitch correction.
* Audio buffer.
* Bluetooth mode.
* Pause when headphones disconnect.
* Background playback behavior.

## 21.7 Download Settings

Allow:

* Download location.
* Wi-Fi only.
* Mobile-data confirmation.
* Simultaneous download count.
* Automatic updates.
* Automatic imports.
* Checksum verification.
* Catalog refresh frequency.
* Clear failed downloads.
* Storage warnings.

## 21.8 Privacy Settings

Allow users to control:

* Crash reports.
* Anonymous diagnostics.
* Compatibility reports.
* Catalog requests.
* Achievement synchronization.
* Cloud saves.
* Playtime tracking.
* Recent-game history.
* Online artwork.
* Personalized recommendations.

## 21.9 Settings Profiles

Users can create profiles such as:

* Authentic Handheld.
* Maximum Performance.
* Battery Saver.
* OLED Night.
* Minimal UI.
* Nostalgia.
* Grinding.
* Controller Mode.
* Touch Mode.
* Custom.

A profile may include:

* Appearance.
* Performance.
* Audio.
* Display.
* Controls.
* Library layout.

Allow profiles to be:

* Exported.
* Imported.
* Duplicated.
* Renamed.
* Applied globally.
* Assigned to individual games.

---

# 22. Display and Shader System

Support:

* Sharp pixels.
* Bilinear filtering.
* LCD simulation.
* Subtle ghosting.
* Scanlines.
* Color correction.
* Warm display.
* Cool display.
* Saturation adjustment.
* Contrast adjustment.
* Gamma adjustment.
* Integer scaling.
* Custom aspect ratio.
* Safe shader import.

Shader imports must use a restricted format and must not access the filesystem, network, or unrelated device resources.

Provide a live preview.

---

# 23. Audio System

Implement:

* Low-latency audio.
* Stable synchronization.
* Ring-buffer architecture.
* Configurable buffering.
* Bluetooth latency profile.
* Audio focus handling.
* Fast-forward audio behavior.
* Optional pitch preservation.
* Headphone-disconnect pause.
* Recording support where permitted.

Extreme fast-forward should degrade audio predictably rather than destabilizing the emulator.

---

# 24. Screenshots and Recording

Support:

* Screenshots.
* Screenshot gallery.
* Optional game frame.
* Optional metadata overlay.
* PNG and supported alternative formats.
* Short video recording.
* Game-audio capture where permitted.
* Microphone disabled by default.
* Android share sheet.
* Privacy metadata removal.
* Custom export folder.

Never start recording without clear user action and a visible indicator.

---

# 25. Achievements

Support:

* Local achievements.
* Provider-based achievements.
* Progress achievements.
* Hidden achievements.
* Rarity.
* Completion percentage.
* Offline queue.
* Pure Run eligibility.
* Achievement showcase.
* Optional popups.
* Optional sounds.

Third-party achievement systems must use authorized APIs and follow provider requirements.

---

# 26. Cloud and Sync

Cloud synchronization must be optional.

Synchronize:

* Battery saves.
* Save states.
* Settings profiles.
* Touch layouts.
* Controller mappings.
* Collections.
* Metadata edits.
* Cheat profiles.
* Achievement progress.
* User notes.

Requirements:

* Offline-first behavior.
* Encryption in transit.
* Clear sync status.
* Conflict detection.
* Version history.
* Manual conflict resolution.
* No silent destructive overwrite.
* Account deletion.
* Data export.
* Cloud-data deletion.

Do not upload ROMs by default.

Allow users to connect their own storage provider for personal backups where technically possible.

---

# 27. Accessibility

Implement:

* Screen-reader descriptions.
* Logical focus order.
* Controller-only navigation.
* Large touch targets.
* Scalable text.
* High-contrast mode.
* Reduced transparency.
* Reduced motion.
* Color-blind-safe indicators.
* Non-color status labels.
* Left-handed controls.
* One-handed controls.
* Adjustable hold duration.
* Haptic alternatives.
* Accessible errors.
* Accessible settings descriptions.

Decorative glass must never reduce readability.

---

# 28. Security

Protect against:

* Malicious ROM files.
* Malicious archives.
* Archive bombs.
* Path traversal.
* Corrupt patches.
* Malicious catalog manifests.
* Fake checksums.
* Redirect attacks.
* Compromised providers.
* Malicious cheat packs.
* Shader abuse.
* Save corruption.
* Cloud conflicts.
* Token theft.
* Dependency compromise.

Downloaded content must be:

* Size limited.
* Type checked.
* Hash verified.
* Schema validated.
* Stored temporarily before validation.
* Rejected if malformed.
* Never executed as Android code.

---

# 29. Error Experience

Create useful errors for:

* Invalid game.
* Unsupported archive.
* Missing file.
* Lost folder access.
* Download failure.
* Checksum mismatch.
* Unauthorized catalog format.
* Wrong patch base.
* Corrupt save.
* Incompatible state.
* Cheat mismatch.
* Controller disconnect.
* Renderer failure.
* Audio failure.
* Native-core crash.
* Low storage.
* Thermal pressure.

Every error must answer:

* What happened?
* Is the save safe?
* What can the user do?
* Can Retra repair it?
* Is a diagnostic report available?

---

# 30. Diagnostics

Display:

* App version.
* Emulator-core version.
* Android version.
* Device model.
* CPU architecture.
* Renderer.
* Display refresh rate.
* Audio backend.
* Emulation speed.
* Presented FPS.
* Emulated FPS.
* Frame drops.
* Audio underruns.
* Input devices.
* ROM hash.
* Active patch.
* Active cheats.
* Rewind memory.
* Thermal state.
* Recent errors.

Allow a privacy-sanitized export.

Never include ROM or save contents.

---

# 31. Initial Development Order

## Stage 1: Foundation

* Create Android project.
* Configure Compose.
* Create Retra Prism design tokens.
* Create navigation.
* Create database.
* Create settings framework.
* Verify the project builds.

## Stage 2: Local Library

* Implement file import.
* Implement folder selection.
* Scan `.gba` files.
* Parse headers.
* Calculate hashes.
* Detect duplicates.
* Display library.
* Create game-detail page.

## Stage 3: Emulator Integration

* Select a legal mature core.
* Integrate native build.
* Load games.
* Render video.
* Play audio.
* Send input.
* Pause and resume.
* Reset and stop.

## Stage 4: Save Reliability

* Battery saves.
* Atomic writes.
* Rotating backups.
* Save states.
* Thumbnails.
* Suspend state.
* Crash recovery.

## Stage 5: Player UX

* Touch controls.
* Controller support.
* In-game menu.
* Retra Boost.
* Fast-forward.
* Display settings.
* Audio settings.

## Stage 6: Legal Online Catalog

* Catalog schema.
* Official mock catalog.
* Legal homebrew downloads.
* Download manager.
* Checksum verification.
* Automatic import.
* Source and license display.

## Stage 7: Custom Catalogs

* Manifest import.
* Provider validation.
* Private catalog configuration.
* WebDAV or personal-server abstraction.
* Catalog enable and disable controls.
* Catalog health status.

## Stage 8: Retra Codes

* Cheat schema.
* Hash matching.
* Cheat profiles.
* Core integration.
* Conflict detection.
* Save protection.

## Stage 9: Retra Patch

* IPS.
* UPS.
* BPS.
* Base-game validation.
* Patched-game library entries.

## Stage 10: Advanced Personalization

* Theme editor.
* Custom home sections.
* Library layouts.
* Navigation customization.
* Settings profiles.
* Settings import and export.

## Stage 11: Advanced Features

* Rewind.
* Achievements.
* Pure Run.
* Cloud sync.
* Multiplayer research.
* Compatibility reporting.

---

# 32. Minimum First-Build Acceptance Criteria

The first substantial build is successful only when:

* The project compiles.
* The application launches.
* Onboarding works.
* The user can select a folder.
* `.gba` files are detected.
* ROM headers are parsed.
* Hashes are calculated.
* Games appear in the library.
* Game details open.
* A real emulator core is integrated.
* Video renders.
* Audio plays.
* Touch input works.
* Battery-save paths function.
* Save states work.
* Fast-forward works.
* The in-game menu works.
* Appearance settings persist.
* Library-layout settings persist.
* The application contains no commercial ROMs or proprietary BIOS files.
* Legal catalog infrastructure exists.
* At least one legally distributable test or homebrew entry can be downloaded and imported.
* Core non-native logic has automated tests.
* Actual build status is reported honestly.

---

# 33. Required Final Report

After each autonomous development session, report:

## Implemented

Only list features that genuinely work.

## Build Status

Include:

* Build command.
* Build result.
* Test command.
* Test result.
* APK path when available.

## Verified Flows

List user flows that were actually tested.

## Partial Features

Describe incomplete work clearly.

## Known Issues

Include build, emulator, UI, and performance issues.

## Next Actions

List the next five concrete engineering tasks.

Do not provide an inspirational essay.

Do not claim universal game compatibility.

Do not describe mock interfaces as completed online services.

---

# 34. Quality Priority

When priorities conflict, use this order:

1. Save integrity.
2. Emulator accuracy.
3. Build reliability.
4. Input responsiveness.
5. Audio stability.
6. Rendering stability.
7. Download security.
8. Content legality.
9. Accessibility.
10. Performance.
11. UI clarity.
12. Customization.
13. Visual effects.
14. Online social features.

A polished interface cannot compensate for damaged saves.

A huge feature list cannot compensate for a project that does not compile.

A downloader cannot compromise user safety or distribute unauthorized commercial games.

---

# 35. Final Instruction

Begin building Retra now.

Do not respond with only a plan.

Inspect the development environment, create the project, implement the first complete vertical slice, compile it, run tests, repair errors, and continue through the prioritized stages.

The result should be:

* Technically credible.
* Beautiful.
* Fast.
* Highly customizable.
* Safe.
* Accessible.
* Nostalgic.
* Easy for beginners.
* Powerful for advanced users.
* Respectful of game creators and copyright.

Retra should not merely emulate old games.

It should feel like the definitive modern home for the user’s retro-game memories.

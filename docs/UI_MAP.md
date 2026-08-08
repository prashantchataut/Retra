# Retra UI map

Single product shell. There is no separate V1/V2/V3 UI.

| File | Role |
|------|------|
| `RetraAppUi.kt` | `RetraApp` root, nav dock/rail, import orchestration |
| `RetraHomeUi.kt` | Home / Continue |
| `RetraLibraryUi.kt` | Library |
| `RetraDiscoverUi.kt` | Discover, Drift, patch guides, homebrew |
| `RetraProfileUi.kt` | Profile |
| `RetraGameDetailsUi.kt` | Game sheet + patch dialog |
| `RetraOnboardingUi.kt` | First-run |
| `RetraSettingsUi.kt` | Settings |
| `RetraToolsUi.kt` | Controller Studio, Save Timeline, Performance Advisor |
| `RetraSharedUi.kt` | Shared library cards / helpers |
| `PlayerUi.kt` | Immersive player |
| `ui/components/RetraComponents.kt` | Panel, title, badge, empty state |
| `GlassUi.kt` / `BrandUi.kt` / `ControllerUi.kt` | Shared chrome |

Entry: `MainActivity` → `RetraApp`.

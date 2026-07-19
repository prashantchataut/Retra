# Feedback, sound, and notifications

## Semantic feedback map

| Event | Haptic | Sound |
|---|---|---|
| Navigation / selection | Tick | `retra_tap.wav` |
| Game touch button | Tick | None, to avoid masking game audio |
| Confirmation | Click | `retra_confirm.wav` |
| Save/state protection | Click | `retra_save.wav` |
| Achievement | Double click | `retra_achievement.wav` |
| Multiplayer invitation | Double click | `retra_invite.wav` |
| Error | Heavy click | `retra_error.wav` |

Frequent events intentionally use the lightest effects. Every category is controlled by the user's Retra settings.

## Sound assets

All cues are original project assets, 16-bit mono PCM at 22,050 Hz, and shorter than 0.5 seconds. `SoundPool` is used because the cues are short and latency-sensitive. Playback occurs only after the asynchronous load-complete callback confirms the sample is ready.

## Notification channels

- Achievements — default importance, optional celebration sound and vibration.
- Library and downloads — low importance and quiet.
- Multiplayer — default importance, invite sound and vibration.
- Saves and protection — low importance and quiet.

Android owns channel settings after creation. Retra provides a direct route to the system notification settings and does not override the user's choices.

## Permission policy

On Android 13+, `POST_NOTIFICATIONS` is requested only after the user opens the Alerts category and chooses to allow notifications. Local play, imports, saves, and all offline functionality work when permission is denied.

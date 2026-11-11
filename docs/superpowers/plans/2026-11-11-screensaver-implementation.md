# Screensaver implementation plan

**Goal:** Add configurable idle screensavers using the existing approved design, without restoring the removed Control Center button.

**Architecture:** Share one lifecycle-managed rendering view between a launcher activity and the existing Android DreamService. Home schedules the activity only while foreground and idle; settings remain parent-only. Images come from app-specific wallpaper storage, with bounded background decoding and a built-in fallback. Video playback must fall back safely when unavailable.

**Tasks**

1. Add validated mode, timeout and overlay preferences with unit tests.
2. Build the shared clock, wallpaper slideshow and video rendering engine; release timers and resources on stop, and reuse it from both entry points.
3. Add the idle watchdog, reset it on interaction, cancel it on pause, and consume the wake key only in the screensaver activity.
4. Add parent-only settings, preview and system screensaver settings while preserving dimmer controls.
5. Run unit tests and release builds, inspect the diff, and record device-validation limitations.

**Constraints:** Preserve native accessibility handling of protected keys, Kid Mode settings lock, existing system Dream component, and the user's removal of the Control Center action. Do not change the emulator without a snapshot. The FamilyTime restart/dream issue is a separate cross-application concern and must not be obscured by a guessed state.

# Screensaver

In Parent Mode, open Launcher Settings → Display & Screensaver. Choose Off, Clock (Fliqlo retro flip clock), or Wallpapers (4K wallpaper slideshow with slow zoom); set the Home-screen inactivity timeout to Off, 2, 5, 10, or 15 minutes; and choose whether to show the clock overlay. Preview and Android system screensaver settings are available on the same page. The controls are unavailable in Kid Mode, but the saved idle behavior still applies.

The idle timer runs only while the launcher Home screen has focus. A remote key or touch dismisses the in-launcher screensaver. Android can also run the launcher's `Clock` dream after it is selected in system screensaver settings; timing outside Home is controlled by Android.

Wallpapers are read from the launcher's external `files/wallpapers` directory (`.jpg`), with `files/wallpaper.jpg` and a dark background as fallbacks.

To validate on an emulator or TV, preview each mode, change the timeout, leave Home idle, wake it with the remote, and check that playback in another app is not interrupted by the Home timer. Verify Kid Mode cannot open launcher settings. The FamilyTime service-restart issue described in the earlier review is separate and remains unresolved.

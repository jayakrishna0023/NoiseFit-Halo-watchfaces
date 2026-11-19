# Lumina — living watch faces for NoiseFit Halo

> Formerly *Halo Studio*. **Download:** [release/Lumina.apk](release/Lumina.apk) (public build — import your own templates & user ID, see Setup).

Open-source watch-face designer for the **NoiseFit Halo** (model NSW-301, ZH/"Apricot" protocol, Realtek chip).
Design faces on your Android phone — photos, gradients, fonts, colours, stickers, text — and install them
over Bluetooth, without the official app's paid customisation.

> **Not affiliated with Noise / NoiseFit.** This is an independent interoperability project for your own
> device. It ships **no Noise artwork, firmware, keys or personal data**. Use at your own risk.
> Tested only on: NoiseFit Halo NSW-301, firmware V1.3.0, Android 16 (vivo V60e).

## Features
| Area | What you can do |
|---|---|
| Gallery | **102 preinstalled faces** in 10 categories (Alien Tech, Cosmic, Space, Nature, Luxury, Cyber, Neon, Abstract, Minimal, Retro), search, face of the day, **🎲 Surprise** (random readable face), animated detail view with live time |
| Generative art | **33 procedural art styles** (nebula, aurora, dunes, synthwave, planet, bokeh, mesh, topography, low-poly, waves, rings, sunburst, marble, starfield, halftone, minimal, black hole, liquid chrome, flow field, mountains, crystal, neon tunnel, glass, spiral galaxy, circuit, kaleidoscope, code rain, fireflies, sunset sea, Bauhaus, xeno core, HUD scanner, plasma reactor) × palettes × seeds — all original, generated on the phone |
| Layouts | **Galaxy** (big clock, movable) · **Moon** (stacked clock + AM/PM on tiles) — from your own captured faces |
| Background | Generative art · photo with **interactive crop** (drag, pinch, rotate) · 10 gradients + custom · solid colour |
| Effects | Brightness, contrast, saturation, blur, vignette, black & white |
| Clock | Time/date colours, **gradient time**, Moon tile colour, Galaxy clock position (experimental) |
| Font | 10 system font styles or **your own .ttf/.otf**, bold/italic, size, outline, glow, separator, uppercase |
| Labels | Weekdays (EN + 中文) and AM/PM are redrawn in your font too |
| Stickers & text | Emoji/symbol stickers (drag, pinch, rotate) and free text |
| Face list | Optionally writes your design as the watch's 310×310 **face-picker preview** (experimental, off by default) |
| AOD (experimental) | Read the watch's always-on settings; set mode (always / scheduled / off), schedule and analog/digital style |
| Live previews | Animated in-app previews: twinkling & shooting stars, bokeh, code rain, fireflies, embers, shimmer, live seconds ring, 3D tilt; animated logo & launch screen |
| Editing | Hue shift, frames (thin / neon / minute ticks / double ring), textures (grain, scanlines, glass sheen, dot grid), random harmonious palettes, reverse/darken palette |
| Layouts+ | Tile layout uses rounded glass panels instead of hard boxes · optional centred clock for the round screen · 12/24-hour preview setting |
| Look & feel | Bright beige theme (dark theme in Settings) · responsive layout: 2/3/4-column grids by screen width, previews sized to the screen height, centred column on tablets/foldables |
| Install | Progress %, time left, **cancel at any moment** (watch keeps its current face) |
| My faces | Save any face, reopen, customize, install, delete |

## Safety model
- Every face is built and **fully re-validated on the phone** before sending (LZO round-trip, both checksums,
  only allowed bytes changed — `Composer.verifyAllowed`).
- One write gate (`WatchLink.guardedWrite`) only allows: the user check, the watch-face pre-check, AOD read/write
  and the pieces of the face just built. The firmware/DFU service is **not referenced anywhere**.
- The watch itself answers READY / BUSY / LOW_BATTERY before any transfer and rejects faces it doesn't trust
  (it falls back to its default face — use *Restore* or the official app).
- AOD write is only enabled after re-encoding the watch's own reply reproduces it **byte for byte**.

## Setup
1. **Capture your watch's data** (one time): enable *Developer options → Bluetooth HCI snoop log → Enabled*
   on an Android phone where it works (not all OEMs write it — vivo works; Realme/OPPO didn't), install the
   Galaxy and/or Moon face once in NoiseFit, then pull a bug report (`adb bugreport out.zip`).
2. `python tools/extract_face.py out.zip faces/` → official face files (`face_<id>_type16_*.bin`).
3. `python tools/find_user_id.py out.zip` → your NoiseFit user ID.
4. In Lumina: *Watch → Settings* → paste the user ID; *Import template file* → pick the face file(s).

## Build
Plain Android SDK command-line tools (no Gradle): JDK 17+, `platforms;android-35`, `build-tools;35.0.0`.
```powershell
cd app
powershell -ExecutionPolicy Bypass -File build.ps1   # → build/Lumina.apk
```
Optional for personal builds: put `template_galaxy.raw`, `template_moon.raw` and
`local_config.json` (`{"userToken": "...", "watchAddress": "..."}`) in `app/private_assets/`
(git-ignored, never publish it).

Offline tests (need your own captured templates in `faces/`): `javac -d out app/src/com/jk/halostudio/core/*.java app/pctest/PresetTest.java && java -cp out PresetTest`

## How it works
See [docs/PROTOCOL.md](docs/PROTOCOL.md): BLE services, small-message framing, upload framing + CRC32,
the `AF AF AF 02` LZO1X container and its checksum, face element maps, preview image, AOD protobuf.

## Not yet supported
Video/animated faces, other watch models, changing the AOD look itself (the AOD styles are built into the firmware).

## License
MIT — see [LICENSE](LICENSE).

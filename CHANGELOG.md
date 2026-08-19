# Changelog

## 0.1.0-beta.1 - 2026-08-19

- Added FPS-independent animated hotbar selector while preserving the original draw call.
- Added safe opening animation for `GuiContainer`; any input immediately completes it.
- Added Unicode GUI-scale correction and expanded Unicode font assets.
- Added an F8 GUI catalog with bytecode-only discovery, mod filtering, search, runtime observation, and per-class animation policies.
- Added fail-open ASM diagnostics and safe mode.
- Added localized per-GUI animation editor with duration, offset, easing and reset controls.
- Reworked hotbar motion: nearby slots use a sub-pixel spring, long jumps crossfade without travelling across the entire bar, and the captured resource-pack selector renders above items.
- Added advance discovery of Minecraft, Forge, OptiFine and installed-mod GUI classes without opening each screen first.
- Added an original MotionUI project icon and Forge ModList logo.

### Known issues

- Some IndustrialCraft 2 interfaces may show dark artifacts around individual Unicode glyphs. This is planned for a future compatibility update.

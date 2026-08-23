# Changelog

## 0.2.0 - 2026-08-24

- Fixed black rectangles behind some Cyrillic glyphs, most visibly in creative
  inventory tab titles, by removing stray near-transparent black pixels from
  the HD Unicode atlas without changing its resolution or glyph metrics.
- Expanded ASM regression coverage for unknown, ambiguous, repeated and
  obfuscated transformation inputs, and accepted the obfuscated `GuiIngame`
  class name independently of transformer order.
- Added a client-only closing animation that fades a GPU snapshot after the
  underlying container has already closed; input and screen transitions cancel
  the visual immediately.
- Added global Off, Subtle, Smooth and Expressive animation profiles to the F8
  catalog while preserving per-GUI overrides and existing custom settings.
- Moved global options to a dedicated settings screen and added Disabled,
  Simplified and Full closing modes. Simplified renders a non-blocking snapshot;
  Full blocks movement and camera control until the animation completes.
- Reworked the F8 catalog with persistent search, filters, sorting and scroll,
  compact inherited opening/closing summaries, and independent per-GUI rules.
- Added a diagnostics screen with ASM patch states, Forge/OptiFine versions,
  active font resource providers and a copyable compatibility report.
- Added optional JEI 1.12 integration: the ingredient-list overlay remains
  stationary during opening animations while JEI recipe screens animate. On
  close, the right-side JEI region stays fixed and fades quickly while the main
  GUI keeps its configured duration and offset.
- Rebuilt the per-GUI editor with vanilla scalable buttons, dedicated opening
  and closing tabs, tooltip-style panels and read-only duration/offset fields.
- Added reverse cycling with the right mouse button to profiles, filters,
  sorting, policies, closing modes and easing; left click cycles forward.
- Added formatted explanatory tooltips for profiles, closing modes, catalog
  sorting and the configured-only filter.
- Hid MotionUI's internal closing-snapshot screen from the GUI catalog and
  extended shifted closing snapshots at the top edge to avoid a visible gap.

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

# Changelog

## 0.3.0 - 2026-08-26

- Added slide, scale and combined scale + slide styles for GUI opening and
  closing animations.
- Added up, down, left and right animation directions while preserving existing
  0.2.x rules as slide-up animations.
- Extended per-GUI rule editing and configuration serialization with the new
  style and direction fields.
- Added animation rules at the mod ID level with deterministic class, mod and
  global inheritance.
- Added rule copy/paste and bulk application to every discovered GUI belonging
  to a mod.
- Added a representative animation preview with automatic replay, manual
  replay and a draggable progress scrubber. Compact layouts open the same
  preview on a dedicated screen.
- Added an optional redesigned F3 overlay using the same panels, cards, spacing
  and palette as the MotionUI F8 interface. Vanilla F3 remains the default.
- Added Compact, Standard and Full F3 profiles, switchable from settings or by
  holding F3 and pressing Shift, plus a persistent shortcut footer.
- Expanded Full F3 with local/chunk/region coordinates, light, surface height,
  difficulty, chunk cache, target block/fluid properties and connection data.
- Added sampled average/low FPS, cached system information, throttled memory
  readings and text-plus-color memory pressure warnings.
- Replaced full-screen closing snapshots and mirrored edge skirts with a
  transparent GUI-only framebuffer guarded by a narrow client ASM hook.
- Added a comfort envelope for closing animations: perceptually smooth opacity,
  zero-jerk motion boundaries, duration/resolution-aware transform limits and
  gentler scaling.
- Fixed an LWJGL 2 state-query buffer size crash when closing a GUI.
- Fade the GUI from the first closing frame in sync with the background while
  protecting a real frame-time budget and reducing scale shimmer.
- Preserve vanilla's stationary world-dimming gradient during closing and fade
  it independently instead of transforming it with the GUI snapshot.
- Restore the stationary JEI closing snapshot captured at the end of the
  background phase, before container and JEI tooltips are rendered.
- Limit container-layer composition to padded GUI bounds and reject residual
  low-alpha RGB during fading, preventing OptiFine from ghosting the live world.
- Stop overwriting the fixed-function texture environment after closing frames;
  this avoids OptiFine foliage flicker when shaders are disabled.
- Preserve and restore the actual OptiFine end-of-frame GL state around both
  dimming and GUI composition instead of forcing vanilla default state.

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

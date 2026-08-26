# MotionUI Roadmap

This roadmap records the planned scope of upcoming MotionUI releases. Items may
change during implementation when Minecraft 1.12.2, Forge, OptiFine or mod GUI
compatibility requires a safer fallback.

## 0.3.0 — Animation Customization

- [x] Add `AnimationStyle` options for slide, scale and combined scale + slide
  transitions.
- [x] Add direction selection for directional animation styles.
- [x] Add an interactive preview to the animation rule editor.
- [x] Add animation rules at the mod ID level, inherited by that mod's GUI classes
  unless a class-specific rule overrides them.
- [x] Add copying and bulk application of animation settings, including applying a
  rule to multiple screens or all screens belonging to a mod.

### Interactive preview design

- Render an isolated representative GUI card rather than instantiating the
  selected mod screen. Many 1.12.2 screens require a live container, world,
  tile entity or mod-owned state, so constructing them from a settings screen
  would be unsafe and would make preview availability mod-dependent.
- On wide screens, place the preview in a right-hand editor pane. On compact
  GUI scales, expose the same pane through one clearly labelled Preview button
  instead of shrinking the controls or layering them over one another.
- Reuse the production `AnimationTransform` and easing calculation so the
  preview matches the real transition. Do not maintain a second approximation
  of duration, scale or slide math.
- Replay automatically after a setting changes, with an explicit Replay control
  and a draggable progress scrubber for inspecting the beginning and end of a
  transition. Opening and Closing use the currently selected editor tab.
- Preview only the GUI layer, dim background and optional JEI side regions as
  separate visual layers. This makes their timing visible without capturing a
  real framebuffer or invoking third-party rendering code.

## 0.4.0 — HUD Motion

- Animate experience changes.
- Animate health and armor changes.
- Animate notifications as they appear.
- Add chat motion effects.
- Animate the boss bar.
- Animate held-item switching.

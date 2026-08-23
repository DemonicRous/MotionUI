# MotionUI

![MotionUI logo](docs/media/motionui-logo.png)

Modern, configurable UI animations for Minecraft Forge 1.12.2.

MotionUI is a lightweight client-side mod that makes legacy Minecraft interfaces feel smoother while keeping control in the player's hands. It supports vanilla screens and GUI classes added by other mods.

## Features

- Smooth, FPS-independent GUI opening animations.
- Safe closing animation that never delays the actual container close.
- Per-GUI enable, disable, duration, offset and easing settings.
- F8 catalog that discovers GUI classes from Minecraft, Forge, OptiFine and installed mods before they are opened.
- Filtering by mod and class-name search.
- Smooth hotbar selector with short-path motion and long-jump crossfade.
- Original selector textures from resource packs and compatible HUD renderers are preserved.
- Unicode GUI-scale correction and expanded font resources.
- English and Russian localization.
- Fail-open ASM diagnostics: incompatible visual patches are skipped instead of crashing the client.

## Requirements

- Minecraft 1.12.2
- Forge 14.23.5.2847 or newer
- Java 8

MotionUI is client-side only and does not need to be installed on a server.

## Installation

1. Install Forge for Minecraft 1.12.2.
2. Place the MotionUI release JAR in the instance's `mods` directory.
3. Start the game and press F8 to open the GUI catalog.

Only files ending in `-release.jar` are intended for normal Minecraft installations. The unqualified JAR produced during development may contain development mappings.

## GUI catalog

The F8 catalog scans installed bytecode without constructing or initializing GUI classes. A hollow marker means a screen was discovered in advance; a green marker means it has also been observed during play.

Use the mod filter and search box to find a screen. The state button controls whether the global animation policy is inherited, explicitly enabled or disabled. The gear button opens per-screen animation settings and reset controls.

## Compatibility

MotionUI has been tested with Forge 14.23.5.2847/2864 and OptiFine 1.12.2 HD U G5. Because legacy coremods and custom HUD renderers may transform the same Minecraft classes, new compatibility reports should include `latest.log`, the Forge version and the full mod list.

## Building

Use JDK 8:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-8.0.502.7-hotspot'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat clean test build
```

The verified production artifact is written to `build/libs/MotionUI-<version>-release.jar`.

## License

MotionUI is available under the [MIT License](LICENSE).

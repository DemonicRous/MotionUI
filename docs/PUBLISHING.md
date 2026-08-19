# Publishing MotionUI

## Common release data

- Name: MotionUI
- Mod ID: `motionui`
- Minecraft: 1.12.2
- Loader: Forge
- Side: Client
- Java: 8
- License: MIT
- Required dependency: Forge 14.23.5.2847 or newer
- Source: https://github.com/DemonicRous/MotionUI
- Primary artifact: `build/libs/MotionUI-<version>.jar`

## CurseForge

Create the project later under Minecraft Mods, select MIT, Forge, client-side, and Minecraft 1.12.2. Use the short and long descriptions from the README. Upload the reobfuscated primary JAR, not the sources JAR. Mark 1.12.2 and Forge explicitly.

## Modrinth

Create the project later with slug `motionui`, MIT, client-side=required, server-side=unsupported, environment Forge/1.12.2. Upload the same reobfuscated JAR and copy the current changelog section into the version notes.

## Release checklist

1. Build with JDK 8: `gradlew clean test build`.
2. Test Forge 14.23.5.2847 and 14.23.5.2860, with and without OptiFine G5.
3. Test F8 catalog, Unicode font, odd GUI scales, containers, JEI, and Mouse Tweaks.
4. Update version and changelog.
5. Tag `v<version>` and publish the primary JAR plus sources JAR on GitHub.
6. Reuse the primary JAR on CurseForge and Modrinth.

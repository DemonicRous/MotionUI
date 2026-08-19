package com.demonicrous.motionui.core;

import java.io.*;
import java.util.Properties;

/** Tiny early config read before Forge's normal config lifecycle. */
public final class MotionUIEarlyConfig {
    private static boolean safeMode;
    private static boolean unicode = true;
    private static boolean hotbar = true;
    private MotionUIEarlyConfig() {}
    public static synchronized void load(File gameDir) {
        Properties p = new Properties();
        File file = new File(new File(gameDir == null ? new File(".") : gameDir, "config"), "motionui-core.properties");
        if (file.isFile()) try (InputStream in = new FileInputStream(file)) { p.load(in); } catch (IOException ignored) {}
        safeMode = Boolean.parseBoolean(System.getProperty("motionui.safeMode", p.getProperty("core.safeMode", "false")));
        unicode = Boolean.parseBoolean(p.getProperty("core.unicodeGuiScale", "true"));
        hotbar = Boolean.parseBoolean(p.getProperty("core.hotbarPatch", "true"));
    }
    public static boolean isSafeModeEnabled() { return safeMode; }
    public static boolean isUnicodeGuiScaleEnabled() { return unicode; }
    public static boolean isHotbarAnimationEnabled() { return hotbar && !safeMode; }
    public static boolean isDiagnosticLoggingEnabled() { return true; }
    static void setUnicodeEnabledForTests(boolean value) { unicode=value; }
}

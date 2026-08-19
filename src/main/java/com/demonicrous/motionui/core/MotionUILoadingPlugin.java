package com.demonicrous.motionui.core;

import java.io.File;
import java.util.Map;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.Name("MotionUI")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions("com.demonicrous.motionui.core")
public final class MotionUILoadingPlugin implements IFMLLoadingPlugin {
    public String[] getASMTransformerClass() {
        PatchDiagnostics.register("unicode_gui_scale");
        PatchDiagnostics.register("animated_hotbar_selector");
        if (MotionUIEarlyConfig.isSafeModeEnabled()) {
            PatchDiagnostics.safeMode("unicode_gui_scale");
            PatchDiagnostics.safeMode("animated_hotbar_selector");
            return new String[0];
        }
        return new String[] { UnicodeGuiScaleTransformer.class.getName(), HotbarSelectorTransformer.class.getName() };
    }
    public String getModContainerClass() { return null; }
    public String getSetupClass() { return null; }
    public void injectData(Map<String, Object> data) {
        Object location = data.get("mcLocation");
        MotionUIEarlyConfig.load(location instanceof File ? (File) location : null);
    }
    public String getAccessTransformerClass() { return null; }
}

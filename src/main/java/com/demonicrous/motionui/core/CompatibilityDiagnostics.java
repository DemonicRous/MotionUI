package com.demonicrous.motionui.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;

/** Inspects the public LaunchWrapper transformer list without modifying it. */
public final class CompatibilityDiagnostics {
    private CompatibilityDiagnostics() {
    }

    public static List<String> transformerClassNames() {
        if (Launch.classLoader == null) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<String>();
        for (IClassTransformer transformer : Launch.classLoader.getTransformers()) {
            if (transformer != null) {
                names.add(transformer.getClass().getName());
            }
        }
        return Collections.unmodifiableList(names);
    }

    public static List<String> thirdPartyTransformerClassNames() {
        return thirdPartyTransformerClassNames(transformerClassNames());
    }

    static List<String> thirdPartyTransformerClassNames(List<String> names) {
        List<String> thirdParty = new ArrayList<String>();
        for (String name : names) {
            if (!isPlatformOrMotionUITransformer(name)) {
                thirdParty.add(name);
            }
        }
        return Collections.unmodifiableList(thirdParty);
    }

    private static boolean isPlatformOrMotionUITransformer(String name) {
        String normalized = name;
        while (normalized.startsWith("$wrapper.")) {
            normalized = normalized.substring("$wrapper.".length());
        }
        return normalized.startsWith("com.demonicrous.motionui.")
                || normalized.startsWith("net.minecraftforge.")
                || normalized.startsWith("net.minecraft.")
                || normalized.startsWith("cpw.mods.");
    }
}


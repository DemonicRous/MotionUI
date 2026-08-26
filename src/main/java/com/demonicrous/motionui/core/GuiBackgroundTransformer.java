package com.demonicrous.motionui.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Adds the one missing hook needed to render a GUI without its world-dimming background. */
public final class GuiBackgroundTransformer implements IClassTransformer {
    public static final String PATCH = "transparent_gui_layer";
    private static final Logger LOGGER = LogManager.getLogger("MotionUI/ASM");
    private static final String TARGET = "net.minecraft.client.gui.GuiScreen";
    private static final String OBF_TARGET = "blk";
    private static final String HELPER =
            "com/demonicrous/motionui/client/GuiBackgroundControl";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !isTarget(name, transformedName)) {
            return basicClass;
        }
        if (!MotionUIEarlyConfig.isTransparentGuiLayerEnabled()) {
            PatchDiagnostics.disabled(PATCH);
            LOGGER.info("Transparent GUI-layer patch is disabled");
            return basicClass;
        }

        try {
            ClassReader reader = new ClassReader(basicClass);
            ClassNode node = new ClassNode();
            reader.accept(node, 0);
            MethodNode target = null;
            int matches = 0;
            int existingHooks = 0;

            for (MethodNode method : node.methods) {
                if (isBackgroundMethod(method)) {
                    target = method;
                    matches++;
                }
                existingHooks += countHooks(method);
            }

            if (existingHooks == 1 && matches == 1) {
                PatchDiagnostics.applied(PATCH, "background capture guard already present");
                return basicClass;
            }
            if (existingHooks != 0 || matches != 1 || target == null) {
                String detail = "expected one background method and no hooks, found methods="
                        + matches + ", hooks=" + existingHooks;
                PatchDiagnostics.skipped(PATCH, detail);
                LOGGER.warn("Transparent GUI-layer patch skipped: {}", detail);
                return basicClass;
            }

            LabelNode drawNormally = new LabelNode();
            InsnList guard = new InsnList();
            guard.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HELPER,
                    "shouldSuppressBackground", "()Z", false));
            guard.add(new JumpInsnNode(Opcodes.IFEQ, drawNormally));
            guard.add(new InsnNode(Opcodes.RETURN));
            guard.add(drawNormally);
            target.instructions.insert(guard);

            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
            node.accept(writer);
            PatchDiagnostics.applied(PATCH, "guarded one GuiScreen background method");
            LOGGER.info("Applied transparent GUI-layer background guard");
            return writer.toByteArray();
        } catch (RuntimeException error) {
            PatchDiagnostics.failed(PATCH, error);
            LOGGER.error("Transparent GUI-layer patch failed; closing animation is disabled", error);
            return basicClass;
        }
    }

    private static boolean isTarget(String name, String transformedName) {
        return TARGET.equals(transformedName) || TARGET.equals(name) || OBF_TARGET.equals(name);
    }

    private static boolean isBackgroundMethod(MethodNode method) {
        return "(I)V".equals(method.desc)
                && ("drawWorldBackground".equals(method.name)
                || "func_146270_b".equals(method.name)
                || "d_".equals(method.name));
    }

    private static int countHooks(MethodNode method) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (call.getOpcode() == Opcodes.INVOKESTATIC && HELPER.equals(call.owner)
                        && "shouldSuppressBackground".equals(call.name)
                        && "()Z".equals(call.desc)) {
                    count++;
                }
            }
        }
        return count;
    }
}

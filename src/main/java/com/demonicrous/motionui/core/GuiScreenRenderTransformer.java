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
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Wraps Forge's complete GUI draw, including mod Pre/Post overlays such as JEI. */
public final class GuiScreenRenderTransformer implements IClassTransformer {
    public static final String PATCH = "single_pass_gui_capture";
    private static final Logger LOGGER = LogManager.getLogger("MotionUI/ASM");
    private static final String TARGET = "net.minecraftforge.client.ForgeHooksClient";
    private static final String GUI_SCREEN = "net/minecraft/client/gui/GuiScreen";
    private static final String OBF_GUI_SCREEN = "blk";
    private static final String BRIDGE =
            "com/demonicrous/motionui/client/GuiScreenRenderBridge";
    private static final String DRAW = "drawScreen";
    private static final String SCREEN_DRAW_DESC = "(IIF)V";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !TARGET.equals(name) && !TARGET.equals(transformedName)) {
            return basicClass;
        }
        if (!MotionUIEarlyConfig.isTransparentGuiLayerEnabled()) {
            PatchDiagnostics.disabled(PATCH);
            return basicClass;
        }

        try {
            ClassReader reader = new ClassReader(basicClass);
            ClassNode node = new ClassNode();
            reader.accept(node, 0);
            MethodNode target = null;
            String targetScreen = null;
            int methods = 0;
            int beginHooks = 0;
            int endHooks = 0;
            int mouseHooks = 0;
            int screenCalls = 0;

            for (MethodNode method : node.methods) {
                String methodScreen = screenArgument(method.desc);
                if (DRAW.equals(method.name) && methodScreen != null) {
                    target = method;
                    targetScreen = methodScreen;
                    methods++;
                }
                for (AbstractInsnNode instruction : method.instructions.toArray()) {
                    if (!(instruction instanceof MethodInsnNode)) continue;
                    MethodInsnNode call = (MethodInsnNode) instruction;
                    if (method == target && call.getOpcode() == Opcodes.INVOKEVIRTUAL
                            && targetScreen != null && targetScreen.equals(call.owner)
                            && SCREEN_DRAW_DESC.equals(call.desc)
                            && (DRAW.equals(call.name) || "func_73863_a".equals(call.name)
                            || "a".equals(call.name))) {
                        screenCalls++;
                    }
                    if (call.getOpcode() != Opcodes.INVOKESTATIC || !BRIDGE.equals(call.owner)) {
                        continue;
                    }
                    if ("beginFrame".equals(call.name)) beginHooks++;
                    else if ("endFrame".equals(call.name)) endHooks++;
                    else if ("adjustMouseCoordinate".equals(call.name)) mouseHooks++;
                }
            }

            if (methods == 1 && screenCalls == 1 && beginHooks == 1
                    && endHooks == 1 && mouseHooks == 2) {
                PatchDiagnostics.applied(PATCH, "complete Forge GUI wrapper already present");
                return basicClass;
            }
            if (methods != 1 || target == null || screenCalls != 1
                    || beginHooks != 0 || endHooks != 0
                    || mouseHooks != 0 || countReturns(target) != 1) {
                String detail = "expected one unpatched Forge GUI method/return, found methods="
                        + methods + ", returns=" + (target == null ? 0 : countReturns(target))
                        + ", screen=" + screenCalls
                        + ", begin=" + beginHooks + ", end=" + endHooks
                        + ", mouse=" + mouseHooks;
                PatchDiagnostics.skipped(PATCH, detail);
                LOGGER.warn("Single-pass GUI wrapper skipped: {}", detail);
                return basicClass;
            }

            InsnList prefix = new InsnList();
            String screenDescriptor = "L" + targetScreen + ";";
            prefix.add(new VarInsnNode(Opcodes.ALOAD, 0));
            prefix.add(new VarInsnNode(Opcodes.ILOAD, 1));
            prefix.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BRIDGE,
                    "adjustMouseCoordinate", "(" + screenDescriptor + "I)I", false));
            prefix.add(new VarInsnNode(Opcodes.ISTORE, 1));
            prefix.add(new VarInsnNode(Opcodes.ALOAD, 0));
            prefix.add(new VarInsnNode(Opcodes.ILOAD, 2));
            prefix.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BRIDGE,
                    "adjustMouseCoordinate", "(" + screenDescriptor + "I)I", false));
            prefix.add(new VarInsnNode(Opcodes.ISTORE, 2));
            prefix.add(new VarInsnNode(Opcodes.ALOAD, 0));
            prefix.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BRIDGE,
                    "beginFrame", "(" + screenDescriptor + ")V", false));
            target.instructions.insert(prefix);

            for (AbstractInsnNode instruction : target.instructions.toArray()) {
                if (instruction.getOpcode() != Opcodes.RETURN) continue;
                InsnList suffix = new InsnList();
                suffix.add(new VarInsnNode(Opcodes.ALOAD, 0));
                suffix.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BRIDGE,
                        "endFrame", "(" + screenDescriptor + ")V", false));
                target.instructions.insertBefore(instruction, suffix);
            }

            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_MAXS);
            node.accept(writer);
            PatchDiagnostics.applied(PATCH, "wrapped Forge GUI draw including mod overlays");
            LOGGER.info("Applied complete single-pass Forge GUI wrapper");
            return writer.toByteArray();
        } catch (RuntimeException error) {
            PatchDiagnostics.failed(PATCH, error);
            LOGGER.error("Single-pass Forge GUI wrapper failed", error);
            return basicClass;
        }
    }

    private static String screenArgument(String descriptor) {
        String deobfuscated = "(L" + GUI_SCREEN + ";IIF)V";
        if (deobfuscated.equals(descriptor)) return GUI_SCREEN;
        String obfuscated = "(L" + OBF_GUI_SCREEN + ";IIF)V";
        return obfuscated.equals(descriptor) ? OBF_GUI_SCREEN : null;
    }

    private static int countReturns(MethodNode method) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof InsnNode && instruction.getOpcode() == Opcodes.RETURN) count++;
        }
        return count;
    }
}

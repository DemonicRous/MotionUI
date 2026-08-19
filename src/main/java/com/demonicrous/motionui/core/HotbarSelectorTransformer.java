package com.demonicrous.motionui.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Replaces only the vanilla selector's final X coordinate with an animated one. */
public final class HotbarSelectorTransformer implements IClassTransformer {
    private static final Logger LOGGER = LogManager.getLogger("MotionUI/ASM");
    private static final String PATCH = "animated_hotbar_selector";
    private static final String TARGET = "net.minecraft.client.gui.GuiIngame";
    private static final String HELPER =
            "com/demonicrous/motionui/client/HotbarAnimationController";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !TARGET.equals(transformedName)
                && !TARGET.equals(name)) {
            return basicClass;
        }
        try {
            ClassReader reader = new ClassReader(basicClass);
            ClassNode node = new ClassNode();
            reader.accept(node, 0);
            int matches = 0;
            for (MethodNode method : node.methods) {
                for (AbstractInsnNode instruction : method.instructions.toArray()) {
                    if (isSelectorSlotField(instruction)) {
                        AbstractInsnNode twenty = nextReal(instruction);
                        AbstractInsnNode multiply = nextReal(twenty);
                        AbstractInsnNode add = nextReal(multiply);
                        if (isInt(twenty, 20) && multiply != null
                                && multiply.getOpcode() == Opcodes.IMUL && add != null
                                && add.getOpcode() == Opcodes.IADD) {
                            method.instructions.insert(add, new MethodInsnNode(
                                    Opcodes.INVOKESTATIC, HELPER,
                                    "adjustSelectorX", "(I)I", false));
                            matches++;
                        }
                    }
                }
            }
            if (matches != 1) {
                String detail = "expected one hotbar selector coordinate, found " + matches;
                PatchDiagnostics.skipped(PATCH, detail);
                LOGGER.warn("Hotbar animation patch skipped: {}", detail);
                return basicClass;
            }
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
            node.accept(writer);
            PatchDiagnostics.applied(PATCH, "animated vanilla selector X coordinate");
            LOGGER.info("Applied animated hotbar selector patch");
            return writer.toByteArray();
        } catch (RuntimeException error) {
            PatchDiagnostics.failed(PATCH, error);
            LOGGER.error("Hotbar animation patch failed; using original class", error);
            return basicClass;
        }
    }

    private static boolean isSelectorSlotField(AbstractInsnNode instruction) {
        if (!(instruction instanceof FieldInsnNode)
                || instruction.getOpcode() != Opcodes.GETFIELD) {
            return false;
        }
        FieldInsnNode field = (FieldInsnNode) instruction;
        return "I".equals(field.desc) && ("currentItem".equals(field.name)
                || "field_70461_c".equals(field.name) || "c".equals(field.name));
    }

    private static boolean isInt(AbstractInsnNode instruction, int value) {
        return instruction instanceof IntInsnNode
                && ((IntInsnNode) instruction).operand == value;
    }

    private static AbstractInsnNode nextReal(AbstractInsnNode instruction) {
        AbstractInsnNode next = instruction == null ? null : instruction.getNext();
        while (next != null && next.getOpcode() < 0) {
            next = next.getNext();
        }
        return next;
    }

}


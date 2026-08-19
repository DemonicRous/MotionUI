package com.demonicrous.motionui.core;

import net.minecraft.launchwrapper.IClassTransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Captures the vanilla selector draw so it can be composited smoothly above the items. */
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
                    if(instruction instanceof MethodInsnNode&&isSelectorDraw((MethodInsnNode)instruction)){
                        MethodInsnNode draw=(MethodInsnNode)instruction;
                        draw.setOpcode(Opcodes.INVOKESTATIC);draw.owner=HELPER;draw.name="deferSelector";draw.desc="(Lnet/minecraft/client/gui/Gui;IIIIII)V";draw.itf=false;matches++;
                    }
                }
            }
            if (matches != 1) {
                String detail = "expected one hotbar selector draw, found " + matches;
                PatchDiagnostics.skipped(PATCH, detail);
                LOGGER.warn("Hotbar animation patch skipped: {}", detail);
                return basicClass;
            }
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
            node.accept(writer);
            PatchDiagnostics.applied(PATCH, "captured original selector for post-item overlay");
            LOGGER.info("Applied animated hotbar selector patch");
            return writer.toByteArray();
        } catch (RuntimeException error) {
            PatchDiagnostics.failed(PATCH, error);
            LOGGER.error("Hotbar animation patch failed; using original class", error);
            return basicClass;
        }
    }

    private static boolean isInt(AbstractInsnNode instruction, int value) {
        if(instruction instanceof IntInsnNode)return ((IntInsnNode)instruction).operand==value;
        if(instruction instanceof LdcInsnNode)return ((LdcInsnNode)instruction).cst instanceof Integer&&((Integer)((LdcInsnNode)instruction).cst)==value;
        return instruction instanceof InsnNode&&value>=-1&&value<=5&&instruction.getOpcode()==Opcodes.ICONST_0+value;
    }

    private static AbstractInsnNode nextReal(AbstractInsnNode instruction) {
        AbstractInsnNode next = instruction == null ? null : instruction.getNext();
        while (next != null && next.getOpcode() < 0) {
            next = next.getNext();
        }
        return next;
    }

    private static AbstractInsnNode previousReal(AbstractInsnNode instruction){AbstractInsnNode p=instruction==null?null:instruction.getPrevious();while(p!=null&&p.getOpcode()<0)p=p.getPrevious();return p;}
    private static boolean isSelectorDraw(MethodInsnNode call){boolean owner="net/minecraft/client/gui/Gui".equals(call.owner)||"net/minecraft/client/gui/GuiIngame".equals(call.owner)||"bif".equals(call.owner)||"biq".equals(call.owner);if(call.getOpcode()!=Opcodes.INVOKEVIRTUAL||!owner||!"(IIIIII)V".equals(call.desc)||!("drawTexturedModalRect".equals(call.name)||"func_73729_b".equals(call.name)||"b".equals(call.name)))return false;AbstractInsnNode height=previousReal(call),width=previousReal(height),textureY=previousReal(width),textureX=previousReal(textureY);return isInt(height,22)&&isInt(width,24)&&isInt(textureY,22)&&isInt(textureX,0);}

}


package com.demonicrous.motionui.core;

import java.util.ArrayList;
import java.util.List;
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

/** Preserves an odd GUI scale when Minecraft uses its Unicode font renderer. */
public final class UnicodeGuiScaleTransformer implements IClassTransformer {
    private static final Logger LOGGER = LogManager.getLogger("MotionUI/ASM");
    private static final String PATCH = "unicode_gui_scale";
    private static final String DEOBF_TARGET = "net.minecraft.client.gui.ScaledResolution";
    private static final String OBF_TARGET = "bit";
    private static final String DEOBF_OWNER = "net/minecraft/client/Minecraft";
    private static final String OBF_OWNER = "bib";

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (basicClass == null || !isTarget(name, transformedName)) {
            return basicClass;
        }

        if (!MotionUIEarlyConfig.isUnicodeGuiScaleEnabled()) {
            PatchDiagnostics.disabled(PATCH);
            LOGGER.info("Unicode GUI-scale patch is disabled");
            return basicClass;
        }

        try {
            ClassReader reader = new ClassReader(basicClass);
            ClassNode classNode = new ClassNode();
            reader.accept(classNode, 0);
            List<Match> matches = findUnicodeChecks(classNode);

            if (matches.size() != 1) {
                String detail = "expected exactly one Minecraft.isUnicode() call, found "
                        + matches.size();
                PatchDiagnostics.skipped(PATCH, detail);
                LOGGER.warn("Unicode GUI-scale patch skipped: {}", detail);
                return basicClass;
            }

            Match match = matches.get(0);
            MethodInsnNode call = match.instruction;
            InsnList replacement = new InsnList();
            replacement.add(new InsnNode(Opcodes.POP));
            replacement.add(new InsnNode(Opcodes.ICONST_0));
            match.method.instructions.insertBefore(call, replacement);
            match.method.instructions.remove(call);

            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
            classNode.accept(writer);
            PatchDiagnostics.applied(PATCH, "replaced one Minecraft.isUnicode() call");
            LOGGER.info("Preserved odd Unicode GUI scales");
            return writer.toByteArray();
        } catch (RuntimeException error) {
            PatchDiagnostics.failed(PATCH, error);
            LOGGER.error("Unicode GUI-scale patch failed; using the original class", error);
            return basicClass;
        }
    }

    private static boolean isTarget(String name, String transformedName) {
        return DEOBF_TARGET.equals(transformedName)
                || DEOBF_TARGET.equals(name)
                || OBF_TARGET.equals(name);
    }

    private static List<Match> findUnicodeChecks(ClassNode classNode) {
        List<Match> matches = new ArrayList<Match>();
        for (MethodNode method : classNode.methods) {
            if (!"<init>".equals(method.name)) {
                continue;
            }
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof MethodInsnNode
                        && isUnicodeCheck((MethodInsnNode) instruction)) {
                    matches.add(new Match(method, (MethodInsnNode) instruction));
                }
            }
        }
        return matches;
    }

    private static boolean isUnicodeCheck(MethodInsnNode call) {
        return call.getOpcode() == Opcodes.INVOKEVIRTUAL
                && "()Z".equals(call.desc)
                && (DEOBF_OWNER.equals(call.owner) || OBF_OWNER.equals(call.owner))
                && ("isUnicode".equals(call.name)
                || "func_152349_b".equals(call.name)
                || "e".equals(call.name));
    }

    private static final class Match {
        private final MethodNode method;
        private final MethodInsnNode instruction;

        private Match(MethodNode method, MethodInsnNode instruction) {
            this.method = method;
            this.instruction = instruction;
        }
    }
}


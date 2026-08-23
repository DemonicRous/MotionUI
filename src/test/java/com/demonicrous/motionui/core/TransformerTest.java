package com.demonicrous.motionui.core;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import net.minecraft.client.gui.ScaledResolution;
import org.junit.Before;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public final class TransformerTest {
    private static final String SCALED = "net.minecraft.client.gui.ScaledResolution";
    private static final String INGAME = "net.minecraft.client.gui.GuiIngame";
    private static final String HELPER =
            "com/demonicrous/motionui/client/HotbarAnimationController";

    @Before
    public void resetState() {
        PatchDiagnostics.resetForTests();
        MotionUIEarlyConfig.setUnicodeEnabledForTests(true);
    }

    @Test
    public void unicodePatchRemovesUnicodeCheck() throws Exception {
        byte[] output = new UnicodeGuiScaleTransformer().transform(SCALED, SCALED,
                bytes("/net/minecraft/client/gui/ScaledResolution.class"));
        assertEquals(0, countCalls(output, null, "isUnicode", "func_152349_b"));
        assertEquals(1, countPopFalsePairs(output));
    }

    @Test
    public void unicodePatchSupportsObfuscatedNames() {
        byte[] output = new UnicodeGuiScaleTransformer().transform(
                "bit", "bit", unicodeClass("bit", "bib", "e", 1));
        assertEquals(0, countCalls(output, "bib", "e"));
        assertEquals(1, countPopFalsePairs(output));
    }

    @Test
    public void unicodePatchRejectsUnknownAndAmbiguousBytecode() {
        UnicodeGuiScaleTransformer transformer = new UnicodeGuiScaleTransformer();
        byte[] missing = unicodeClass(SCALED,
                "net/minecraft/client/Minecraft", "isUnicode", 0);
        byte[] duplicate = unicodeClass(SCALED,
                "net/minecraft/client/Minecraft", "isUnicode", 2);
        assertSame(missing, transformer.transform(SCALED, SCALED, missing));
        assertSame(duplicate, transformer.transform(SCALED, SCALED, duplicate));
    }

    @Test
    public void unicodePatchIsIdempotent() {
        UnicodeGuiScaleTransformer transformer = new UnicodeGuiScaleTransformer();
        byte[] first = transformer.transform(
                "bit", "bit", unicodeClass("bit", "bib", "e", 1));
        byte[] second = transformer.transform("bit", "bit", first);
        assertSame(first, second);
        assertArrayEquals(first, second);
        assertEquals(1, countPopFalsePairs(second));
    }

    @Test
    public void hotbarPatchOnlyAddsDeferHelper() throws Exception {
        byte[] output = new HotbarSelectorTransformer().transform(INGAME, INGAME,
                bytes("/net/minecraft/client/gui/GuiIngame.class"));
        assertEquals(1, countCalls(output, HELPER, "deferSelector"));
        assertEquals(0, countCalls(output, HELPER,
                "adjustSelectorX", "beginSubpixelRender", "endSubpixelRender"));
    }

    @Test
    public void hotbarPatchSupportsObfuscatedNames() {
        byte[] output = new HotbarSelectorTransformer().transform(
                "biq", "biq", hotbarClass("biq", "bif", "b", 1));
        assertEquals(1, countCalls(output, HELPER, "deferSelector"));
        assertEquals(0, countCalls(output, "bif", "b"));
    }

    @Test
    public void hotbarPatchRejectsUnknownAndAmbiguousBytecode() {
        HotbarSelectorTransformer transformer = new HotbarSelectorTransformer();
        byte[] missing = hotbarClass(INGAME,
                "net/minecraft/client/gui/Gui", "drawTexturedModalRect", 0);
        byte[] duplicate = hotbarClass(INGAME,
                "net/minecraft/client/gui/Gui", "drawTexturedModalRect", 2);
        assertSame(missing, transformer.transform(INGAME, INGAME, missing));
        assertSame(duplicate, transformer.transform(INGAME, INGAME, duplicate));
    }

    @Test
    public void hotbarPatchIsIdempotent() {
        HotbarSelectorTransformer transformer = new HotbarSelectorTransformer();
        byte[] first = transformer.transform(
                "biq", "biq", hotbarClass("biq", "bif", "b", 1));
        byte[] second = transformer.transform("biq", "biq", first);
        assertSame(first, second);
        assertArrayEquals(first, second);
        assertEquals(1, countCalls(second, HELPER, "deferSelector"));
    }

    @Test
    public void transformersIgnoreUnrelatedClassesAndNullBytecode() {
        byte[] unrelated = unicodeClass("example.Unrelated",
                "net/minecraft/client/Minecraft", "isUnicode", 1);
        assertSame(unrelated, new UnicodeGuiScaleTransformer().transform(
                "example.Unrelated", "example.Unrelated", unrelated));
        assertSame(unrelated, new HotbarSelectorTransformer().transform(
                "example.Unrelated", "example.Unrelated", unrelated));
        assertNull(new UnicodeGuiScaleTransformer().transform(SCALED, SCALED, null));
        assertNull(new HotbarSelectorTransformer().transform(INGAME, INGAME, null));
    }

    private static byte[] unicodeClass(
            String className, String owner, String methodName, int calls) {
        ClassNode node = baseClass(className);
        MethodNode constructor = constructor();
        AbstractInsnNode returnInstruction = constructor.instructions.getLast();
        for (int index = 0; index < calls; index++) {
            constructor.instructions.insertBefore(returnInstruction, new InsnNode(Opcodes.ACONST_NULL));
            constructor.instructions.insertBefore(returnInstruction,
                    new MethodInsnNode(Opcodes.INVOKEVIRTUAL, owner, methodName, "()Z", false));
            constructor.instructions.insertBefore(returnInstruction, new InsnNode(Opcodes.POP));
        }
        node.methods.add(constructor);
        return write(node);
    }

    private static byte[] hotbarClass(
            String className, String owner, String methodName, int calls) {
        ClassNode node = baseClass(className);
        node.methods.add(constructor());
        MethodNode render = new MethodNode(Opcodes.ACC_PUBLIC, "render", "()V", null, null);
        for (int index = 0; index < calls; index++) {
            render.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
            render.instructions.add(new InsnNode(Opcodes.ICONST_0));
            render.instructions.add(new InsnNode(Opcodes.ICONST_0));
            render.instructions.add(new InsnNode(Opcodes.ICONST_0));
            render.instructions.add(new IntInsnNode(Opcodes.BIPUSH, 22));
            render.instructions.add(new IntInsnNode(Opcodes.BIPUSH, 24));
            render.instructions.add(new IntInsnNode(Opcodes.BIPUSH, 22));
            render.instructions.add(new MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL, owner, methodName, "(IIIIII)V", false));
        }
        render.instructions.add(new InsnNode(Opcodes.RETURN));
        node.methods.add(render);
        return write(node);
    }

    private static ClassNode baseClass(String className) {
        ClassNode node = new ClassNode();
        node.version = Opcodes.V1_8;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = className.replace('.', '/');
        node.superName = "java/lang/Object";
        return node;
    }

    private static MethodNode constructor() {
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new MethodInsnNode(
                Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        return method;
    }

    private static byte[] write(ClassNode node) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    private static int countCalls(byte[] bytes, String owner, String... names) {
        int count = 0;
        for (MethodNode method : read(bytes).methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (!(instruction instanceof MethodInsnNode)) {
                    continue;
                }
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (owner != null && !owner.equals(call.owner)) {
                    continue;
                }
                for (String name : names) {
                    if (name.equals(call.name)) {
                        count++;
                        break;
                    }
                }
            }
        }
        return count;
    }

    private static int countPopFalsePairs(byte[] bytes) {
        int count = 0;
        for (MethodNode method : read(bytes).methods) {
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                AbstractInsnNode next = nextReal(instruction);
                if (instruction.getOpcode() == Opcodes.POP
                        && next != null && next.getOpcode() == Opcodes.ICONST_0) {
                    count++;
                }
            }
        }
        return count;
    }

    private static AbstractInsnNode nextReal(AbstractInsnNode instruction) {
        AbstractInsnNode next = instruction.getNext();
        while (next != null && next.getOpcode() < 0) {
            next = next.getNext();
        }
        return next;
    }

    private static ClassNode read(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    private static byte[] bytes(String path) throws IOException {
        InputStream input = ScaledResolution.class.getResourceAsStream(path);
        assertNotNull(input);
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            for (int read; (read = input.read(buffer)) >= 0; ) {
                output.write(buffer, 0, read);
            }
            return output.toByteArray();
        } finally {
            input.close();
        }
    }
}

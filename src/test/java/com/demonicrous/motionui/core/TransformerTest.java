package com.demonicrous.motionui.core;

import static org.junit.Assert.*;
import java.io.*;
import net.minecraft.client.gui.ScaledResolution;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

public final class TransformerTest {
    @Test public void unicodePatchRemovesUnicodeCheck() throws Exception {
        String n="net.minecraft.client.gui.ScaledResolution";MotionUIEarlyConfig.setUnicodeEnabledForTests(true);
        byte[] out=new UnicodeGuiScaleTransformer().transform(n,n,bytes("/net/minecraft/client/gui/ScaledResolution.class"));int calls=0,pairs=0;ClassNode node=new ClassNode();new ClassReader(out).accept(node,0);
        for(MethodNode m:node.methods)for(AbstractInsnNode i:m.instructions.toArray()){if(i instanceof MethodInsnNode&&"()Z".equals(((MethodInsnNode)i).desc)&&("isUnicode".equals(((MethodInsnNode)i).name)||"func_152349_b".equals(((MethodInsnNode)i).name)))calls++;if(i.getOpcode()==org.objectweb.asm.Opcodes.POP&&i.getNext()!=null&&i.getNext().getOpcode()==org.objectweb.asm.Opcodes.ICONST_0)pairs++;}
        assertEquals(0,calls);assertEquals(1,pairs);
    }
    @Test public void hotbarPatchOnlyAddsXHelper() throws Exception {
        String n="net.minecraft.client.gui.GuiIngame";byte[] out=new HotbarSelectorTransformer().transform(n,n,bytes("/net/minecraft/client/gui/GuiIngame.class"));int adjust=0,defer=0;ClassNode node=new ClassNode();new ClassReader(out).accept(node,0);
        for(MethodNode m:node.methods)for(AbstractInsnNode i:m.instructions.toArray())if(i instanceof MethodInsnNode){MethodInsnNode c=(MethodInsnNode)i;if("com/demonicrous/motionui/client/HotbarAnimationController".equals(c.owner)){if("adjustSelectorX".equals(c.name))adjust++;if("deferSelector".equals(c.name))defer++;}}
        assertEquals(1,adjust);assertEquals(0,defer);
    }
    private static byte[] bytes(String path)throws IOException{InputStream in=ScaledResolution.class.getResourceAsStream(path);assertNotNull(in);try{ByteArrayOutputStream o=new ByteArrayOutputStream();byte[] b=new byte[8192];for(int r;(r=in.read(b))>=0;)o.write(b,0,r);return o.toByteArray();}finally{in.close();}}
}

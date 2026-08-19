package com.demonicrous.motionui.client;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.*;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import org.objectweb.asm.ClassReader;

/** Finds GUI classes from bytecode without loading or initializing mod classes. */
public final class GuiCatalog {
    public static final class Entry { public final String className,modId; public volatile boolean observed; Entry(String c,String m){className=c;modId=m;} }
    private static final Map<String,Entry> ENTRIES=new ConcurrentHashMap<String,Entry>();
    private GuiCatalog() {}
    public static Collection<Entry> entries(){return new ArrayList<Entry>(ENTRIES.values());}
    public static void observe(Class<?> type){String mod=owner(type.getName());Entry e=ENTRIES.get(type.getName());if(e==null){e=new Entry(type.getName(),mod);ENTRIES.put(type.getName(),e);}e.observed=true;}
    public static void scanAsync(){Thread t=new Thread(new Runnable(){public void run(){scan();}},"MotionUI-GUI-Scanner");t.setDaemon(true);t.start();}
    private static void scan(){
        Map<String,String> supers=new HashMap<String,String>();Map<String,String> owners=new HashMap<String,String>();
        for(ModContainer mod:Loader.instance().getModList()){File source=mod.getSource();if(source==null)continue;try{if(source.isFile())scanJar(source,mod.getModId(),supers,owners);else scanDir(source,source,mod.getModId(),supers,owners);}catch(IOException ignored){}}
        for(String name:supers.keySet())if(isGui(name,supers,new HashSet<String>()))ENTRIES.putIfAbsent(name,new Entry(name,owners.get(name)));
    }
    private static boolean isGui(String n,Map<String,String>s,Set<String>seen){if(!seen.add(n))return false;String p=s.get(n);return "net.minecraft.client.gui.GuiScreen".equals(p)||p!=null&&isGui(p,s,seen);}
    private static void scanJar(File f,String id,Map<String,String>s,Map<String,String>o)throws IOException{JarFile j=new JarFile(f);try{Enumeration<JarEntry> es=j.entries();while(es.hasMoreElements()){JarEntry e=es.nextElement();if(!e.isDirectory()&&e.getName().endsWith(".class"))read(j.getInputStream(e),id,s,o);}}finally{j.close();}}
    private static void scanDir(File root,File f,String id,Map<String,String>s,Map<String,String>o)throws IOException{File[] fs=f.listFiles();if(fs==null)return;for(File x:fs){if(x.isDirectory())scanDir(root,x,id,s,o);else if(x.getName().endsWith(".class"))read(new FileInputStream(x),id,s,o);}}
    private static void read(InputStream in,String id,Map<String,String>s,Map<String,String>o)throws IOException{try{ClassReader r=new ClassReader(in);String n=r.getClassName().replace('/','.');String p=r.getSuperName();s.put(n,p==null?null:p.replace('/','.'));o.put(n,id);}finally{in.close();}}
    private static String owner(String name){for(Entry e:ENTRIES.values())if(e.className.equals(name))return e.modId;return name.startsWith("net.minecraft.")?"minecraft":"unknown";}
}

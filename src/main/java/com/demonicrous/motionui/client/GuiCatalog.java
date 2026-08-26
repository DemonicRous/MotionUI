package com.demonicrous.motionui.client;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.ClassReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Finds GUI classes from bytecode without loading or initializing mod classes. */
public final class GuiCatalog {
    private static final Logger LOGGER=LogManager.getLogger("MotionUI/GuiCatalog");
    public static final class Entry { public final String className,modId; public volatile boolean observed; Entry(String c,String m){className=c;modId=m;} }
    private static final Map<String,Entry> ENTRIES=new ConcurrentHashMap<String,Entry>();
    private static final AtomicInteger REVISION=new AtomicInteger();
    private static volatile boolean scanning;
    private GuiCatalog() {}
    public static Collection<Entry> entries(){ArrayList<Entry> result=new ArrayList<Entry>();for(Entry entry:ENTRIES.values())if(isUserFacing(entry.className))result.add(entry);return result;}
    /** Returns the catalog owner used by mod-level animation rule inheritance. */
    public static String modId(String className){return owner(className);}
    public static int revision(){return REVISION.get();}
    public static boolean isScanning(){return scanning;}
    public static void observe(Class<?> type){String name=type.getName();if(!isUserFacing(name))return;String mod=owner(type);Entry e=ENTRIES.get(name);if(e==null){Entry added=new Entry(name,mod);e=ENTRIES.putIfAbsent(name,added);if(e==null){e=added;REVISION.incrementAndGet();}}else if("unknown".equals(e.modId)&&!"unknown".equals(mod)){Entry replacement=new Entry(name,mod);replacement.observed=true;if(ENTRIES.replace(name,e,replacement)){e=replacement;REVISION.incrementAndGet();}}e.observed=true;}
    public static void scanAsync(){if(scanning)return;scanning=true;Thread t=new Thread(new Runnable(){public void run(){try{scan();}finally{scanning=false;REVISION.incrementAndGet();}}},"MotionUI-GUI-Scanner");t.setDaemon(true);t.start();}
    private static void scan(){
        Map<String,String> supers=new HashMap<String,String>();Map<String,String> owners=new HashMap<String,String>();
        Map<String,String> sources=new LinkedHashMap<String,String>();
        for(ModContainer mod:Loader.instance().getActiveModList())addSource(sources,mod.getSource(),mod.getModId());
        addCodeSource(sources,GuiScreen.class,"minecraft");addCodeSource(sources,Minecraft.class,"minecraft");addCodeSource(sources,Loader.class,"forge");
        addCodeSource(sources,GuiCatalog.class,"motionui");
        int failures=0;
        for(Map.Entry<String,String> source:sources.entrySet()){File file=new File(source.getKey());try{if(file.isFile())scanJar(file,source.getValue(),supers,owners);else if(file.isDirectory())scanDir(file,file,source.getValue(),supers,owners);}catch(Exception problem){failures++;LOGGER.warn("Could not scan GUI source {}",file,problem);}}
        int before=ENTRIES.size();
        for(String name:supers.keySet())if(isUserFacing(name)&&isGuiHierarchy(name,supers,new HashSet<String>()))put(name,new Entry(name,owners.get(name)));
        LOGGER.info("GUI scan complete: {} GUI classes ({} new), {} class sources, {} failed sources",ENTRIES.size(),ENTRIES.size()-before,sources.size(),failures);
    }
    private static void addCodeSource(Map<String,String> sources,Class<?> type,String id){try{java.security.CodeSource code=type.getProtectionDomain().getCodeSource();if(code!=null&&code.getLocation()!=null&&"file".equalsIgnoreCase(code.getLocation().getProtocol()))addSource(sources,new File(code.getLocation().toURI()),id);}catch(Exception ignored){}}
    private static void addSource(Map<String,String> sources,File file,String id){if(file==null)return;try{if("unknown".equals(id))id=sourceOwner(file);String path=file.getCanonicalPath();String old=sources.get(path);if(old==null||"unknown".equals(old))sources.put(path,id);}catch(IOException ignored){}}
    private static String sourceOwner(File file){String name=file.getName().toLowerCase(Locale.ROOT);return name.startsWith("optifine_")||name.startsWith("optifine-")?"optifine":"unknown";}
    private static void put(String name,Entry entry){Entry old=ENTRIES.putIfAbsent(name,entry);if(old==null){REVISION.incrementAndGet();return;}if("unknown".equals(old.modId)&&!"unknown".equals(entry.modId)){entry.observed=old.observed;if(ENTRIES.replace(name,old,entry))REVISION.incrementAndGet();}}
    static boolean isGuiHierarchy(String n,Map<String,String>s,Set<String>seen){if(!seen.add(n))return false;String p=s.get(n);return isGuiRoot(p)||p!=null&&isGuiHierarchy(p,s,seen);}
    private static boolean isGuiRoot(String name){return "net.minecraft.client.gui.GuiScreen".equals(name)||"net.minecraft.client.gui.inventory.GuiContainer".equals(name)||"net.minecraftforge.fml.client.config.GuiConfig".equals(name);}
    private static boolean isUserFacing(String name){return !"com.demonicrous.motionui.client.ClosingAnimationEvents$LayerScreen".equals(name);}
    private static void scanJar(File f,String id,Map<String,String>s,Map<String,String>o)throws IOException{JarFile j=new JarFile(f);try{Enumeration<JarEntry> es=j.entries();while(es.hasMoreElements()){JarEntry e=es.nextElement();if(!e.isDirectory()&&e.getName().endsWith(".class"))read(j.getInputStream(e),id,s,o);}}finally{j.close();}}
    private static void scanDir(File root,File f,String id,Map<String,String>s,Map<String,String>o)throws IOException{File[] fs=f.listFiles();if(fs==null)return;for(File x:fs){if(x.isDirectory())scanDir(root,x,id,s,o);else if(x.getName().endsWith(".class"))read(new FileInputStream(x),id,s,o);}}
    private static void read(InputStream in,String id,Map<String,String>s,Map<String,String>o)throws IOException{try{ClassReader r=new ClassReader(in);String n=map(r.getClassName()),p=r.getSuperName();p=p==null?null:map(p);s.put(n,p);o.put(n,ownerFor(n,id));}finally{in.close();}}
    private static String map(String internal){return FMLDeobfuscatingRemapper.INSTANCE.map(internal).replace('/','.');}
    private static String ownerFor(String name,String fallback){if(name.startsWith("net.minecraft."))return "minecraft";if(name.startsWith("net.minecraftforge."))return "forge";if(name.startsWith("net.optifine.")||name.startsWith("optifine."))return "optifine";return fallback;}
    private static String owner(Class<?> type){String byName=owner(type.getName());if(!"unknown".equals(byName))return byName;File source=classSource(type);if(source==null)return byName;for(ModContainer mod:Loader.instance().getActiveModList())if(sameFile(source,mod.getSource()))return mod.getModId();return sourceOwner(source);}
    private static File classSource(Class<?> type){try{java.security.CodeSource code=type.getProtectionDomain().getCodeSource();if(code!=null&&code.getLocation()!=null&&"file".equalsIgnoreCase(code.getLocation().getProtocol()))return new File(code.getLocation().toURI());}catch(Exception ignored){}try{java.net.URL resource=type.getResource('/'+type.getName().replace('.','/')+".class");if(resource!=null&&"jar".equalsIgnoreCase(resource.getProtocol()))return new File(((java.net.JarURLConnection)resource.openConnection()).getJarFileURL().toURI());}catch(Exception ignored){}return null;}
    private static boolean sameFile(File first,File second){if(first==null||second==null)return false;try{return first.getCanonicalFile().equals(second.getCanonicalFile());}catch(IOException ignored){return false;}}
    private static String owner(String name){Entry known=ENTRIES.get(name);if(known!=null)return known.modId;if(name.startsWith("net.minecraft."))return "minecraft";if(name.startsWith("net.minecraftforge."))return "forge";if(name.startsWith("net.optifine.")||name.startsWith("optifine.")||name.endsWith("OF"))return "optifine";if(name.startsWith("com.demonicrous.motionui."))return "motionui";return "unknown";}
}

package com.demonicrous.motionui.config;

import java.io.File;
import java.util.*;
import net.minecraftforge.common.config.Configuration;

public final class MotionUIConfig {
    public enum Policy { DEFAULT, ENABLED, DISABLED }
    private static Configuration config;
    public static boolean hotbar=true, containers=true;
    public static int hotbarDuration=100, containerDuration=160, containerOffset=10;
    private static final Map<String,Policy> policies=new HashMap<String,Policy>();
    private MotionUIConfig() {}
    public static synchronized void load(File file){
        config=new Configuration(file); config.load();
        hotbar=config.getBoolean("enabled","hotbar",true,"Animate the vanilla hotbar selector.");
        hotbarDuration=config.getInt("durationMs","hotbar",100,40,400,"Animation duration.");
        containers=config.getBoolean("enabled","screens.containers",true,"Animate GuiContainer opening.");
        containerDuration=config.getInt("durationMs","screens.containers",160,0,400,"Animation duration.");
        containerOffset=config.getInt("offsetPx","screens.containers",10,0,32,"Initial vertical offset.");
        policies.clear();
        for(String entry:config.getStringList("classPolicies","compatibility",new String[0],"class=DEFAULT|ENABLED|DISABLED")){
            int i=entry.lastIndexOf('='); if(i>0) try{policies.put(entry.substring(0,i),Policy.valueOf(entry.substring(i+1)));}catch(IllegalArgumentException ignored){}
        }
        if(config.hasChanged())config.save();
    }
    public static synchronized Policy policy(String name){Policy p=policies.get(name);return p==null?Policy.DEFAULT:p;}
    public static synchronized boolean animationAllowed(Class<?> type){return animationAllowed(type.getName());}
    public static synchronized boolean animationAllowed(String name){Policy p=policy(name);return p!=Policy.DISABLED;}
    public static synchronized Policy cycle(String name){Policy next=policy(name)==Policy.DEFAULT?Policy.ENABLED:policy(name)==Policy.ENABLED?Policy.DISABLED:Policy.DEFAULT; if(next==Policy.DEFAULT)policies.remove(name);else policies.put(name,next);savePolicies();return next;}
    private static void savePolicies(){List<String> out=new ArrayList<String>();for(Map.Entry<String,Policy> e:policies.entrySet())out.add(e.getKey()+"="+e.getValue());Collections.sort(out);config.get("compatibility","classPolicies",new String[0]).set(out.toArray(new String[out.size()]));config.save();}
}

package com.demonicrous.motionui.config;

import java.io.File;
import java.util.*;
import net.minecraftforge.common.config.Configuration;

public final class MotionUIConfig {
    public enum Policy { DEFAULT, ENABLED, DISABLED }
    public enum ScreenEasing { EASE_OUT_CUBIC, EASE_OUT_QUINT, EASE_IN_OUT_CUBIC }
    public static final class ScreenRule {
        public final int duration,offset; public final ScreenEasing easing;
        ScreenRule(int duration,int offset,ScreenEasing easing){this.duration=duration;this.offset=offset;this.easing=easing;}
    }
    private static Configuration config;
    public static boolean hotbar=true, containers=true;
    public static int hotbarDuration=100, containerDuration=160, containerOffset=10;
    private static final Map<String,Policy> policies=new HashMap<String,Policy>();
    private static final Map<String,ScreenRule> rules=new HashMap<String,ScreenRule>();
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
        rules.clear();
        for(String entry:config.getStringList("classAnimations","compatibility",new String[0],"class|durationMs|offsetPx|easing")){
            String[] p=entry.split("\\|",4);if(p.length==4)try{rules.put(p[0],new ScreenRule(clamp(Integer.parseInt(p[1]),0,600),clamp(Integer.parseInt(p[2]),0,48),ScreenEasing.valueOf(p[3])));}catch(RuntimeException ignored){}
        }
        if(config.hasChanged())config.save();
    }
    public static synchronized Policy policy(String name){Policy p=policies.get(name);return p==null?Policy.DEFAULT:p;}
    public static synchronized boolean animationAllowed(Class<?> type){return animationAllowed(type.getName());}
    public static synchronized boolean animationAllowed(String name){Policy p=policy(name);return p!=Policy.DISABLED;}
    public static synchronized Policy cycle(String name){Policy next=policy(name)==Policy.DEFAULT?Policy.ENABLED:policy(name)==Policy.ENABLED?Policy.DISABLED:Policy.DEFAULT; if(next==Policy.DEFAULT)policies.remove(name);else policies.put(name,next);savePolicies();return next;}
    public static synchronized ScreenRule rule(String name){ScreenRule r=rules.get(name);return r==null?new ScreenRule(containerDuration,containerOffset,ScreenEasing.EASE_OUT_CUBIC):r;}
    public static synchronized boolean hasRule(String name){return rules.containsKey(name);}
    public static synchronized void setRule(String name,int duration,int offset,ScreenEasing easing){rules.put(name,new ScreenRule(clamp(duration,0,600),clamp(offset,0,48),easing));saveRules();}
    public static synchronized void resetRule(String name){rules.remove(name);policies.remove(name);saveRules();savePolicies();}
    private static void savePolicies(){List<String> out=new ArrayList<String>();for(Map.Entry<String,Policy> e:policies.entrySet())out.add(e.getKey()+"="+e.getValue());Collections.sort(out);config.get("compatibility","classPolicies",new String[0]).set(out.toArray(new String[out.size()]));config.save();}
    private static void saveRules(){List<String> out=new ArrayList<String>();for(Map.Entry<String,ScreenRule> e:rules.entrySet()){ScreenRule r=e.getValue();out.add(e.getKey()+"|"+r.duration+"|"+r.offset+"|"+r.easing.name());}Collections.sort(out);config.get("compatibility","classAnimations",new String[0]).set(out.toArray(new String[out.size()]));config.save();}
    private static int clamp(int value,int min,int max){return Math.max(min,Math.min(max,value));}
}

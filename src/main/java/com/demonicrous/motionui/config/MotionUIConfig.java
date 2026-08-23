package com.demonicrous.motionui.config;

import java.io.File;
import java.util.*;
import net.minecraftforge.common.config.Configuration;

public final class MotionUIConfig {
    public enum Policy { DEFAULT, ENABLED, DISABLED }
    public enum ScreenEasing { EASE_OUT_CUBIC, EASE_OUT_QUINT, EASE_IN_OUT_CUBIC }
    public enum ClosingMode { DISABLED, SIMPLIFIED, FULL }
    public enum AnimationProfile {
        OFF(false, false, false, 100, 160, 10, 120, 6),
        SUBTLE(true, true, true, 80, 110, 4, 90, 3),
        SMOOTH(true, true, true, 100, 160, 10, 120, 6),
        EXPRESSIVE(true, true, true, 150, 240, 18, 180, 12),
        CUSTOM(true, true, true, 100, 160, 10, 120, 6);

        final boolean hotbar, containers, closing;
        final int hotbarDuration, containerDuration, containerOffset, closingDuration, closingOffset;
        AnimationProfile(boolean hotbar,boolean containers,boolean closing,int hotbarDuration,
                int containerDuration,int containerOffset,int closingDuration,int closingOffset){
            this.hotbar=hotbar;this.containers=containers;this.closing=closing;
            this.hotbarDuration=hotbarDuration;this.containerDuration=containerDuration;
            this.containerOffset=containerOffset;this.closingDuration=closingDuration;
            this.closingOffset=closingOffset;
        }
    }
    public static final class ScreenRule {
        public final int duration,offset; public final ScreenEasing easing;
        ScreenRule(int duration,int offset,ScreenEasing easing){this.duration=duration;this.offset=offset;this.easing=easing;}
    }
    private static Configuration config;
    public static boolean hotbar=true, containers=true, closing=true;
    public static int hotbarDuration=100, containerDuration=160, containerOffset=10;
    public static int closingDuration=120, closingOffset=6;
    private static AnimationProfile profile=AnimationProfile.SMOOTH;
    private static ClosingMode closingMode=ClosingMode.SIMPLIFIED;
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
        closing=config.getBoolean("enabled","screens.closing",true,"Animate GuiContainer closing without delaying the actual close.");
        closingDuration=config.getInt("durationMs","screens.closing",120,0,300,"Snapshot fade duration.");
        closingOffset=config.getInt("offsetPx","screens.closing",6,0,24,"Final vertical offset.");
        if(config.hasKey("screens.closing","mode")){
            closingMode=parseClosingMode(config.getString("mode","screens.closing","SIMPLIFIED","DISABLED, SIMPLIFIED or FULL."));
        }else{
            String legacyControl=config.getString("control","screens.closing","IMMEDIATE","Legacy setting; replaced by mode.");
            closingMode=!closing?ClosingMode.DISABLED:"AFTER_ANIMATION".equals(legacyControl)?ClosingMode.FULL:ClosingMode.SIMPLIFIED;
        }
        closing=closingMode!=ClosingMode.DISABLED;
        config.get("screens.closing","mode","SIMPLIFIED").set(closingMode.name());
        config.get("screens.closing","enabled",true).set(closing);
        boolean hadProfile=config.hasKey("animations","profile");
        AnimationProfile requested=parseProfile(config.getString("profile","animations","SMOOTH","Global animation profile."));
        AnimationProfile matching=matchingProfile();
        profile=!hadProfile?matching:requested==AnimationProfile.CUSTOM||requested!=matching?AnimationProfile.CUSTOM:requested;
        config.get("animations","profile","SMOOTH").set(profile.name());
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
    public static synchronized AnimationProfile profile(){return profile;}
    public static synchronized ClosingMode closingMode(){return closingMode;}
    public static synchronized ClosingMode cycleClosingMode(){
        closingMode=closingMode==ClosingMode.DISABLED?ClosingMode.SIMPLIFIED:
                closingMode==ClosingMode.SIMPLIFIED?ClosingMode.FULL:ClosingMode.DISABLED;
        closing=closingMode!=ClosingMode.DISABLED;
        profile=matchingProfile();
        config.get("animations","profile","SMOOTH").set(profile.name());
        config.get("screens.closing","mode","SIMPLIFIED").set(closingMode.name());
        config.get("screens.closing","enabled",true).set(closing);
        config.save();return closingMode;
    }
    public static synchronized AnimationProfile cycleProfile(){
        AnimationProfile next=profile==AnimationProfile.OFF?AnimationProfile.SUBTLE:
                profile==AnimationProfile.SUBTLE?AnimationProfile.SMOOTH:
                profile==AnimationProfile.SMOOTH?AnimationProfile.EXPRESSIVE:AnimationProfile.OFF;
        setProfile(next);return next;
    }
    public static synchronized void setProfile(AnimationProfile next){
        if(next==null||next==AnimationProfile.CUSTOM)return;
        profile=next;apply(next);saveProfile();
    }
    public static synchronized boolean animationAllowed(Class<?> type){return animationAllowed(type.getName());}
    public static synchronized boolean animationAllowed(String name){Policy p=policy(name);return p!=Policy.DISABLED;}
    public static synchronized Policy cycle(String name){Policy next=policy(name)==Policy.DEFAULT?Policy.ENABLED:policy(name)==Policy.ENABLED?Policy.DISABLED:Policy.DEFAULT; if(next==Policy.DEFAULT)policies.remove(name);else policies.put(name,next);savePolicies();return next;}
    public static synchronized ScreenRule rule(String name){ScreenRule r=rules.get(name);return r==null?new ScreenRule(containerDuration,containerOffset,ScreenEasing.EASE_OUT_CUBIC):r;}
    public static synchronized boolean hasRule(String name){return rules.containsKey(name);}
    public static synchronized void setRule(String name,int duration,int offset,ScreenEasing easing){rules.put(name,new ScreenRule(clamp(duration,0,600),clamp(offset,0,48),easing));saveRules();}
    public static synchronized void resetRule(String name){rules.remove(name);policies.remove(name);saveRules();savePolicies();}
    private static void savePolicies(){List<String> out=new ArrayList<String>();for(Map.Entry<String,Policy> e:policies.entrySet())out.add(e.getKey()+"="+e.getValue());Collections.sort(out);config.get("compatibility","classPolicies",new String[0]).set(out.toArray(new String[out.size()]));config.save();}
    private static void saveRules(){List<String> out=new ArrayList<String>();for(Map.Entry<String,ScreenRule> e:rules.entrySet()){ScreenRule r=e.getValue();out.add(e.getKey()+"|"+r.duration+"|"+r.offset+"|"+r.easing.name());}Collections.sort(out);config.get("compatibility","classAnimations",new String[0]).set(out.toArray(new String[out.size()]));config.save();}
    private static AnimationProfile parseProfile(String value){try{return AnimationProfile.valueOf(value);}catch(IllegalArgumentException ignored){return AnimationProfile.CUSTOM;}}
    private static ClosingMode parseClosingMode(String value){try{return ClosingMode.valueOf(value);}catch(IllegalArgumentException ignored){return ClosingMode.SIMPLIFIED;}}
    private static AnimationProfile matchingProfile(){for(AnimationProfile p:AnimationProfile.values())if(p!=AnimationProfile.CUSTOM&&matches(p))return p;return AnimationProfile.CUSTOM;}
    private static boolean matches(AnimationProfile p){return hotbar==p.hotbar&&containers==p.containers&&closing==p.closing&&hotbarDuration==p.hotbarDuration&&containerDuration==p.containerDuration&&containerOffset==p.containerOffset&&closingDuration==p.closingDuration&&closingOffset==p.closingOffset;}
    private static void apply(AnimationProfile p){hotbar=p.hotbar;containers=p.containers;closing=p.closing;if(!closing)closingMode=ClosingMode.DISABLED;else if(closingMode==ClosingMode.DISABLED)closingMode=ClosingMode.SIMPLIFIED;hotbarDuration=p.hotbarDuration;containerDuration=p.containerDuration;containerOffset=p.containerOffset;closingDuration=p.closingDuration;closingOffset=p.closingOffset;}
    private static void saveProfile(){
        config.get("animations","profile","SMOOTH").set(profile.name());
        config.get("hotbar","enabled",true).set(hotbar);config.get("hotbar","durationMs",100).set(hotbarDuration);
        config.get("screens.containers","enabled",true).set(containers);config.get("screens.containers","durationMs",160).set(containerDuration);config.get("screens.containers","offsetPx",10).set(containerOffset);
        config.get("screens.closing","enabled",true).set(closing);config.get("screens.closing","durationMs",120).set(closingDuration);config.get("screens.closing","offsetPx",6).set(closingOffset);
        config.get("screens.closing","mode","SIMPLIFIED").set(closingMode.name());
        config.save();
    }
    private static int clamp(int value,int min,int max){return Math.max(min,Math.min(max,value));}
}

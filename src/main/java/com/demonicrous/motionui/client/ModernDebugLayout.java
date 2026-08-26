package com.demonicrous.motionui.client;

/** Deterministic two-column layout shared by the modern F3 renderer and tests. */
final class ModernDebugLayout {
    private ModernDebugLayout() {}
    static MotionUILayout.Rect[] columns(int screenWidth,int leftPreferred,int rightPreferred){
        int gap=MotionUILayout.S8,margin=MotionUILayout.S8;
        int available=Math.max(2,screenWidth-margin*2-gap);
        int left=Math.min(leftPreferred,available/2),right=Math.min(rightPreferred,available-left);
        if(left+right>available){left=available/2;right=available-left;}
        return new MotionUILayout.Rect[]{new MotionUILayout.Rect(margin,margin,left,0),new MotionUILayout.Rect(screenWidth-margin-right,margin,right,0)};
    }
}

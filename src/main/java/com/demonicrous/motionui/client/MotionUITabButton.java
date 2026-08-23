package com.demonicrous.motionui.client;

/** Vanilla button sprite with a persistent selected state, used as a tab. */
final class MotionUITabButton extends MotionUIButton {
    private final boolean selected;

    MotionUITabButton(int id, int x, int y, int width, String text, boolean selected) {
        super(id, x, y, width, 20, text);
        this.selected = selected;
        packedFGColour = selected ? 0xFFFFA0 : 0xE0E0E0;
    }

    @Override
    protected int getHoverState(boolean mouseOver) {
        return selected ? 2 : super.getHoverState(mouseOver);
    }
}

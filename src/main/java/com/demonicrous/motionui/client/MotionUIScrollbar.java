package com.demonicrous.motionui.client;

import net.minecraft.client.gui.Gui;

/** Fixed-row scrollbar with wheel, track paging and thumb dragging. */
final class MotionUIScrollbar extends Gui {
    private MotionUILayout.Rect track = new MotionUILayout.Rect(0, 0, 0, 0);
    private MotionUILayout.Rect wheelArea = track;
    private int itemCount, visibleCount, value;
    private boolean dragging;
    private int dragOffset;

    void layout(MotionUILayout.Rect bounds, MotionUILayout.Rect wheelBounds, int items, int visible) {
        track = bounds;
        wheelArea = wheelBounds;
        itemCount = Math.max(0, items);
        visibleCount = Math.max(1, visible);
        value = clamp(value);
        if (max() == 0) dragging = false;
    }

    int value() { return value; }

    void draw(int mouseX, int mouseY) {
        if (track.width <= 0 || track.height <= 0) return;
        drawRect(track.x, track.y, track.right(), track.bottom(), MotionUITheme.SURFACE_RAISED);
        MotionUILayout.Rect thumb = thumb();
        int color = max() == 0 ? MotionUITheme.BORDER_SUBTLE
                : thumb.contains(mouseX, mouseY) || dragging ? MotionUITheme.FOCUS : MotionUITheme.BORDER;
        drawRect(thumb.x, thumb.y, thumb.right(), thumb.bottom(), color);
    }

    boolean wheel(int mouseX, int mouseY, int delta) {
        if (!wheelArea.contains(mouseX, mouseY) || delta == 0 || max() == 0) return false;
        value = clamp(value + (delta > 0 ? -1 : 1));
        return true;
    }

    boolean mousePressed(int mouseX, int mouseY) {
        if (!track.contains(mouseX, mouseY) || max() == 0) return false;
        MotionUILayout.Rect thumb = thumb();
        if (thumb.contains(mouseX, mouseY)) {
            dragging = true;
            dragOffset = mouseY - thumb.y;
        } else {
            value = clamp(value + (mouseY < thumb.y ? -visibleCount : visibleCount));
        }
        return true;
    }

    void mouseDragged(int mouseY) {
        if (!dragging || max() == 0) return;
        MotionUILayout.Rect thumb = thumb();
        int travel = Math.max(1, track.height - thumb.height);
        int top = Math.max(track.y, Math.min(track.bottom() - thumb.height, mouseY - dragOffset));
        value = clamp(Math.round((top - track.y) * max() / (float) travel));
    }

    void mouseReleased() { dragging = false; }

    private MotionUILayout.Rect thumb() {
        if (itemCount <= 0 || itemCount <= visibleCount) return track;
        int height = Math.max(12, track.height * visibleCount / itemCount);
        int travel = Math.max(0, track.height - height);
        int top = track.y + Math.round(travel * value / (float) max());
        return new MotionUILayout.Rect(track.x, top, track.width, height);
    }

    private int max() { return Math.max(0, itemCount - visibleCount); }
    private int clamp(int candidate) { return Math.max(0, Math.min(max(), candidate)); }
}

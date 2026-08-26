package com.demonicrous.motionui.client;

/** Deterministic logical-pixel layout primitives shared by every MotionUI screen. */
final class MotionUILayout {
    static final int S2 = 2, S4 = 4, S8 = 8, S12 = 12, S16 = 16;
    static final int CONTROL = 22, TAB = 22, FOOTER = 24;

    private MotionUILayout() {}

    static final class Rect {
        final int x, y, width, height;
        Rect(int x, int y, int width, int height) {
            this.x = x; this.y = y; this.width = Math.max(0, width); this.height = Math.max(0, height);
        }
        int right() { return x + width; }
        int bottom() { return y + height; }
        int centerX() { return x + width / 2; }
        int centerY() { return y + height / 2; }
        Rect inset(int amount) { return inset(amount, amount, amount, amount); }
        Rect inset(int left, int top, int right, int bottom) {
            return new Rect(x + left, y + top, width - left - right, height - top - bottom);
        }
        boolean contains(int px, int py) { return px >= x && px < right() && py >= y && py < bottom(); }
    }

    static Rect frame(int screenWidth, int screenHeight, int preferredWidth, int preferredHeight) {
        int width = Math.max(1, Math.min(preferredWidth, Math.max(1, screenWidth - S16)));
        int height = Math.max(1, Math.min(preferredHeight, Math.max(1, screenHeight - S16)));
        return new Rect((screenWidth - width) / 2, (screenHeight - height) / 2, width, height);
    }

    static Rect[] tracks(Rect area, int count, int gap) {
        Rect[] result = new Rect[count];
        int available = Math.max(0, area.width - gap * (count - 1));
        int base = count == 0 ? 0 : available / count;
        int remainder = count == 0 ? 0 : available % count;
        int x = area.x;
        for (int i = 0; i < count; i++) {
            int width = base + (i < remainder ? 1 : 0);
            result[i] = new Rect(x, area.y, width, area.height);
            x += width + gap;
        }
        return result;
    }

    static Rect[] twoColumnOrStack(Rect area, int gap, int minColumnWidth) {
        if (area.width >= minColumnWidth * 2 + gap) return tracks(area, 2, gap);
        int firstHeight = Math.max(0, (area.height - gap) / 2);
        return new Rect[] {
                new Rect(area.x, area.y, area.width, firstHeight),
                new Rect(area.x, area.y + firstHeight + gap, area.width, area.height - firstHeight - gap)
        };
    }
}

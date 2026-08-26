package com.demonicrous.motionui.client;

import org.junit.Test;
import static org.junit.Assert.*;

public class MotionUILayoutTest {
    @Test public void frameNeverEscapesSmallScreen() {
        MotionUILayout.Rect frame = MotionUILayout.frame(240, 180, 800, 500);
        assertTrue(frame.x >= 0); assertTrue(frame.y >= 0);
        assertTrue(frame.right() <= 240); assertTrue(frame.bottom() <= 180);
    }

    @Test public void tracksConsumeExactWidthWithoutOverlap() {
        MotionUILayout.Rect area = new MotionUILayout.Rect(7, 9, 101, 22);
        MotionUILayout.Rect[] tracks = MotionUILayout.tracks(area, 3, 4);
        assertEquals(area.x, tracks[0].x);
        assertEquals(area.right(), tracks[2].right());
        assertEquals(4, tracks[1].x - tracks[0].right());
        assertEquals(4, tracks[2].x - tracks[1].right());
    }

    @Test public void stackedColumnsStayInsideParent() {
        MotionUILayout.Rect area = new MotionUILayout.Rect(10, 20, 220, 160);
        MotionUILayout.Rect[] columns = MotionUILayout.twoColumnOrStack(area, 8, 140);
        assertEquals(area.x, columns[0].x); assertEquals(area.x, columns[1].x);
        assertEquals(area.bottom(), columns[1].bottom());
        assertEquals(8, columns[1].y - columns[0].bottom());
    }
}

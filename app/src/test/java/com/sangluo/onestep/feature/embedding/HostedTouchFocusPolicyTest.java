package com.sangluo.onestep.feature.embedding;

import static org.junit.Assert.assertEquals;

import com.sangluo.onestep.feature.embedding.HostedTouchFocusPolicy;

import org.junit.Test;

/** Validates the physical system-gesture classification added for HyperOS 3. */
public class HostedTouchFocusPolicyTest {
    private static final float WIDTH = 1080f;
    private static final float HEIGHT = 2400f;
    private static final int EDGE = 66;      // 22dp @3x
    private static final int BOTTOM = 132;   // 44dp @3x
    private static final float SLOP = 72f;   // 24dp @3x

    private static int classify(float downX, float downY, float x, float y) {
        return HostedTouchFocusPolicy.resolvePhysicalSystemGesture(
                downX, downY, x, y, WIDTH, HEIGHT, EDGE, BOTTOM, SLOP);
    }

    @Test
    public void rightEdgeHorizontalSwipe_isBack() {
        assertEquals(HostedTouchFocusPolicy.SYSTEM_GESTURE_BACK,
                classify(1070f, 1200f, 900f, 1210f));
    }

    @Test
    public void leftEdgeHorizontalSwipe_isBack() {
        assertEquals(HostedTouchFocusPolicy.SYSTEM_GESTURE_BACK,
                classify(10f, 1200f, 200f, 1190f));
    }

    @Test
    public void bottomStripUpwardSwipe_isHome() {
        assertEquals(HostedTouchFocusPolicy.SYSTEM_GESTURE_HOME,
                classify(540f, 2380f, 540f, 2100f));
    }

    @Test
    public void shortMovement_staysNone() {
        assertEquals(HostedTouchFocusPolicy.SYSTEM_GESTURE_NONE,
                classify(1070f, 1200f, 1050f, 1210f));
    }

    @Test
    public void middleScreenSwipe_isNone() {
        assertEquals(HostedTouchFocusPolicy.SYSTEM_GESTURE_NONE,
                classify(540f, 1200f, 540f, 900f));
    }

    @Test
    public void bottomHorizontalDominantSwipe_isNone() {
        // Bottom-origin but horizontally dominant movement is content scrolling,
        // not the HOME gesture.
        assertEquals(HostedTouchFocusPolicy.SYSTEM_GESTURE_NONE,
                classify(540f, 2380f, 900f, 2370f));
    }

    @Test
    public void downwardDragFromBottom_isNone() {
        assertEquals(HostedTouchFocusPolicy.SYSTEM_GESTURE_NONE,
                classify(540f, 2300f, 540f, 2390f));
    }
}

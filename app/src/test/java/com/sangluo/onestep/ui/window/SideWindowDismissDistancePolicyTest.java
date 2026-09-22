package com.sangluo.onestep.ui.window;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Validates the side-window dismiss distance against narrow-window cases. */
public class SideWindowDismissDistancePolicyTest {
    private static final int FIXED = 144;   // 48dp @3x
    private static final float FRACTION = 0.6f;
    private static final int FLOOR = 60;    // 20dp @3x

    @Test
    public void wideWindow_keepsFixedDistance() {
        assertEquals(FIXED, SideWindowDismissDistancePolicy.resolvePx(
                FIXED, 800, FRACTION, FLOOR));
        assertEquals(FIXED, SideWindowDismissDistancePolicy.resolvePx(
                FIXED, 240, FRACTION, FLOOR));
    }

    @Test
    public void narrowWindow_capsDistanceToWindowFraction() {
        // Six side windows on a phone can leave ~160px per window.
        assertEquals(96, SideWindowDismissDistancePolicy.resolvePx(
                FIXED, 160, FRACTION, FLOOR));
        assertEquals(120, SideWindowDismissDistancePolicy.resolvePx(
                FIXED, 200, FRACTION, FLOOR));
    }

    @Test
    public void extremelyNarrowWindow_keepsFloorDistance() {
        assertEquals(FLOOR, SideWindowDismissDistancePolicy.resolvePx(
                FIXED, 60, FRACTION, FLOOR));
        assertEquals(FLOOR, SideWindowDismissDistancePolicy.resolvePx(
                FIXED, 10, FRACTION, FLOOR));
    }

    @Test
    public void unknownWindowEdge_fallsBackToFixedDistance() {
        assertEquals(FIXED, SideWindowDismissDistancePolicy.resolvePx(
                FIXED, 0, FRACTION, FLOOR));
        assertEquals(FIXED, SideWindowDismissDistancePolicy.resolvePx(
                FIXED, -5, FRACTION, FLOOR));
    }

    @Test
    public void neverExceedsFixedDistanceOrDropsBelowFloor() {
        assertEquals(FLOOR, SideWindowDismissDistancePolicy.resolvePx(
                FIXED, 100, 0.5f, FLOOR));
        assertEquals(1, SideWindowDismissDistancePolicy.resolvePx(
                1, 10, 0.5f, 0));
    }
}

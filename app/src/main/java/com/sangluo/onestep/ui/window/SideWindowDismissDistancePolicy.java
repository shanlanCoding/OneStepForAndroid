package com.sangluo.onestep.ui.window;

/**
 * Resolves the side-window swipe-to-dismiss distance. The fixed distance stays
 * comfortable on wide windows, but narrow windows (e.g. six side windows on a
 * phone) cap it to a fraction of the window edge so a dismissal stays reachable.
 */
public final class SideWindowDismissDistancePolicy {
    private SideWindowDismissDistancePolicy() {
    }

    /**
     * @param fixedDistancePx       preferred dismiss distance on roomy windows
     * @param smallestWindowEdgePx  width (horizontal layouts) or height (vertical
     *                              layouts) of the narrowest dismissible side
     *                              window; 0 or negative when unknown
     * @param fractionOfWindow      how much of the window edge may be required
     * @param minDistancePx         floor that keeps quick flicks working
     */
    public static int resolvePx(int fixedDistancePx, int smallestWindowEdgePx,
                                float fractionOfWindow, int minDistancePx) {
        int fixed = Math.max(1, fixedDistancePx);
        int floor = Math.max(1, minDistancePx);
        if (smallestWindowEdgePx <= 0 || fractionOfWindow <= 0f) {
            return fixed;
        }
        int fromWindow = Math.round(smallestWindowEdgePx * fractionOfWindow);
        return Math.max(floor, Math.min(fixed, fromWindow));
    }
}

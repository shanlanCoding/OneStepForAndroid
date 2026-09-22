package com.sangluo.onestep.feature.embedding;

/**
 * Separates physical system gestures from touches intended for the hosted display.
 * On HyperOS 3 / Android 16 the system gesture layer only reacts on the focused
 * display, so OneStep recognises and executes the physical gestures itself.
 */
public final class HostedTouchFocusPolicy {
    public static final int SYSTEM_GESTURE_NONE = 0;
    public static final int SYSTEM_GESTURE_BACK = 1;
    public static final int SYSTEM_GESTURE_HOME = 2;

    private HostedTouchFocusPolicy() {
    }

    public static boolean shouldReserveForSystemNavigation(
            float rawX, float rawY,
            int windowLeft, int windowTop, int windowRight, int windowBottom,
            int insetLeft, int insetTop, int insetRight, int insetBottom) {
        if (windowRight <= windowLeft || windowBottom <= windowTop
                || rawX < windowLeft || rawX > windowRight
                || rawY < windowTop || rawY > windowBottom) {
            return false;
        }
        return (insetLeft > 0 && rawX < windowLeft + insetLeft)
                || (insetTop > 0 && rawY < windowTop + insetTop)
                || (insetRight > 0 && rawX > windowRight - insetRight)
                || (insetBottom > 0 && rawY > windowBottom - insetBottom);
    }

    /**
     * Classifies a physical-screen gesture once it moves past the touch slop.
     * Edge-originating horizontal movement means BACK; a dominant upward drag
     * from the bottom strip means HOME. Anything else stays with the hosted app.
     */
    public static int resolvePhysicalSystemGesture(
            float downX, float downY, float currentX, float currentY,
            float viewWidth, float viewHeight,
            int edgeRegionPx, int bottomRegionPx, float slopPx) {
        float dx = currentX - downX;
        float dy = currentY - downY;
        if (dx * dx + dy * dy <= slopPx * slopPx) {
            return SYSTEM_GESTURE_NONE;
        }
        boolean fromLeftEdge = downX <= edgeRegionPx;
        boolean fromRightEdge = downX >= viewWidth - edgeRegionPx;
        boolean fromBottomEdge = downY >= viewHeight - bottomRegionPx;
        if ((fromLeftEdge || fromRightEdge) && Math.abs(dx) > Math.abs(dy)) {
            return SYSTEM_GESTURE_BACK;
        }
        if (fromBottomEdge && dy < 0 && Math.abs(dy) > Math.abs(dx)) {
            return SYSTEM_GESTURE_HOME;
        }
        return SYSTEM_GESTURE_NONE;
    }
}

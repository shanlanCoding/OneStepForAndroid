package com.sangluo.onestep.feature.embedding;

/**
 * Decides whether dismissing a hosted task should also stop its package.
 *
 * <p>Dismissal only hides the window; the package keeps running in the background
 * and cleanup is left to the platform's own process management. Force-stopping on
 * dismissal killed foreground instant-messaging apps mid-use (a WeChat forward
 * gesture hit the side-window dismiss gesture and looked like a crash), so the
 * policy no longer requests it for any app.
 */
public final class DismissedAppClosePolicy {
    private DismissedAppClosePolicy() {
    }

    public static boolean shouldForceStop(boolean homeEntry) {
        return false;
    }
}

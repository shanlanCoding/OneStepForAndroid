package com.sangluo.onestep;

import android.content.Intent;

import java.util.Set;

/** Keeps common activity-result contracts attached to their original caller task. */
final class CrossAppLaunchRoutingPolicy {
    /**
     * Payment apps whose verification chain (fingerprint / password / face) must run
     * with the system auth UI on the focused default display; inside a container the
     * verification activity dies before its dialog can appear.
     */
    private static final Set<String> PAYMENT_VERIFICATION_PACKAGES = Set.of(
            "com.eg.android.AlipayGphone");

    private CrossAppLaunchRoutingPolicy() {
    }

    /**
     * True when a cross-app launch should skip container routing and open on the
     * physical display instead. Payment verification chains need the system auth UI,
     * which only works on the focused default display; an unreachable or not-exported
     * target component also means OneStep could never start it itself.
     */
    static boolean shouldBypassContainerRouting(
            String targetPackage, boolean componentUnreachableOrNotExported) {
        if (componentUnreachableOrNotExported) {
            return true;
        }
        return targetPackage != null
                && PAYMENT_VERIFICATION_PACKAGES.contains(targetPackage);
    }

    static boolean shouldBypassHomeLaunch(
            String action, boolean hasHomeCategory, boolean hasSecondaryHomeCategory) {
        return Intent.ACTION_MAIN.equals(action)
                && (hasHomeCategory || hasSecondaryHomeCategory);
    }

    static boolean shouldPreserveCallerTask(String action, int flags) {
        if ((flags & Intent.FLAG_ACTIVITY_FORWARD_RESULT) != 0) {
            return true;
        }
        if (action == null) {
            return false;
        }
        switch (action) {
            case Intent.ACTION_GET_CONTENT:
            case Intent.ACTION_OPEN_DOCUMENT:
            case Intent.ACTION_CREATE_DOCUMENT:
            case Intent.ACTION_OPEN_DOCUMENT_TREE:
            case Intent.ACTION_PICK:
            case Intent.ACTION_PICK_ACTIVITY:
            case "android.provider.action.PICK_IMAGES":
            case "android.provider.action.PICK_IMAGES_MAX":
            case "android.media.action.IMAGE_CAPTURE":
            case "android.media.action.IMAGE_CAPTURE_SECURE":
            case "android.media.action.VIDEO_CAPTURE":
            case "android.provider.MediaStore.RECORD_SOUND":
            case "android.intent.action.RINGTONE_PICKER":
            case "android.speech.action.RECOGNIZE_SPEECH":
                return true;
            default:
                return false;
        }
    }
}

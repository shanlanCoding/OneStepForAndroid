package com.sangluo.onestep.feature.embedding;

import java.util.Locale;
import java.util.Set;

/**
 * Decides when a launch must carry the OEM clone authorization extras.
 *
 * <p>MIUI/HyperOS (and MIUI-derived clone frameworks) pop their "choose an
 * instance" resolver for launches that lack an authorization extra, which breaks
 * direct instance launches from the OneStep top bar and asks the user again on
 * every attempt. The extras authorize a launch to the exact user OneStep has
 * already resolved, so the resolver is skipped.
 */
public final class OemCloneLaunchPolicy {
    private static final Set<String> CLONE_RESOLVER_MANUFACTURERS = Set.of(
            "xiaomi", "redmi", "poco", "nubia", "zte");

    private OemCloneLaunchPolicy() {
    }

    public static boolean shouldAuthorizeDirectInstanceLaunch(String manufacturer) {
        return manufacturer != null
                && CLONE_RESOLVER_MANUFACTURERS.contains(
                        manufacturer.trim().toLowerCase(Locale.US));
    }
}

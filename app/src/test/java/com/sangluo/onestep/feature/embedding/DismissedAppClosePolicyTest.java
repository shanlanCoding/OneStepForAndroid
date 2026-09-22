package com.sangluo.onestep.feature.embedding;

import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class DismissedAppClosePolicyTest {
    @Test
    public void ordinaryAppKeepsRunningAfterDismissal() {
        assertFalse(DismissedAppClosePolicy.shouldForceStop(false));
    }

    @Test
    public void homePackageKeepsRunningAfterDismissal() {
        assertFalse(DismissedAppClosePolicy.shouldForceStop(true));
    }
}

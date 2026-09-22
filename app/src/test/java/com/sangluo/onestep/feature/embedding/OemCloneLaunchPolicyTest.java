package com.sangluo.onestep.feature.embedding;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Validates which manufacturers need the clone-resolver authorization extras. */
public class OemCloneLaunchPolicyTest {

    @Test
    public void xiaomiFamilyAuthorizesDirectInstanceLaunch() {
        assertTrue(OemCloneLaunchPolicy.shouldAuthorizeDirectInstanceLaunch("Xiaomi"));
        assertTrue(OemCloneLaunchPolicy.shouldAuthorizeDirectInstanceLaunch("Redmi"));
        assertTrue(OemCloneLaunchPolicy.shouldAuthorizeDirectInstanceLaunch("POCO"));
        assertTrue(OemCloneLaunchPolicy.shouldAuthorizeDirectInstanceLaunch(" xiaomi "));
    }

    @Test
    public void legacyCloneResolverManufacturersStillAuthorize() {
        assertTrue(OemCloneLaunchPolicy.shouldAuthorizeDirectInstanceLaunch("nubia"));
        assertTrue(OemCloneLaunchPolicy.shouldAuthorizeDirectInstanceLaunch("ZTE"));
    }

    @Test
    public void otherManufacturersKeepDefaultLaunch() {
        assertFalse(OemCloneLaunchPolicy.shouldAuthorizeDirectInstanceLaunch("Google"));
        assertFalse(OemCloneLaunchPolicy.shouldAuthorizeDirectInstanceLaunch("samsung"));
        assertFalse(OemCloneLaunchPolicy.shouldAuthorizeDirectInstanceLaunch(null));
        assertFalse(OemCloneLaunchPolicy.shouldAuthorizeDirectInstanceLaunch(""));
    }
}

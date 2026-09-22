package com.sangluo.onestep.data.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.sangluo.onestep.data.settings.OneStepSettingsStore.TopAppListConfig;
import com.sangluo.onestep.data.settings.SettingsBackupCodec.BackupData;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

/** Validates the user-facing settings backup encode/decode contract. */
public class SettingsBackupCodecTest {

    private static TopAppListConfig unconfigured() {
        return new TopAppListConfig(false, Collections.emptyList(),
                Collections.emptySet());
    }

    private static OneStepSettings sampleSettings() {
        return new OneStepSettings(
                5, 4,
                110, 85, 60,
                false, true, true, 5,
                70, 115, 145, true);
    }

    @Test
    public void roundTrip_preservesAllValues() {
        OneStepSettings settings = sampleSettings();
        TopAppListConfig topAppList = new TopAppListConfig(true,
                Arrays.asList("com.a/pkg/0", "com.b/pkg/0", "com.c/pkg/0"),
                new LinkedHashSet<>(Arrays.asList("com.a/pkg/0", "com.c/pkg/0")));
        String json = SettingsBackupCodec.encode(settings, topAppList,
                "content://downloads/document/7");
        BackupData restored = SettingsBackupCodec.decode(json);

        assertEquals(settings.desktopGridRows, restored.settings.desktopGridRows);
        assertEquals(settings.desktopGridColumns, restored.settings.desktopGridColumns);
        assertEquals(settings.topAppIconScalePct, restored.settings.topAppIconScalePct);
        assertEquals(settings.topAppStripSpacingScalePct,
                restored.settings.topAppStripSpacingScalePct);
        assertEquals(settings.topAppStripVerticalPaddingScalePct,
                restored.settings.topAppStripVerticalPaddingScalePct);
        assertEquals(settings.topComponentsVisible, restored.settings.topComponentsVisible);
        assertEquals(settings.statusBarSpacingEnabled,
                restored.settings.statusBarSpacingEnabled);
        assertEquals(settings.verticalWindowLayout, restored.settings.verticalWindowLayout);
        assertEquals(settings.sideWindowCount, restored.settings.sideWindowCount);
        assertEquals(settings.topNavVerticalMarginScalePct,
                restored.settings.topNavVerticalMarginScalePct);
        assertEquals(settings.oneStepTriggerAreaScalePct,
                restored.settings.oneStepTriggerAreaScalePct);
        assertEquals(settings.cornerTriggerSensitivityPct,
                restored.settings.cornerTriggerSensitivityPct);
        assertEquals(settings.logRecordingEnabled, restored.settings.logRecordingEnabled);
        assertTrue(restored.topAppConfigured);
        assertEquals(topAppList.orderedKeys, restored.topAppOrder);
        assertEquals(topAppList.selectedKeys, restored.topAppSelected);
        assertEquals("content://downloads/document/7", restored.backgroundUri);
    }

    @Test
    public void decode_sanitizesOutOfRangeValues() {
        OneStepSettings settings = new OneStepSettings(
                99, 99,
                999, 1, 55,
                true, false, false, 42,
                999, 5, 9999, false);
        String json = SettingsBackupCodec.encode(settings, unconfigured(), null);
        BackupData restored = SettingsBackupCodec.decode(json);

        assertEquals(OneStepSettings.DESKTOP_PAGE_ROWS, restored.settings.desktopGridRows);
        assertEquals(OneStepSettings.DESKTOP_PAGE_COLUMNS, restored.settings.desktopGridColumns);
        assertEquals(OneStepSettings.TOP_APP_ICON_SCALE_MAX,
                restored.settings.topAppIconScalePct);
        assertEquals(OneStepSettings.TOP_APP_STRIP_SPACING_SCALE_MIN,
                restored.settings.topAppStripSpacingScalePct);
        assertEquals(55, restored.settings.topAppStripVerticalPaddingScalePct);
        assertEquals(OneStepSettings.MAX_SIDE_WINDOWS, restored.settings.sideWindowCount);
        assertEquals(OneStepSettings.TOP_NAV_VERTICAL_MARGIN_SCALE_MAX,
                restored.settings.topNavVerticalMarginScalePct);
        assertEquals(OneStepSettings.ONE_STEP_TRIGGER_AREA_SCALE_MIN,
                restored.settings.oneStepTriggerAreaScalePct);
        assertEquals(OneStepSettings.CORNER_TRIGGER_SENSITIVITY_MAX,
                restored.settings.cornerTriggerSensitivityPct);
        assertFalse(restored.topAppConfigured);
        assertNull(restored.backgroundUri);
    }

    @Test
    public void decode_rejectsForeignAndBrokenPayloads() {
        String wrongSchema = "{\"schema\":99,\"app\":\"OneStep4\"}";
        String wrongApp = "{\"schema\":1,\"app\":\"SomethingElse\"}";
        String noSettings = "{\"schema\":1,\"app\":\"OneStep4\"}";
        String notJson = "not json at all";
        String empty = "   ";
        for (String bad : new String[]{wrongSchema, wrongApp, noSettings, notJson, empty}) {
            try {
                SettingsBackupCodec.decode(bad);
                fail("Expected rejection for: " + bad);
            } catch (IllegalArgumentException expected) {
                // Every unusable payload must fail loudly instead of silently resetting.
            }
        }
    }

    @Test
    public void decode_toleratesMissingOptionalSections() {
        StringBuilder json = new StringBuilder();
        json.append("{\"schema\":1,\"app\":\"OneStep4\",\"settings\":{}}");
        BackupData restored = SettingsBackupCodec.decode(json.toString());
        assertEquals(OneStepSettings.DESKTOP_PAGE_ROWS, restored.settings.desktopGridRows);
        assertEquals(OneStepSettings.DEFAULT_SIDE_WINDOWS, restored.settings.sideWindowCount);
        assertFalse(restored.topAppConfigured);
        assertNull(restored.backgroundUri);
    }

    @Test
    public void encode_skipsUnconfiguredTopAppList() {
        String json = SettingsBackupCodec.encode(sampleSettings(),
                unconfigured(), null);
        assertFalse(json.contains("topAppList"));
        assertFalse(json.contains("backgroundUri"));
        BackupData restored = SettingsBackupCodec.decode(json);
        assertFalse(restored.topAppConfigured);
    }
}

package com.sangluo.onestep.data.settings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Encodes and decodes the user-facing settings backup file. Pure policy: all
 * Android IO stays with callers so the codec stays unit-testable on the JVM.
 */
public final class SettingsBackupCodec {
    public static final int SCHEMA_VERSION = 1;
    public static final String APP_MARKER = "OneStep4";

    private SettingsBackupCodec() {
    }

    /** Restored backup payload; every value is already sanitized for this build. */
    public static final class BackupData {
        public final OneStepSettings settings;
        public final boolean topAppConfigured;
        public final List<String> topAppOrder;
        public final Set<String> topAppSelected;
        public final String backgroundUri;

        BackupData(OneStepSettings settings, boolean topAppConfigured,
                   List<String> topAppOrder, Set<String> topAppSelected,
                   String backgroundUri) {
            this.settings = settings;
            this.topAppConfigured = topAppConfigured;
            this.topAppOrder = topAppOrder;
            this.topAppSelected = topAppSelected;
            this.backgroundUri = backgroundUri;
        }
    }

    public static String encode(OneStepSettings settings,
                                OneStepSettingsStore.TopAppListConfig topAppList,
                                String backgroundUri) {
        JSONObject root = new JSONObject();
        try {
            root.put("schema", SCHEMA_VERSION);
            root.put("app", APP_MARKER);
            root.put("settings", encodeSettings(settings));
            if (topAppList != null && topAppList.configured) {
                root.put("topAppList", new JSONObject()
                        .put("order", encodeStringList(topAppList.orderedKeys))
                        .put("selected", encodeStringList(new ArrayList<>(topAppList.selectedKeys))));
            }
            if (backgroundUri != null && !backgroundUri.isEmpty()) {
                root.put("backgroundUri", backgroundUri);
            }
            return root.toString();
        } catch (JSONException e) {
            throw new IllegalStateException("Unable to encode settings backup", e);
        }
    }

    /** Parses a backup payload; throws IllegalArgumentException on unusable input. */
    public static BackupData decode(String json) {
        if (json == null || json.trim().isEmpty()) {
            throw new IllegalArgumentException("Backup payload is empty");
        }
        JSONObject root;
        try {
            root = new JSONObject(json);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Backup payload is not valid JSON", e);
        }
        if (root.optInt("schema", -1) != SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported backup schema: "
                    + root.optInt("schema", -1));
        }
        String app = root.optString("app", "");
        if (!APP_MARKER.equals(app)) {
            throw new IllegalArgumentException("Backup was not created by OneStep4");
        }
        JSONObject settingsJson = root.optJSONObject("settings");
        if (settingsJson == null) {
            throw new IllegalArgumentException("Backup payload has no settings section");
        }
        OneStepSettings settings = decodeSettings(settingsJson);

        boolean topAppConfigured = false;
        List<String> topAppOrder = new ArrayList<>();
        Set<String> topAppSelected = new LinkedHashSet<>();
        JSONObject topAppJson = root.optJSONObject("topAppList");
        if (topAppJson != null) {
            topAppConfigured = true;
            topAppOrder = readStringList(topAppJson.optJSONArray("order"));
            topAppSelected = new LinkedHashSet<>(
                    readStringList(topAppJson.optJSONArray("selected")));
        }
        String backgroundUri = root.optString("backgroundUri", "");
        if (backgroundUri.isEmpty()) {
            backgroundUri = null;
        }
        return new BackupData(settings, topAppConfigured, topAppOrder,
                topAppSelected, backgroundUri);
    }

    private static JSONObject encodeSettings(OneStepSettings settings) throws JSONException {
        return new JSONObject()
                .put("gridRows", settings.desktopGridRows)
                .put("gridColumns", settings.desktopGridColumns)
                .put("topAppIconScalePct", settings.topAppIconScalePct)
                .put("topAppStripSpacingScalePct", settings.topAppStripSpacingScalePct)
                .put("topAppStripVerticalPaddingScalePct",
                        settings.topAppStripVerticalPaddingScalePct)
                .put("topComponentsVisible", settings.topComponentsVisible)
                .put("statusBarSpacingEnabled", settings.statusBarSpacingEnabled)
                .put("verticalWindowLayout", settings.verticalWindowLayout)
                .put("sideWindowCount", settings.sideWindowCount)
                .put("topNavVerticalMarginScalePct", settings.topNavVerticalMarginScalePct)
                .put("oneStepTriggerAreaScalePct", settings.oneStepTriggerAreaScalePct)
                .put("cornerTriggerSensitivityPct", settings.cornerTriggerSensitivityPct)
                .put("logRecordingEnabled", settings.logRecordingEnabled);
    }

    private static OneStepSettings decodeSettings(JSONObject json) {
        return new OneStepSettings(
                OneStepSettings.sanitizeGridRows(json.optInt("gridRows",
                        OneStepSettings.DESKTOP_PAGE_ROWS)),
                OneStepSettings.sanitizeGridColumns(json.optInt("gridColumns",
                        OneStepSettings.DESKTOP_PAGE_COLUMNS)),
                OneStepSettings.sanitizeTopAppIconScale(json.optInt("topAppIconScalePct",
                        OneStepSettings.TOP_APP_ICON_SCALE_DEFAULT)),
                OneStepSettings.sanitizeTopAppStripSpacingScale(
                        json.optInt("topAppStripSpacingScalePct",
                                OneStepSettings.TOP_APP_STRIP_SPACING_SCALE_DEFAULT)),
                OneStepSettings.sanitizeTopAppStripVerticalPaddingScale(
                        json.optInt("topAppStripVerticalPaddingScalePct",
                                OneStepSettings.TOP_APP_STRIP_VERTICAL_PADDING_SCALE_DEFAULT)),
                json.optBoolean("topComponentsVisible", true),
                json.optBoolean("statusBarSpacingEnabled", false),
                json.optBoolean("verticalWindowLayout", false),
                OneStepSettings.sanitizeAllowedSideWindowCount(json.optInt("sideWindowCount",
                        OneStepSettings.DEFAULT_SIDE_WINDOWS)),
                OneStepSettings.sanitizeTopNavVerticalMarginScale(
                        json.optInt("topNavVerticalMarginScalePct",
                                OneStepSettings.TOP_NAV_VERTICAL_MARGIN_SCALE_DEFAULT)),
                OneStepSettings.sanitizeOneStepTriggerAreaScale(
                        json.optInt("oneStepTriggerAreaScalePct",
                                OneStepSettings.ONE_STEP_TRIGGER_AREA_SCALE_DEFAULT)),
                OneStepSettings.sanitizeCornerTriggerSensitivity(
                        json.optInt("cornerTriggerSensitivityPct",
                                OneStepSettings.CORNER_TRIGGER_SENSITIVITY_DEFAULT)),
                json.optBoolean("logRecordingEnabled", false));
    }

    private static JSONArray encodeStringList(List<String> values) {
        JSONArray array = new JSONArray();
        if (values != null) {
            for (String value : values) {
                if (value != null && !value.isEmpty()) {
                    array.put(value);
                }
            }
        }
        return array;
    }

    private static List<String> readStringList(JSONArray array) {
        List<String> values = new ArrayList<>();
        if (array != null) {
            for (int i = 0; i < array.length(); i++) {
                String value = array.optString(i, "");
                if (!value.isEmpty()) {
                    values.add(value);
                }
            }
        }
        return values;
    }
}

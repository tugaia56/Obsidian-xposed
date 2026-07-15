package it.tugaia56.obsidian.utils;

import android.content.Context;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import it.tugaia56.obsidian.ui.models.DarkShadowItem;

public class DarkShadowUtils {

    public static final String PREF_PREFIX               = "DST_";
    public static final String PREF_PIN                  = "DST_PIN";
    public static final String PREF_PIN_NUM              = "DST_PIN_NUM";
    public static final String PREF_PIN_CUSTOM_COLOR     = "DST_PIN_CUSTOM_COLOR";
    public static final String PREF_PIN_NUM_CUSTOM_COLOR = "DST_PIN_NUM_CUSTOM_COLOR";

    private static final String PKG_FRAMEWORK = "android";

    // ── Resource lists ────────────────────────────────────────────────────────

    /** ACCENT1 — main dark accent + all system accent shades */
    private static final List<String> ACCENT1_RES = Arrays.asList(
        "accent_material_dark",
        "holo_blue_light",
        "system_accent1_100", "system_accent1_200", "system_accent1_300",
        "system_accent1_400", "system_accent1_500", "system_accent1_600", "system_accent1_700",
        "system_accent2_100", "system_accent2_200", "system_accent2_300",
        "system_accent2_400", "system_accent2_500", "system_accent2_600", "system_accent2_700",
        "system_accent3_100", "system_accent3_200", "system_accent3_300",
        "system_accent3_400", "system_accent3_500", "system_accent3_600", "system_accent3_700",
        "system_secondary_container"
    );

    /** ACCENT2 — light accent variant */
    private static final List<String> ACCENT2_RES = Arrays.asList(
        "accent_material_light"
    );

    /** ACCENT3 — ripple / touch feedback */
    private static final List<String> ACCENT3_RES = Arrays.asList(
        "ripple_material_dark"
    );

    /** BACKGROUND — system dark background resources (same as OC) */
    private static final List<String> BACKGROUND_RES = Arrays.asList(
        "background_dark",
        "background_device_default_dark",
        "legacy_primary",
        "legacy_primary_dark",
        "black",
        "primary_dark_material_dark",
        "primary_material_dark"
    );

    /**
     * Background lighter variants (cards, buttons, dialogs, etc.).
     * Values are brightness offsets: +25% = slightly lighter than base, etc.
     */
    private static final Map<String, Integer> BACKGROUND_ADJUST;
    static {
        BACKGROUND_ADJUST = new LinkedHashMap<>();
        BACKGROUND_ADJUST.put("background_floating_material_dark", 25);
        BACKGROUND_ADJUST.put("button_material_dark",              30);
        BACKGROUND_ADJUST.put("holo_primary",                      35);
        BACKGROUND_ADJUST.put("holo_light_primary_dark",           40);
        BACKGROUND_ADJUST.put("button_material_light",             45);
        BACKGROUND_ADJUST.put("holo_primary_dark",                 50);
        BACKGROUND_ADJUST.put("background_holo_dark",              55);
        BACKGROUND_ADJUST.put("background_leanback_dark",          60);
    }

    // ── Item factory ─────────────────────────────────────────────────────────

    public static List<DarkShadowItem> getItems(Context ctx) {
        List<DarkShadowItem> list = new ArrayList<>();

        // ── Accent 1: Accento Principale ────────────────────────────────────
        int c1 = ObsidianPrefs.getInt(PREF_PREFIX + "ACCENT1", 0xFF6200EE);
        boolean on1 = ObsidianPrefs.getBoolean(PREF_PREFIX + "ACCENT1_on", false);
        list.add(new DarkShadowItem(
                ctx.getString(it.tugaia56.obsidian.R.string.section_accent1),
                "ACCENT1",
                List.of(PKG_FRAMEWORK), ACCENT1_RES, null,
                c1, on1));

        // ── Accent 2: Accento Chiaro ─────────────────────────────────────────
        int c2 = ObsidianPrefs.getInt(PREF_PREFIX + "ACCENT2", 0xFF3700B3);
        boolean on2 = ObsidianPrefs.getBoolean(PREF_PREFIX + "ACCENT2_on", false);
        list.add(new DarkShadowItem(
                ctx.getString(it.tugaia56.obsidian.R.string.section_accent2),
                "ACCENT2",
                List.of(PKG_FRAMEWORK), ACCENT2_RES, null,
                c2, on2));

        // ── Accent 3: Ripple ─────────────────────────────────────────────────
        int c3 = ObsidianPrefs.getInt(PREF_PREFIX + "ACCENT3", 0x336200EE);
        boolean on3 = ObsidianPrefs.getBoolean(PREF_PREFIX + "ACCENT3_on", false);
        list.add(new DarkShadowItem(
                ctx.getString(it.tugaia56.obsidian.R.string.section_accent3),
                "ACCENT3",
                List.of(PKG_FRAMEWORK), ACCENT3_RES, null,
                c3, on3));

        // ── Background ───────────────────────────────────────────────────────
        // Applies FabricatedOverlay to framework background resources (like OC).
        // The saved color is also read by the QsBackground hook for solid QS BG.
        int bg = ObsidianPrefs.getInt(PREF_PREFIX + "BACKGROUND", 0xFF1A1A2E);
        boolean bgOn = ObsidianPrefs.getBoolean(PREF_PREFIX + "BACKGROUND_on", false);
        list.add(new DarkShadowItem(
                ctx.getString(it.tugaia56.obsidian.R.string.dst_background),
                "BACKGROUND",
                List.of(PKG_FRAMEWORK), BACKGROUND_RES, BACKGROUND_ADJUST,
                bg, bgOn));

        return list;
    }

    public static void saveColor(DarkShadowItem item) {
        ObsidianPrefs.putInt(PREF_PREFIX + item.getOverlayName(), item.getColor());
        ObsidianPrefs.putBoolean(PREF_PREFIX + item.getOverlayName() + "_on", true);
    }
}

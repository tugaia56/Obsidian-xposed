package it.tugaia56.obsidian.utils;

import android.graphics.Color;

import com.topjohnwu.superuser.Shell;

import org.json.JSONObject;

/**
 * Applies a custom primary colour to OOS 16's system colour engine:
 *  1. Writes /data/oplus/uxres/uxcolor/ux_custom_color[_night].xml
 *     (the files OOS reads for couiSingleFirst* colours)
 *  2. Sets settings secure theme_customization_overlay_packages
 *     (the standard Android Monet/Material-You customisation key)
 *
 * Derived from OC's ColorsFragment — same mechanism, confirmed working.
 */
public class SystemColorUtils {

    private static final String UX_COLOR_DIR = "/data/oplus/uxres/uxcolor";

    /** Pref key where we persist the last applied system color. */
    public static final String PREF_SYSTEM_COLOR = "SYSTEM_COLOR";

    /**
     * Apply {@code color} as the OOS system primary colour.
     * Runs synchronously — call from a background thread.
     *
     * @return true if commands executed without shell error.
     */
    public static boolean apply(int color) {
        try {
            String hex8 = String.format("#%08X", color & 0xFFFFFFFFL);   // #FFRRGGBB
            String hex8bare = String.format("%08X", color & 0xFFFFFFFFL); // FFRRGGBB

            // ── Day colours ──────────────────────────────────────────────────
            int pressed       = ColorUtils.adjustColorForPressed(color, 0.3f);
            int lightNormal   = ColorUtils.adjustAlpha(color, 0.3f);
            int lightPressed  = ColorUtils.adjustAlpha(pressed, 0.3f);
            int textHighlight = ColorUtils.adjustAlpha(color, 0.15f);

            writeColorXml(buildXml(hex8, pressed, lightNormal, lightPressed, textHighlight),
                    "ux_custom_color.xml");

            // ── Night colours ─────────────────────────────────────────────────
            int nightPressed      = ColorUtils.adjustColorForPressed(color, 0.7f);
            int nightLightNormal  = ColorUtils.adjustAlpha(color, 0.4f);
            int nightLightPressed = ColorUtils.adjustAlpha(nightPressed, 0.3f);

            writeColorXml(buildXml(hex8, nightPressed, nightLightNormal, nightLightPressed, textHighlight),
                    "ux_custom_color_night.xml");

            // ── Secure settings ───────────────────────────────────────────────
            long ts = System.currentTimeMillis();
            JSONObject json = new JSONObject();
            json.put("_applied_timestamp", ts);
            json.put("android.theme.customization.theme_style", "TONAL_SPOT");
            json.put("android.theme.customization.color_source", "home_wallpaper");
            json.put("material_you_overlay_enable", 1);
            json.put("android.theme.customization.color_index", 0);
            json.put("android.theme.customization.system_palette", hex8bare);
            json.put("android.theme.customization.accent_color",   hex8bare);

            Shell.cmd("settings put secure theme_customization_overlay_packages '"
                    + json.toString() + "'").exec();

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static String buildXml(String hex8, int pressed, int lightNormal,
                                   int lightPressed, int textHighlight) {
        String p   = fmt(pressed);
        String ln  = fmt(lightNormal);
        String lp  = fmt(lightPressed);
        String th  = fmt(textHighlight);
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>\n"
            + "<resources>\n"
            + "<color name=\"couiSingleFirstNormal\">"          + hex8 + "</color>\n"
            + "<color name=\"couiSingleFirstPressed\">"         + p    + "</color>\n"
            + "<color name=\"couiSingleFirstLightNormal\">"     + ln   + "</color>\n"
            + "<color name=\"couiSingleFirstLightPressed\">"    + lp   + "</color>\n"
            + "<color name=\"couiSingleFirstTextHighLight\">"   + th   + "</color>\n"
            + "<color name=\"couiSingleFirstBarDisabledColor\">"+ th   + "</color>\n"
            + "<color name=\"NXcolorSingleFirstNormal\">"       + hex8 + "</color>\n"
            + "<color name=\"NXcolorSingleFirstPressed\">"      + p    + "</color>\n"
            + "<color name=\"NXcolorSingleFirstLightNormal\">"  + ln   + "</color>\n"
            + "<color name=\"NXcolorSingleFirstLightPressed\">" + lp   + "</color>\n"
            + "<color name=\"NXcolorSingleFirstTextHighLight\">"+ th   + "</color>\n"
            + "<color name=\"NXcolorSingleFirstBarDisabledColor\">"+ th + "</color>\n"
            + "</resources>";
    }

    private static String fmt(int color) {
        return String.format("#%08X", color & 0xFFFFFFFFL);
    }

    private static void writeColorXml(String content, String fileName) {
        // Use printf instead of echo to avoid escape issues
        String escaped = content.replace("'", "'\\''");
        Shell.cmd("mkdir -p " + UX_COLOR_DIR,
                  "printf '" + escaped + "' > " + UX_COLOR_DIR + "/" + fileName).exec();
    }
}

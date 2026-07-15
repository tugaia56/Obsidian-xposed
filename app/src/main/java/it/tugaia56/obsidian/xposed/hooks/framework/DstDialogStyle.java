package it.tugaia56.obsidian.xposed.hooks.framework;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;

import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;

import it.tugaia56.obsidian.utils.ColorUtils;
import android.content.res.XResources;

/**
 * Xposed hook: replaces android:drawable/dialog_background_material with one
 * of 8 DST dialog style presets (identical to OC's DSTD**** RRO overlays).
 *
 * Uses a preload-from-disk pattern because:
 *  - android package resources are initialized in EVERY process
 *  - ContentProvider (Xprefs) is unavailable in system_server / early boot
 *  - Always re-reads to avoid stale zygote fork statics
 *
 * Preset codes (stored in DST_DLG_PRESET_NAME):
 *   DSTDHT  – Dialog Higher Transparent
 *   DSTDHTO – Dialog Higher Transparent Outlined
 *   DSTDLT  – Dialog Lower Transparent
 *   DSTDLYO – Dialog Lower Transparent Outlined
 *   DSTDMT  – Dialog Medium Transparent
 *   DSTDMTO – Dialog Medium Transparent Outlined
 *   DSTDS   – Dialog Solid
 *   DSTDSO  – Dialog Solid Outlined
 */
public class DstDialogStyle {

    private static final String PKG_ANDROID   = "android";
    private static final String DRAWABLE_NAME = "dialog_background_material";
    private static final String PREF_PRESET   = "DST_DLG_PRESET_NAME";
    private static final String PREF_ACCENT1  = "DST_ACCENT1";
    private static final String PREF_BG       = "DST_BACKGROUND";
    private static final String PREFS_FILE    =
        "/data/user_de/0/it.tugaia56.obsidian/shared_prefs/it.tugaia56.obsidian_preferences.xml";

    // ── Preloaded statics — refreshed on every applyPreloaded() call ─────────
    private static volatile boolean sPreloaded = false;
    private static volatile String  sDlgPreset = null;  // "DSTDHT" … "DSTDSO"
    private static volatile int     sAccent    = 0xFFFFFFFF;
    private static volatile int     sBg        = 0xFF1B2029;

    // ── Boot-time preload (called from ResourceManager.initZygote) ───────────

    public static void preloadFromFile() {
        try {
            java.io.File f = new java.io.File(PREFS_FILE);
            if (!f.exists()) return;
            java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(f));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            br.close();
            String xml = sb.toString();

            sDlgPreset = parseStringContent(xml, PREF_PRESET);
            sAccent    = parseInt(parseAttr(xml, PREF_ACCENT1, "value"), 0xFFFFFFFF);
            sBg        = parseInt(parseAttr(xml, PREF_BG,      "value"), 0xFF1B2029);
            sPreloaded = true;
            XposedBridge.log("[ Obsidian ] DstDialogStyle.preload: preset=" + sDlgPreset);
        } catch (Throwable t) {
            XposedBridge.log("[ Obsidian ] DstDialogStyle.preload ERROR: " + t);
        }
    }

    // ── System-property fallback (same EACCES workaround as MonetFreeze) ──────

    /**
     * Reads persist.obsidian.dst.dlg_preset (and accent/bg colours) via
     * SystemProperties reflection.  Called when preloadFromFile() fails due to
     * EACCES at early boot (system server reads prefs before initZygote chmod).
     */
    private static void preloadFromProps() {
        try {
            Class<?> sp  = Class.forName("android.os.SystemProperties");
            java.lang.reflect.Method get =
                    sp.getDeclaredMethod("get", String.class, String.class);
            get.setAccessible(true);
            String preset = (String) get.invoke(null, "persist.obsidian.dst.dlg_preset", "");
            String a1Str  = (String) get.invoke(null, "persist.obsidian.dst.a1",          "");
            String bgStr  = (String) get.invoke(null, "persist.obsidian.dst.bg",           "");
            if (preset.isEmpty()) return;
            sDlgPreset = preset;
            if (!a1Str.isEmpty()) sAccent = Integer.parseInt(a1Str);
            if (!bgStr.isEmpty()) sBg     = Integer.parseInt(bgStr);
            sPreloaded = true;
            XposedBridge.log("[ Obsidian ] DstDialogStyle.preload(props): preset=" + sDlgPreset);
        } catch (Throwable t) {
            XposedBridge.log("[ Obsidian ] DstDialogStyle.preload(props) ERROR: " + t);
        }
    }

    // ── Called from ResourceManager.handleInitPackageResources ───────────────

    public static void applyPreloaded(XC_InitPackageResources.InitPackageResourcesParam rp) {
        if (!PKG_ANDROID.equals(rp.packageName)) return;
        preloadFromFile(); // always re-read — don't rely on stale zygote statics
        if (!sPreloaded || sDlgPreset == null) preloadFromProps(); // EACCES fallback
        if (!sPreloaded || sDlgPreset == null) return;

        // Capture current statics for the DrawableLoader closure
        final String preset = sDlgPreset;
        final int    accent = sAccent;
        final int    bg     = sBg;

        try {
            rp.res.setReplacement(PKG_ANDROID, "drawable", DRAWABLE_NAME,
                new XResources.DrawableLoader() {
                    @Override
                    public Drawable newDrawable(XResources res, int id) {
                        return buildDrawable(preset, accent, bg,
                                res.getDisplayMetrics().density);
                    }
                });
        } catch (Throwable t) {
            XposedBridge.log("[ Obsidian ] DstDialogStyle.applyPreloaded ERROR: " + t);
        }
    }

    // ── Drawable factory ──────────────────────────────────────────────────────

    private static Drawable buildDrawable(String preset, int accent, int bg, float density) {
        float corner = 14f * density;
        int   stroke = Math.round(1.5f * density);

        switch (preset) {
            case "DSTDHT":  return shape(0x331b2029, 0,      0,      corner);
            case "DSTDHTO": return shape(0x331b2029, accent, stroke, corner);
            case "DSTDLT":  return shape(0xcc1b2029, 0,      0,      corner);
            case "DSTDLYO": return shape(0xcc1b2029, accent, stroke, corner);
            case "DSTDMT":  return shape(0x801b2029, 0,      0,      corner);
            case "DSTDMTO": return shape(0x80000000, accent, stroke, corner);
            case "DSTDS":   return shape(ColorUtils.adjustColor(bg, 30), 0, 0, corner);
            case "DSTDSO": {
                int inset = Math.round(16f * density);
                return new InsetDrawable(shape(bg, accent, stroke, corner), inset);
            }
            default:
                // Unknown preset — transparent fallback (stock drawable used instead)
                return shape(0x00000000, 0, 0, corner);
        }
    }

    private static GradientDrawable shape(int fill, int strokeColor, int strokeWidth,
                                          float cornerRadius) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setColor(fill);
        d.setCornerRadius(cornerRadius);
        if (strokeWidth > 0) d.setStroke(strokeWidth, strokeColor);
        return d;
    }

    // ── XML parse helpers (duplicated from MonetFreeze for independence) ──────

    private static int parseInt(String s, int def) {
        if (s == null) return def;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return def; }
    }

    private static String parseAttr(String xml, String name, String attr) {
        int idx = xml.indexOf("name=\"" + name + "\"");
        if (idx < 0) return null;
        String key = attr + "=\"";
        int s = xml.indexOf(key, idx);
        if (s < 0) return null;
        s += key.length();
        int e = xml.indexOf("\"", s);
        return e < 0 ? null : xml.substring(s, e);
    }

    private static String parseStringContent(String xml, String name) {
        String needle = "name=\"" + name + "\">";
        int idx = xml.indexOf(needle);
        if (idx < 0) return null;
        int s = idx + needle.length();
        int e = xml.indexOf("<", s);
        return e <= s ? null : xml.substring(s, e).trim();
    }
}

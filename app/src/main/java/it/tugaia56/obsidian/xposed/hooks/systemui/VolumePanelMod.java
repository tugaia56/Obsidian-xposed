package it.tugaia56.obsidian.xposed.hooks.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static it.tugaia56.obsidian.xposed.XPrefs.Xprefs;

import android.content.Context;
import android.content.res.ColorStateList;
import android.widget.ImageView;

import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import it.tugaia56.obsidian.utils.Constants;
import it.tugaia56.obsidian.xposed.XposedMods;

/**
 * Volume panel mods (SystemUI):
 *   – Panel position  (isOplusVolumeKeyInRight)
 *   – Timeout         (computeTimeoutH)
 *   – Disable warning (showSafetyWarningH / onShowSafetyWarning)
 *   – Slider colors   (OplusVolumeSeekBar + initRow)
 */
public class VolumePanelMod extends XposedMods {

    private static final String LISTEN = Constants.Packages.SYSTEM_UI;

    // ── Pref keys (saved by VolumePanelFragment / read here via Xprefs) ───────
    public static final String PREF_POSITION        = "volume_panel_position";           // "0"|"1"|"2"
    public static final String PREF_TIMEOUT         = "volume_panel_timeout";            // int seconds
    public static final String PREF_CUSTOM_PROGRESS = "volume_panel_seekbar_color_enabled";
    public static final String PREF_PROGRESS_COLOR  = "volume_panel_seekbar_color";      // int ARGB
    public static final String PREF_CUSTOM_BG       = "volume_panel_seekbar_bg_color_enabled";
    public static final String PREF_BG_COLOR        = "volume_panel_seekbar_bg_color";   // int ARGB
    /** "0"=predefinito "1"=scura "2"=bianca "3"=accento "4"=personalizzata — stessa
     *  struttura di qs_brightness_icon_mode in QsTilesCustomizeMod, su richiesta esplicita
     *  dell'utente ("prepara la stessa opzione per cursore Volume"). */
    public static final String PREF_ICON_MODE       = "qs_volume_icon_mode";
    public static final String PREF_ICON_COLOR      = "qs_volume_icon_custom_color";

    // ── Runtime state ─────────────────────────────────────────────────────────
    private int     mPosition        = 0;
    private int     mTimeoutMs       = 3_000;
    private boolean mCustomProgress  = false;
    private int     mProgressColor   = 0xFFFFFFFF;
    private boolean mCustomBg        = false;
    private int     mBgColor         = 0xFF808080;
    private int     mIconMode        = 0;
    private int     mIconColor       = 0xFFFFFFFF;

    /** Reference to OplusVolumeDialogImpl for live-update of colors. */
    private Object  mOVDI            = null;

    public VolumePanelMod(Context context) { super(context); }

    // ── updatePrefs ──────────────────────────────────────────────────────────

    @Override
    public void updatePrefs(String... Key) {
        if (Xprefs == null) return;
        mPosition       = Integer.parseInt(Xprefs.getString(PREF_POSITION, "0"));
        mTimeoutMs      = Xprefs.getInt(PREF_TIMEOUT, 3) * 1_000;
        mCustomProgress = Xprefs.getBoolean(PREF_CUSTOM_PROGRESS, false);
        mProgressColor  = Xprefs.getBoolean(PREF_PROGRESS_COLOR + "_use_accent", false)
                ? appAccentColor() : Xprefs.getInt(PREF_PROGRESS_COLOR, 0xFFFFFFFF);
        mCustomBg       = Xprefs.getBoolean(PREF_CUSTOM_BG, false);
        mBgColor        = Xprefs.getInt(PREF_BG_COLOR, 0xFF808080);
        try { mIconMode = Integer.parseInt(Xprefs.getString(PREF_ICON_MODE, "0")); } catch (Throwable t) { mIconMode = 0; }
        mIconColor      = Xprefs.getInt(PREF_ICON_COLOR, 0xFFFFFFFF);

        // Live-update color on open dialog if already constructed
        if (Key.length > 0 && mOVDI != null) applyColorsToAll();
    }

    // ── handleLoadPackage ────────────────────────────────────────────────────

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        hookPosition(lpparam.classLoader);
        hookTimeout(lpparam.classLoader);
        hookColors(lpparam.classLoader);
    }

    // ── Position hook ─────────────────────────────────────────────────────────

    private void hookPosition(ClassLoader cl) {
        for (String cn : new String[]{
                "com.oplusos.systemui.common.feature.FeatureOption",
                "com.oplus.systemui.common.feature.FeatureOption"}) {
            try {
                hookAllMethods(findClass(cn, cl), "isOplusVolumeKeyInRight", new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam p) {
                        if (mPosition == 0) return;
                        p.setResult(mPosition == 1); // 1=right, 2=left
                    }
                });
                XposedBridge.log("[ Obsidian ] VolumePanelMod: position hooked via " + cn);
                return;
            } catch (Throwable t) {
                XposedBridge.log("[ Obsidian ] VolumePanelMod: position skip " + cn + ": " + t);
            }
        }
    }

    // ── Timeout hook ──────────────────────────────────────────────────────────

    // OOS16 names the method computeTimeoutH$2 (compiler-generated variant of AOSP's
    // computeTimeoutH). hookAllMethods() uses exact name matching and misses $N variants.
    // We walk the full hierarchy ourselves, log every timeout-related method (DIAG), and
    // hook any that contains "computetimeout" in its name and returns int.

    private void hookTimeout(ClassLoader cl) {
        XC_MethodHook computeHook = new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                // computeTimeoutH$2 spara ad ogni apertura reale del pannello (verificato nel
                // log, a differenza di initRow che sembra scattare una volta sola molto presto
                // all'avvio — troppo presto se l'utente non tocca più l'interruttore dopo il
                // boot). Approfittiamone per riapplicare qui i colori ad ogni apertura, invece
                // di fidarci solo di initRow. p.thisObject è già l'OVDI reale.
                if (mCustomProgress || mCustomBg) applyColorsToAll(p.thisObject);

                int ms = (Xprefs != null) ? Xprefs.getInt(PREF_TIMEOUT, 3) * 1_000 : mTimeoutMs;
                if (ms == 3_000) return; // stock timeout — don't override
                // Accessibility hovering: use recommended 16s
                try {
                    if (getBooleanField(p.thisObject, "mHovering")) {
                        p.setResult(callMethod(getObjectField(p.thisObject, "mAccessibilityMgr"),
                                "getRecommendedTimeoutMillis", 16000, 4));
                        return;
                    }
                } catch (Throwable ignored) {}
                // Expanded panel or normal: our custom timeout (or accessibility 5s if expanded)
                try {
                    synchronized (getObjectField(p.thisObject, "mSafetyWarningLock")) {
                        if (getBooleanField(p.thisObject, "mExpanded")) {
                            p.setResult(callMethod(getObjectField(p.thisObject, "mAccessibilityMgr"),
                                    "getRecommendedTimeoutMillis", 5000, 4));
                        } else {
                            p.setResult(ms);
                        }
                    }
                } catch (Throwable t) {
                    p.setResult(ms); // fallback: just return our timeout
                    XposedBridge.log("[ Obsidian ] VolumePanelMod: computeTimeoutH fallback: " + t);
                }
            }
        };

        for (String cn : new String[]{
                "com.oplus.systemui.volume.OplusVolumeDialogImpl",
                "com.oplusos.systemui.volume.VolumeDialogImplEx",
                "com.android.systemui.volume.VolumeDialogImpl"}) {
            try {
                Class<?> cls = findClass(cn, cl);
                int n = 0;

                // Walk the full hierarchy: log all timeout-related methods (DIAG) and
                // hook any int-returning method whose name contains "computetimeout".
                // This catches computeTimeoutH, computeTimeoutH$2, and any future variant.
                for (Class<?> c = cls; c != null && c != Object.class; c = c.getSuperclass()) {
                    for (java.lang.reflect.Method m : c.getDeclaredMethods()) {
                        String lo = m.getName().toLowerCase();
                        if (lo.contains("timeout") || lo.contains("computetime")) {
                            boolean hook = lo.contains("computetimeout")
                                    && m.getReturnType() == int.class;
                            XposedBridge.log("[ Obsidian ] VolumePanelMod: DIAG "
                                    + c.getSimpleName() + "." + m.getName()
                                    + " [" + m.getReturnType().getSimpleName() + "]"
                                    + (hook ? " ← hooking" : ""));
                            if (hook) {
                                m.setAccessible(true);
                                XposedBridge.hookMethod(m, computeHook);
                                n++;
                            }
                        }
                    }
                }

                XposedBridge.log("[ Obsidian ] VolumePanelMod: timeout hooks=" + n + " via " + cn);
                if (n > 0) return;
            } catch (Throwable t) {
                XposedBridge.log("[ Obsidian ] VolumePanelMod: timeout skip " + cn + ": " + t);
            }
        }
    }


    // ── Slider color hooks ────────────────────────────────────────────────────

    private void hookColors(ClassLoader cl) {
        // Track OVDI instance via constructor + apply colors per-row via initRow
        for (String cn : new String[]{
                "com.oplus.systemui.volume.OplusVolumeDialogImpl",
                "com.oplusos.systemui.volume.VolumeDialogImplEx"}) {
            try {
                Class<?> cls = findClass(cn, cl);
                // Capture instance at construction time
                for (java.lang.reflect.Constructor<?> ctor : cls.getDeclaredConstructors()) {
                    XposedBridge.hookMethod(ctor, new XC_MethodHook() {
                        @Override protected void afterHookedMethod(MethodHookParam p) {
                            mOVDI = p.thisObject;
                        }
                    });
                }
                // Apply colors per-row after initRow (args[0] = VolumeRow on OOS15)
                hookAllMethods(cls, "initRow", new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam p) {
                        if (p.args.length > 0) applyColorsToRow(p.args[0]);
                    }
                });
                XposedBridge.log("[ Obsidian ] VolumePanelMod: color OVDI hook via " + cn);
                break;
            } catch (Throwable t) {
                XposedBridge.log("[ Obsidian ] VolumePanelMod: color OVDI skip " + cn + ": " + t);
            }
        }

        // OOS16: OplusVolumeRow.initRow — row object is thisObject, not args[0]
        try {
            Class<?> rowCls = findClass("com.oplus.systemui.volume.view.OplusVolumeRow", cl);
            hookAllMethods(rowCls, "initRow", new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    applyColorsToRow(p.thisObject);
                }
            });
            XposedBridge.log("[ Obsidian ] VolumePanelMod: color hooked via OplusVolumeRow");
        } catch (Throwable t) {
            XposedBridge.log("[ Obsidian ] VolumePanelMod: color skip OplusVolumeRow: " + t);
        }

        // Background blur color via onPreDraw
        try {
            Class<?> seekBar = findClass("com.oplus.systemui.volume.OplusVolumeSeekBar", cl);
            hookAllMethods(seekBar, "onPreDraw", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    if (!mCustomBg) return;
                    try {
                        Object blur = callMethod(p.thisObject, "getBackgroundBlurDrawable");
                        if (blur != null) callMethod(blur, "setColor", mBgColor);
                    } catch (Throwable ignored) {}
                }
            });
            XposedBridge.log("[ Obsidian ] VolumePanelMod: seekBar bg hooked");
        } catch (Throwable t) {
            XposedBridge.log("[ Obsidian ] VolumePanelMod: seekBar bg skip: " + t);
        }
    }

    /** Apply progress/bg colors to a single VolumeRow. */
    private void applyColorsToRow(Object volumeRow) {
        try {
            Object slider = getObjectField(volumeRow, "slider");
            XposedBridge.log("[ Obsidian ] VolumePanelMod: DIAG applyColorsToRow mCustomProgress=" + mCustomProgress
                    + " mProgressColor=#" + Integer.toHexString(mProgressColor)
                    + " mCustomBg=" + mCustomBg + " mBgColor=#" + Integer.toHexString(mBgColor));
            if (mCustomProgress) {
                try {
                    callMethod(slider, "setProgressColor", ColorStateList.valueOf(mProgressColor));
                    XposedBridge.log("[ Obsidian ] VolumePanelMod: DIAG setProgressColor OK");
                } catch (Throwable t) {
                    XposedBridge.log("[ Obsidian ] VolumePanelMod: DIAG setProgressColor FAILED: " + t);
                }
            }
            if (mCustomBg) {
                try {
                    callMethod(slider, "setSeekBarBackgroundColor", ColorStateList.valueOf(mBgColor));
                    XposedBridge.log("[ Obsidian ] VolumePanelMod: DIAG setSeekBarBackgroundColor OK");
                } catch (Throwable t) {
                    XposedBridge.log("[ Obsidian ] VolumePanelMod: DIAG setSeekBarBackgroundColor FAILED: " + t);
                }
            }
            applyIconColor(volumeRow);
        } catch (Throwable t) {
            XposedBridge.log("[ Obsidian ] VolumePanelMod: applyRow error: " + t);
        }
    }

    // ── Colore icona cursore — icon è una com.oplus.systemui.volume.OplusEffectiveAnimationView
    // extends com.oplus.anim.EffectiveAnimationView (fork OEM di Lottie), che disegna via un
    // proprio EffectiveAnimationDrawable interno: né setColorFilter né setImageTintList hanno
    // effetto (confermati entrambi via log — nessuna eccezione, nessun cambiamento visivo,
    // 2026-08-20 mattina), perché il Drawable dipinge ogni livello con i suoi paint interni,
    // ignorando il tint/filtro standard di ImageView.
    //
    // Il vero meccanismo (trovato nel sorgente decompilato di EffectiveAnimationView, riga che
    // applica l'attributo XML lottie_colorFilter all'inflate):
    //   effectiveDrawable.addValueCallback(new KeyPath("**"), EffectiveAnimationProperty.COLOR_FILTER,
    //           new EffectiveValueCallback(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)))
    // "**" è il KeyPath jolly standard di Lottie (tutti i livelli). addValueCallback mette in coda
    // la richiesta se la composizione non è ancora pronta (lazyCompositionTasks), quindi è sicuro
    // chiamarlo subito dopo initRow senza attese. Le classi sono nel fork OEM "com.oplus.anim.*",
    // non nella libreria Lottie originale "com.airbnb.lottie.*" (entrambe presenti nell'app,
    // package diversi — quella giusta è quella usata dalla classe reale della vista).
    private void applyIconColor(Object volumeRow) {
        if (mIconMode == 0) return; // predefinito, non toccare
        try {
            Object icon = callMethod(volumeRow, "getIcon");
            if (icon == null) return;
            int color = switch (mIconMode) {
                case 4 -> mIconColor;
                case 3 -> appAccentColor();
                case 2 -> 0xFFFFFFFF;
                default -> 0xFF404040; // scura
            };
            if (!applyLottieColorFilter(icon, color) && icon instanceof ImageView) {
                ((ImageView) icon).setImageTintList(ColorStateList.valueOf(color));
            }
        } catch (Throwable t) {
            XposedBridge.log("[ Obsidian ] VolumePanelMod: icon color failed: " + t);
        }
    }

    /** true se il colore è stato applicato via addValueCallback (percorso Lottie reale). */
    private boolean applyLottieColorFilter(Object iconView, int color) {
        try {
            ClassLoader cl = iconView.getClass().getClassLoader();
            Object drawable = getObjectField(iconView, "effectiveDrawable");
            if (drawable == null) return false;
            Class<?> keyPathCls = findClass("com.oplus.anim.model.KeyPath", cl);
            Class<?> valueCallbackCls = findClass("com.oplus.anim.value.EffectiveValueCallback", cl);
            Class<?> propertyCls = findClass("com.oplus.anim.EffectiveAnimationProperty", cl);
            Object keyPath = keyPathCls.getConstructor(String[].class)
                    .newInstance((Object) new String[]{"**"});
            Object colorFilterProperty = de.robv.android.xposed.XposedHelpers.getStaticObjectField(propertyCls, "COLOR_FILTER");
            android.graphics.PorterDuffColorFilter filter = new android.graphics.PorterDuffColorFilter(
                    color, android.graphics.PorterDuff.Mode.SRC_ATOP);
            Object valueCallback = valueCallbackCls.getConstructor(Object.class).newInstance(filter);
            callMethod(drawable, "addValueCallback", keyPath, colorFilterProperty, valueCallback);
            return true;
        } catch (Throwable t) {
            XposedBridge.log("[ Obsidian ] VolumePanelMod: Lottie colorFilter failed: " + t);
            return false;
        }
    }

    private int appAccentColor() {
        if (Xprefs.getBoolean("DST_ACCENT1_on", false)) {
            return Xprefs.getInt("DST_ACCENT1", 0xFF908DFF) | 0xFF000000;
        }
        return 0xFF908DFF;
    }

    /** Live-refresh colors on all currently visible rows. */
    private void applyColorsToAll() {
        if (mOVDI != null) applyColorsToAll(mOVDI);
    }

    @SuppressWarnings("unchecked")
    private void applyColorsToAll(Object ovdi) {
        if (ovdi == null) return;
        try {
            List<Object> rows = (List<Object>) getObjectField(ovdi, "mRows");
            for (Object row : rows) applyColorsToRow(row);
        } catch (Throwable t) {
            XposedBridge.log("[ Obsidian ] VolumePanelMod: applyColorsToAll error: " + t);
        }
    }

    @Override
    public boolean listensTo(String packageName) { return LISTEN.equals(packageName); }
}

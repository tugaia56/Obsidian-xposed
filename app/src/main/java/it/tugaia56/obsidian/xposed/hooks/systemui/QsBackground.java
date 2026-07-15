package it.tugaia56.obsidian.xposed.hooks.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static it.tugaia56.obsidian.utils.Constants.Packages.SYSTEM_UI;
import static it.tugaia56.obsidian.xposed.XPrefs.Xprefs;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import it.tugaia56.obsidian.xposed.XposedMods;

/**
 * Replaces the QS panel blur with a solid DST background color.
 *
 * Mirrors OC's QSTransparency.hookQs() which is confirmed working on OOS16:
 *  - ScrimControllerExImp constructor → store instance for isQsVisible()
 *  - ScrimViewExImp.setBlurAmount → zero blur when isBehind() + isQsVisible()
 *  - ScrimViewExImp.onDraw → draw solid color when isBehind() + isQsVisible()
 *  - ScrimViewExImp.setViewAlpha → force 1.0 when isBehind() + isQsVisible()
 *
 * isBehind() and isQsVisible() are confirmed to exist on OOS16
 * (OC uses the same calls and works).
 */
public class QsBackground extends XposedMods {

    private static final String PREF_ENABLED  = "DST_QS_BG_ENABLED";
    private static final String PREF_BG_COLOR = "DST_BACKGROUND";

    private boolean mEnabled   = false;
    private int     mBgColor   = 0xFF1A1A2E;
    private Object  mScrimCtrl = null;   // ScrimControllerExImp instance
    private View    mScrimView = null;   // behind ScrimViewExImp, for live invalidate

    public QsBackground(Context context) { super(context); }

    @Override
    public void updatePrefs(String... Key) {
        if (Xprefs == null) return;
        mEnabled = Xprefs.getBoolean(PREF_ENABLED, false);
        // If DST_BACKGROUND has never been set (value = 0 = transparent), fall back to the
        // default dark-navy colour so the user sees something reasonable.
        int stored = Xprefs.getInt(PREF_BG_COLOR, 0);
        mBgColor = (stored != 0) ? stored : 0xFF1A1A2E;
        if (mEnabled && mScrimView != null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                try { mScrimView.invalidate(); } catch (Throwable ignored) {}
            });
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) throws Throwable {
        if (!SYSTEM_UI.equals(lp.packageName)) return;

        // ── 1. Grab ScrimControllerExImp instance ─────────────────────────
        try {
            Class<?> ctrlClass = findClass(
                    "com.oplus.systemui.statusbar.phone.ScrimControllerExImp",
                    lp.classLoader);
            hookAllConstructors(ctrlClass, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    mScrimCtrl = p.thisObject;
                }
            });
        } catch (Throwable t) {
            // Class not found — hooks below won't draw but won't crash either
        }

        // ── 2. Hook ScrimViewExImp ─────────────────────────────────────────
        Class<?> viewClass;
        try {
            viewClass = findClass(
                    "com.oplus.systemui.scrim.ScrimViewExImp",
                    lp.classLoader);
        } catch (Throwable t) {
            return;
        }

        // Zero out blur when QS is open and this is the behind scrim
        hookAllMethods(viewClass, "setBlurAmount", new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (!mEnabled || mScrimCtrl == null) return;
                if (isBehind(p.thisObject) && isQsVisible()) {
                    p.args[0] = 0f;
                }
            }
        });

        // Draw solid color; also capture the behind scrim View for live redraw
        hookAllMethods(viewClass, "onDraw", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam p) {
                if (mScrimCtrl == null) return;
                boolean behind = isBehind(p.thisObject);
                if (behind) mScrimView = (View) p.thisObject;
                if (!mEnabled || !behind) return;
                if (isQsVisible()) {
                    ((android.graphics.Canvas) p.args[0]).drawColor(mBgColor);
                }
            }
        });

        // Force full opacity so the solid color isn't blended away
        try {
            hookAllMethods(viewClass, "setViewAlpha", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    if (!mEnabled || mScrimCtrl == null) return;
                    if (isBehind(p.thisObject) && isQsVisible()) {
                        p.args[0] = 1.0f;
                    }
                }
            });
        } catch (Throwable ignored) {}
    }

    @Override public boolean listensTo(String packageName) { return SYSTEM_UI.equals(packageName); }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private boolean isBehind(Object view) {
        try   { return (boolean) callMethod(view, "isBehind"); }
        catch (Throwable t1) {
            try { return getBooleanField(view, "isBehind"); }
            catch (Throwable t2) { return false; }
        }
    }

    private boolean isQsVisible() {
        if (mScrimCtrl == null) return false;
        try   { return (boolean) callMethod(mScrimCtrl, "isQsVisible"); }
        catch (Throwable t1) {
            try { return getBooleanField(mScrimCtrl, "isQsVisible"); }
            catch (Throwable t2) { return false; }
        }
    }
}

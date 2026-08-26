package it.tugaia56.obsidian.xposed.hooks.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static it.tugaia56.obsidian.utils.Constants.Packages.SYSTEM_UI;
import static it.tugaia56.obsidian.xposed.XPrefs.Xprefs;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import it.tugaia56.obsidian.xposed.XposedMods;

/**
 * Real OC mechanism (StatusbarMods padding slice): pads PhoneStatusBarView's
 * "status_bar_contents" child via setPaddingRelative(). Obsidian's UI exposes
 * a single symmetric side-padding value (not OC's dual-thumb start/end pair),
 * applied equally to both sides.
 */
public class StatusbarPadding extends XposedMods {

    private static final String PREF_ENABLED = "statusbar_padding_enabled";
    private static final String PREF_TOP     = "statusbar_top_padding";
    private static final String PREF_SIDE    = "statusbarPaddings";

    private boolean mEnabled = false;
    private float mTopPx = 0f;
    private int mSidePercent = 0;

    private Object mPhoneStatusBarView = null;
    private View mStatusBarContents = null;

    public StatusbarPadding(Context context) {
        super(context);
    }

    @Override
    public void updatePrefs(String... Key) {
        if (Xprefs == null) return;
        mEnabled = Xprefs.getBoolean(PREF_ENABLED, false);
        mTopPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                Xprefs.getSliderInt(PREF_TOP, 0),
                mContext.getResources().getDisplayMetrics());
        mSidePercent = Math.max(0, Xprefs.getSliderInt(PREF_SIDE, 0));

        if (Key.length > 0 && mPhoneStatusBarView != null) {
            try { callMethod(mPhoneStatusBarView, "updateStatusBarHeight"); }
            catch (Throwable ignored) {}
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) throws Throwable {
        if (!SYSTEM_UI.equals(lp.packageName)) return;

        Class<?> phoneStatusBarView = findClass(
                "com.android.systemui.statusbar.phone.PhoneStatusBarView", lp.classLoader);

        hookAllConstructors(phoneStatusBarView, new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                mPhoneStatusBarView = param.thisObject;
            }
        });

        hookAllMethods(phoneStatusBarView, "updateStatusBarHeight", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                try {
                    View v = (View) param.thisObject;
                    int id = mContext.getResources().getIdentifier(
                            "status_bar_contents", "id", SYSTEM_UI);
                    if (id == 0) return;
                    mStatusBarContents = v.findViewById(id);
                    if (mStatusBarContents == null || !mEnabled) return;

                    int screenWidth = mContext.getResources().getDisplayMetrics().widthPixels;
                    int sidePx = Math.round(mSidePercent / 100f * screenWidth);
                    mStatusBarContents.setPaddingRelative(sidePx, Math.round(mTopPx), sidePx, 0);
                } catch (Throwable ignored) {}
            }
        });

        hookAllMethods(phoneStatusBarView, "onConfigurationChanged", new XC_MethodHook() {
            @Override protected void afterHookedMethod(MethodHookParam param) {
                try { callMethod(mPhoneStatusBarView, "updateStatusBarHeight"); }
                catch (Throwable ignored) {}
            }
        });
    }

    @Override public boolean listensTo(String packageName) { return SYSTEM_UI.equals(packageName); }
}

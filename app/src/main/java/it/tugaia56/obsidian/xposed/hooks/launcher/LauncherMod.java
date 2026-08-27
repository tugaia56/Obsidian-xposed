package it.tugaia56.obsidian.xposed.hooks.launcher;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getStaticIntField;
import static it.tugaia56.obsidian.utils.Constants.Packages.LAUNCHER;
import static it.tugaia56.obsidian.xposed.XPrefs.Xprefs;

import android.content.Context;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import it.tugaia56.obsidian.xposed.XposedMods;

/**
 * Real OC Launcher.java mechanism, first slice ported: hide app labels on Home/Drawer
 * (com.android.launcher3.BubbleTextView.applyLabel — DISPLAY_ALL_APPS means Drawer,
 * anything else means Home). Rest of OC's Launcher.java (columns/rows, folder layout,
 * pagination, fast scroll, force-dock, open-app-details, replace-lock) is next, one at a
 * time so each can be tested in isolation.
 */
public class LauncherMod extends XposedMods {

    private static final String KEY_HIDE_DESKTOP_LABELS = "desktop_hide_app_labels";
    private static final String KEY_HIDE_DRAWER_LABELS   = "drawer_hide_app_labels";

    private boolean mHideDesktopLabels = false;
    private boolean mHideDrawerLabels  = false;

    public LauncherMod(Context context) {
        super(context);
    }

    @Override
    public void updatePrefs(String... key) {
        if (Xprefs == null) return;
        mHideDesktopLabels = Xprefs.getBoolean(KEY_HIDE_DESKTOP_LABELS, false);
        mHideDrawerLabels  = Xprefs.getBoolean(KEY_HIDE_DRAWER_LABELS, false);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        Class<?> bubbleTextView = lpparam.classLoader.loadClass("com.android.launcher3.BubbleTextView");

        findAndHookMethod(bubbleTextView, "applyLabel", CharSequence.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                int display = getIntField(param.thisObject, "mDisplay");
                int drawerDisplay = 1;
                try {
                    drawerDisplay = getStaticIntField(bubbleTextView, "DISPLAY_ALL_APPS");
                } catch (Throwable ignored) {}

                boolean hide = (display == drawerDisplay) ? mHideDrawerLabels : mHideDesktopLabels;
                if (hide) param.setResult(null);
            }
        });
    }

    @Override
    public boolean listensTo(String packageName) {
        return LAUNCHER.equals(packageName);
    }
}

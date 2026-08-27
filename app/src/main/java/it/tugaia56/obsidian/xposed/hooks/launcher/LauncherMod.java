package it.tugaia56.obsidian.xposed.hooks.launcher;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.getStaticIntField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;
import static it.tugaia56.obsidian.utils.Constants.Packages.LAUNCHER;
import static it.tugaia56.obsidian.xposed.XPrefs.Xprefs;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Process;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import it.tugaia56.obsidian.xposed.XposedMods;

/**
 * Real OC Launcher.java mechanism, ported one section at a time. So far: hide app labels
 * (Home/Drawer) and the full "Recenti" section — Apri Dettagli App (long-press),
 * Disabilita Pagina Recenti Precedente, Sostituisci Blocco (swipe-up-and-hold in Recents
 * kills the app instead of locking it). Rest of OC's Launcher.java (columns/rows, folder
 * layout, pagination, fast scroll, force-dock) is next, one at a time so each can be
 * tested in isolation.
 */
public class LauncherMod extends XposedMods {

    private static final String KEY_HIDE_DESKTOP_LABELS = "desktop_hide_app_labels";
    private static final String KEY_HIDE_DRAWER_LABELS   = "drawer_hide_app_labels";
    private static final String KEY_OPEN_APP_DETAILS     = "launcher_open_app_details";
    private static final String KEY_DISABLE_PREV_RECENTS = "disable_previous_recents";
    private static final String KEY_REPLACE_LOCK         = "replace_lock";

    private boolean mHideDesktopLabels = false;
    private boolean mHideDrawerLabels  = false;
    private boolean mOpenAppDetails       = false;
    private boolean mDisablePrevRecents   = false;
    private boolean mReplaceLock          = false;

    public LauncherMod(Context context) {
        super(context);
    }

    @Override
    public void updatePrefs(String... key) {
        if (Xprefs == null) return;
        mHideDesktopLabels   = Xprefs.getBoolean(KEY_HIDE_DESKTOP_LABELS, false);
        mHideDrawerLabels    = Xprefs.getBoolean(KEY_HIDE_DRAWER_LABELS, false);
        mOpenAppDetails      = Xprefs.getBoolean(KEY_OPEN_APP_DETAILS, false);
        mDisablePrevRecents  = Xprefs.getBoolean(KEY_DISABLE_PREV_RECENTS, false);
        mReplaceLock         = Xprefs.getBoolean(KEY_REPLACE_LOCK, false);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        hookHideLabels(lpparam);
        hookOpenAppDetails(lpparam);
        hookDisablePreviousRecents(lpparam);
        hookReplaceLock(lpparam);
    }

    // ── Nascondi Etichette (Home/Drawer) ────────────────────────────────────
    private void hookHideLabels(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> bubbleTextView = lpparam.classLoader.loadClass("com.android.launcher3.BubbleTextView");
            // hookAllMethods (not findAndHookMethod with a guessed signature) — applyLabel has
            // multiple overloads across OOS versions, same approach OC's ReflectedClass.before() uses.
            hookAllMethods(bubbleTextView, "applyLabel", new XC_MethodHook() {
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
        } catch (Throwable t) {
            log("hookHideLabels failed: " + t);
        }
    }

    // ── Apri Dettagli App (tieni premuto un'icona in Recenti/Dock) ──────────
    private void hookOpenAppDetails(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> oplusTaskViewImpl = findClass("com.android.quickstep.views.OplusTaskViewImpl", lpparam.classLoader);
            hookAllMethods(oplusTaskViewImpl, "setIcon", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        View headerView = (View) callMethod(param.thisObject, "getHeaderView");
                        View iconView = (View) callMethod(headerView, "getTaskIcon");
                        View titleView = (View) callMethod(headerView, "getTitleTv");
                        Object task = callMethod(param.thisObject, "getTask");
                        if (task == null) return;
                        Object key = getObjectField(task, "key");
                        if (key == null) return;
                        String pkgName = (String) callMethod(key, "getPackageName");
                        int userId = getIntField(key, "userId");
                        AppDetailsClickListener listener = new AppDetailsClickListener(pkgName, userId);
                        iconView.setOnLongClickListener(listener);
                        titleView.setOnLongClickListener(listener);
                    } catch (Throwable t) {
                        log("hookOpenAppDetails (recents card) failed: " + t);
                    }
                }
            });
        } catch (Throwable t) {
            log("OplusTaskViewImpl not found: " + t);
        }

        try {
            Class<?> dockIconView = findClass("com.oplus.quickstep.dock.DockIconView", lpparam.classLoader);
            hookAllMethods(dockIconView, "setIcon", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        Object task = callMethod(param.thisObject, "getTask");
                        if (task == null) return;
                        Object key = getObjectField(task, "key");
                        if (key == null) return;
                        String pkgName = (String) callMethod(key, "getPackageName");
                        int userId = getIntField(key, "userId");
                        View iconView = (View) param.thisObject;
                        iconView.setOnLongClickListener(new AppDetailsClickListener(pkgName, userId));
                    } catch (Throwable t) {
                        log("hookOpenAppDetails (dock icon) failed: " + t);
                    }
                }
            });
        } catch (Throwable t) {
            log("DockIconView not found: " + t);
        }
    }

    private class AppDetailsClickListener implements View.OnLongClickListener {
        final String pkgName;
        final int userId;

        AppDetailsClickListener(String pkgName, int userId) {
            this.pkgName = pkgName;
            this.userId = userId;
        }

        @Override
        public boolean onLongClick(View v) {
            if (!mOpenAppDetails) return false;
            Intent appDetails = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", pkgName, null));
            appDetails.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            appDetails.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            appDetails.putExtra("userId", userId);
            mContext.startActivity(appDetails);
            return true;
        }
    }

    // ── Disabilita Pagina Recenti Precedente ────────────────────────────────
    private void hookDisablePreviousRecents(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> appFeatureUtils = findClass("com.android.common.util.AppFeatureUtils", lpparam.classLoader);
            hookAllMethods(appFeatureUtils, "isSupportAutoFocusToNextPageInOverviewState", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (mDisablePrevRecents) param.setResult(false);
                }
            });
        } catch (Throwable t) {
            log("hookDisablePreviousRecents failed: " + t);
        }
    }

    // ── Sostituisci Blocco: lo swipe-up-e-tieni in Recenti chiude l'app invece di bloccarla ──
    private void hookReplaceLock(XC_LoadPackage.LoadPackageParam lpparam) {
        ClassLoader cl = lpparam.classLoader;

        try {
            Class<?> stackTouchCtrl = findClass(
                    "com.android.quickstep.uioverrides.touchcontrollers.OplusStackTaskViewTouchCtrl", cl);
            hookAllMethods(stackTouchCtrl, "onDragEnd", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!mReplaceLock) return;
                    try {
                        Object recentsView = getObjectField(param.thisObject, "mRecentsView");
                        Object orientationHandler = callMethod(recentsView, "getPagedOrientationHandler");
                        int rotation = (int) callMethod(orientationHandler, "getRotation");
                        float displacement = (float) getObjectField(param.thisObject, "mDisplacement");
                        boolean isRtl = (boolean) getObjectField(param.thisObject, "mIsRtl");

                        boolean shouldKill;
                        if (rotation == 1) {
                            shouldKill = isRtl ? (displacement > 100.0f) : (displacement < -100.0f);
                        } else if (rotation == 3) {
                            shouldKill = isRtl ? (displacement < -100.0f) : (displacement > 100.0f);
                        } else {
                            shouldKill = (displacement > 200.0f);
                        }
                        if (!shouldKill) return;

                        param.setResult(null);
                        Object taskView = getObjectField(param.thisObject, "mTaskBeingDragged");
                        if (taskView == null) return;
                        Object task = callMethod(taskView, "getTask");
                        if (task == null) return;
                        killTaskPackage(taskView, task);
                    } catch (Throwable t) {
                        log("hookReplaceLock (stack onDragEnd) failed: " + t);
                    }
                }
            });
        } catch (Throwable t) {
            log("OplusStackTaskViewTouchCtrl not found: " + t);
        }

        try {
            Class<?> taskViewTouchCtrl = findClass(
                    "com.android.quickstep.uioverrides.touchcontrollers.OplusTaskViewTouchControllerImpl", cl);
            hookAllMethods(taskViewTouchCtrl, "onDragEnd", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (!mReplaceLock) return;
                    try {
                        Object taskBeingDragged = getObjectField(param.thisObject, "mTaskBeingDragged");
                        setAdditionalInstanceField(taskBeingDragged, "mShouldKill", true);
                    } catch (Throwable t) {
                        log("hookReplaceLock (task onDragEnd) failed: " + t);
                    }
                }
            });
        } catch (Throwable t) {
            log("OplusTaskViewTouchControllerImpl not found: " + t);
        }

        XC_MethodHook killHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (!mReplaceLock) return;
                try {
                    Object taskView = param.args[0];
                    boolean shouldKill;
                    try {
                        shouldKill = (boolean) getAdditionalInstanceField(taskView, "mShouldKill");
                    } catch (Throwable ignored) {
                        shouldKill = false;
                    }
                    if (!shouldKill) return;
                    param.setResult(null);
                    Object task = callMethod(taskView, "getTask");
                    if (task == null) return;
                    killTaskPackage(taskView, task);
                } catch (Throwable t) {
                    log("hookReplaceLock (lock manager) failed: " + t);
                }
            }
        };
        try {
            Class<?> lockManager = findClass("com.oplus.quickstep.applock.OplusLockManager", cl);
            hookAllMethods(lockManager, "unLockForPullDown", killHook);
            hookAllMethods(lockManager, "lockForPullDown", killHook);
        } catch (Throwable t) {
            log("OplusLockManager not found: " + t);
        }
    }

    private void killTaskPackage(Object taskView, Object task) {
        String packageName;
        try {
            packageName = (String) callMethod(task, "getPackageName");
        } catch (Throwable t) {
            log("killTaskPackage: getPackageName failed: " + t);
            return;
        }

        try {
            int accessibilityCloseId = mContext.getResources()
                    .getIdentifier("accessibility_close", "string", LAUNCHER);
            callMethod(taskView, "performAccessibilityAction", accessibilityCloseId, null);
        } catch (Throwable t) {
            log("killTaskPackage: dismiss card failed: " + t);
        }
        Toast.makeText(mContext, "App Killed", Toast.LENGTH_SHORT).show();

        // Actual kill happens off the touch-handling thread — forceStopPackageAsUser needs
        // FORCE_STOP_PACKAGES, which this launcher's priv-app whitelist doesn't grant on every
        // ROM (confirmed via SecurityException on this device); su fallback covers that case.
        final String pkg = packageName;
        new Thread(() -> forceStopPackage(pkg)).start();
    }

    private void forceStopPackage(String packageName) {
        try {
            callMethod(mContext.getSystemService(Context.ACTIVITY_SERVICE),
                    "forceStopPackageAsUser",
                    packageName,
                    callMethod(Process.myUserHandle(), "getIdentifier"));
            return;
        } catch (Throwable ignored) {
            // Expected when the launcher doesn't hold FORCE_STOP_PACKAGES — fall through to su.
        }
        try {
            java.lang.Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "am force-stop " + packageName});
            p.waitFor();
        } catch (Throwable t) {
            log("forceStopPackage: su fallback failed: " + t);
        }
    }

    @Override
    public boolean listensTo(String packageName) {
        return LAUNCHER.equals(packageName);
    }
}

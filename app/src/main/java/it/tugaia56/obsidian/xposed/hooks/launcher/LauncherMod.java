package it.tugaia56.obsidian.xposed.hooks.launcher;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static de.robv.android.xposed.XposedHelpers.getStaticIntField;
import static de.robv.android.xposed.XposedHelpers.setAdditionalInstanceField;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.setBooleanField;
import static it.tugaia56.obsidian.utils.Constants.Packages.LAUNCHER;
import static it.tugaia56.obsidian.xposed.XPrefs.Xprefs;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageItemInfo;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Process;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import it.tugaia56.obsidian.xposed.XposedMods;
import it.tugaia56.obsidian.xposed.utils.GoogleMonochromeIconFactory;

/**
 * Real OC Launcher.java mechanism, ported one section at a time. So far: hide app labels
 * (Home/Drawer), the full "Recenti" section (Apri Dettagli App, Disabilita Pagina Recenti
 * Precedente, Sostituisci Blocco), Rimuovi Impaginazione (Home + Cartelle), Nascondi
 * Scroller, Comportamento Personalizzato Swipe Destro (Discover/Shelf panel), and Icone a
 * Tema (Forza/Alternativa monocroma via GoogleMonochromeIconFactory, ported verbatim from
 * OC, + Dove applicare per superficie). Rest of OC's Launcher.java (columns/rows,
 * folder/drawer rearrange, force-dock, Dock Background) is next, one at a time so each can
 * be tested in isolation.
 */
public class LauncherMod extends XposedMods {

    private static final String KEY_HIDE_DESKTOP_LABELS = "desktop_hide_app_labels";
    private static final String KEY_HIDE_DRAWER_LABELS   = "drawer_hide_app_labels";
    private static final String KEY_OPEN_APP_DETAILS     = "launcher_open_app_details";
    private static final String KEY_DISABLE_PREV_RECENTS = "disable_previous_recents";
    private static final String KEY_REPLACE_LOCK         = "replace_lock";
    private static final String KEY_REMOVE_HOME_PAGE     = "remove_home_pagination";
    private static final String KEY_REMOVE_FOLDER_PAGE   = "remove_folder_pagination";
    private static final String KEY_HIDE_SCROLLER        = "hide_scroller";
    private static final String KEY_SWIPE_RIGHT_ENABLED  = "launcher_custom_shelf_switch";
    private static final String KEY_SWIPE_RIGHT_MODE     = "laucher_shelf_custom"; // matches OC/UI exactly
    private static final String KEY_FORCE_THEMED_ICONS   = "force_themed_launcher_icons";
    private static final String KEY_ALT_MONOCHROME       = "alternative_monochrome";
    private static final String KEY_THEMED_ICONS_WHERE   = "custom_themed_icons_where"; // comma-separated indices, matches LauncherFragment's format
    private static final String KEY_CUSTOM_ICON_MAP_ENABLED = "themed_icons_where_enabled";

    private static final int DISPLAY_WORKSPACE           = 0;
    private static final int DISPLAY_ALL_APPS            = 1;
    private static final int DISPLAY_FOLDER              = 2;
    private static final int DISPLAY_TASKBAR              = 5;
    private static final int DISPLAY_SEARCH_RESULT       = 6;
    private static final int DISPLAY_SEARCH_RESULT_SMALL = 7;

    private boolean mHideDesktopLabels = false;
    private boolean mHideDrawerLabels  = false;
    private boolean mOpenAppDetails       = false;
    private boolean mDisablePrevRecents   = false;
    private boolean mReplaceLock          = false;
    private boolean mRemoveHomePagination   = false;
    private boolean mRemoveFolderPagination = false;
    private boolean mHideScroller           = false;
    private boolean mCustomShelfBehavior    = false;
    private int mShelfBehavior              = 2; // SHELF_STOCK — matches default when disabled
    private boolean mForceThemedIcons  = false;
    private boolean mAlternativeMono   = false;
    private boolean mAllowCustomIconMap = false;
    private boolean mWorkspaceMonochrome = true;
    private boolean mDrawerMonochrome    = true;
    private boolean mFolderMonochrome    = true;
    private boolean mSearchMonochrome    = true;
    private boolean mTaskbarMonochrome   = true;

    private View mFastScrollView;
    private int mIconBitmapSize;

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
        mRemoveHomePagination   = Xprefs.getBoolean(KEY_REMOVE_HOME_PAGE, false);
        mRemoveFolderPagination = Xprefs.getBoolean(KEY_REMOVE_FOLDER_PAGE, false);
        mHideScroller           = Xprefs.getBoolean(KEY_HIDE_SCROLLER, false);
        mCustomShelfBehavior    = Xprefs.getBoolean(KEY_SWIPE_RIGHT_ENABLED, false);
        mShelfBehavior          = Xprefs.getInt(KEY_SWIPE_RIGHT_MODE, 2);

        mForceThemedIcons = Xprefs.getBoolean(KEY_FORCE_THEMED_ICONS, false);
        mAlternativeMono  = Xprefs.getBoolean(KEY_ALT_MONOCHROME, false);
        mAllowCustomIconMap = Xprefs.getBoolean(KEY_CUSTOM_ICON_MAP_ENABLED, false);
        Set<String> where = parseThemedIconsWhere(Xprefs.getString(KEY_THEMED_ICONS_WHERE, "0,1,2,3,4"));
        mWorkspaceMonochrome = where.contains("0");
        mDrawerMonochrome    = where.contains("1");
        mFolderMonochrome    = where.contains("2");
        mSearchMonochrome    = where.contains("3");
        mTaskbarMonochrome   = where.contains("4");

        if (key.length > 0 && KEY_HIDE_SCROLLER.equals(key[0])) {
            updateFastScroll();
        }
        // Icone a Tema does NOT force a live mass-refresh here on purpose — forcing every icon
        // on screen to regenerate its monochrome bitmap at once froze the whole Launcher on
        // test (main thread blocked long enough that widgets/search/drawer all went blank).
        // Same rule as every other Launcher mod: enable, then reboot.
    }

    private static Set<String> parseThemedIconsWhere(String stored) {
        Set<String> result = new HashSet<>();
        for (String s : stored.split(",")) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) result.add(trimmed);
        }
        return result;
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        hookHideLabels(lpparam);
        hookOpenAppDetails(lpparam);
        hookDisablePreviousRecents(lpparam);
        hookReplaceLock(lpparam);
        hookRemovePagination(lpparam);
        hookHideScroller(lpparam);
        hookSwipeRightBehavior(lpparam);
        hookThemedIcons(lpparam);
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

    // ── Rimuovi Impaginazione (Home + Cartelle, stesso hook per entrambe come in OC) ──────
    private void hookRemovePagination(XC_LoadPackage.LoadPackageParam lpparam) {
        XC_MethodHook pageIndicatorHook = new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) {
                if (!mRemoveHomePagination && !mRemoveFolderPagination) return;
                View v = (View) param.thisObject;
                if (v.getParent() == null) return;
                String parentClass = v.getParent().getClass().getCanonicalName();
                if (parentClass == null) return;
                if (parentClass.equals("com.android.launcher3.OplusDragLayer")) {
                    if (mRemoveHomePagination) {
                        v.setVisibility(View.GONE);
                        param.setResult(null);
                    }
                } else if (parentClass.equals("android.widget.FrameLayout")) {
                    if (mRemoveFolderPagination) {
                        v.setVisibility(View.GONE);
                        param.setResult(null);
                    }
                }
            }
        };
        try {
            Class<?> pageIndicator = findClass("com.android.launcher.pageindicators.OplusPageIndicator", lpparam.classLoader);
            hookAllMethods(pageIndicator, "dispatchDraw", pageIndicatorHook);
            hookAllMethods(pageIndicator, "onDraw", pageIndicatorHook);
        } catch (Throwable t) {
            log("OplusPageIndicator not found: " + t);
        }

        try {
            Class<?> touchHelper = findClass("com.android.launcher.pageindicators.PageIndicatorTouchHelper", lpparam.classLoader);
            hookAllMethods(touchHelper, "dispatchTouchEvent", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (mRemoveHomePagination) param.setResult(false);
                }
            });
        } catch (Throwable t) {
            log("PageIndicatorTouchHelper not found: " + t);
        }
    }

    // ── Nascondi Scroller (barra a lettere del Drawer) ──────────────────────
    private void hookHideScroller(XC_LoadPackage.LoadPackageParam lpparam) {
        // Class name changed across OOS versions — try both, whichever exists on this ROM.
        String[] candidates = {
                "com.android.launcher3.allapps.OplusFastScrollLayout", // OOS 14-15
                "com.android.launcher3.allapps.LetterIndexFastScrollHelper" // OOS 13
        };
        for (String className : candidates) {
            try {
                Class<?> fastScrollClass = findClass(className, lpparam.classLoader);
                hookAllConstructors(fastScrollClass, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        try {
                            if (className.endsWith("LetterIndexFastScrollHelper")) {
                                mFastScrollView = (View) getObjectField(param.thisObject, "mFastScroll");
                            } else {
                                mFastScrollView = (View) param.thisObject;
                            }
                            updateFastScroll();
                        } catch (Throwable t) {
                            log("hookHideScroller construction failed: " + t);
                        }
                    }
                });
                return; // found one candidate, no need to try the other
            } catch (Throwable ignored) {
                // try next candidate
            }
        }
        log("No fast-scroll class found on this ROM — Nascondi Scroller has no effect here");
    }

    private void updateFastScroll() {
        if (mFastScrollView == null) return;
        mFastScrollView.setVisibility(mHideScroller ? View.GONE : View.VISIBLE);
    }

    // ── Comportamento Personalizzato Swipe Destro (pannello Discover/Shelf) ─────────────────
    private void hookSwipeRightBehavior(XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            Class<?> overlayProxy = findClass("com.android.overlay.OverlayProxy", lpparam.classLoader);
            hookAllMethods(overlayProxy, "getAssistScreenType", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (mCustomShelfBehavior) param.setResult(mShelfBehavior);
                }
            });
            hookAllMethods(overlayProxy, "setAssistScreenType", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (mCustomShelfBehavior) param.args[0] = mShelfBehavior;
                }
            });
        } catch (Throwable t) {
            log("OverlayProxy not found: " + t);
        }

        try {
            Class<?> featureOption = findClass("com.android.common.config.FeatureOption", lpparam.classLoader);
            hookAllMethods(featureOption, "getSShelfAssistantEnable", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (mCustomShelfBehavior) param.setResult(mShelfBehavior == 0);
                }
            });
            hookAllMethods(featureOption, "updateSupportShelfAssistant", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (mCustomShelfBehavior) setBooleanField(param.thisObject, "sShelfAssistantEnable", mShelfBehavior == 0);
                }
            });
        } catch (Throwable t) {
            log("FeatureOption not found: " + t);
        }
    }

    // ── Icone a Tema (monocrome, stile Material You) ────────────────────────────────────────
    private void hookThemedIcons(XC_LoadPackage.LoadPackageParam lpparam) {
        ClassLoader cl = lpparam.classLoader;

        // Forza Icone a Tema / Alternativa — genera un'icona monocroma anche per le app che
        // non la supportano nativamente, invece di lasciare l'icona a colori originale.
        try {
            Class<?> baseIconFactory = findClass("com.android.launcher3.icons.BaseIconFactory", cl);
            hookAllConstructors(baseIconFactory, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        mIconBitmapSize = getIntField(param.thisObject, "mIconBitmapSize");
                    } catch (Throwable ignored) {}
                }
            });
        } catch (Throwable t) {
            log("BaseIconFactory not found: " + t);
        }

        try {
            Class<?> uxIconLoaderHelper = findClass("com.oplus.uxicon.ui.util.UxIconLoaderHelper", cl);
            hookAllMethods(uxIconLoaderHelper, "getIconThemeDrawable", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        if (!mForceThemedIcons || !mAlternativeMono || param.getResult() != null) return;
                        if (mIconBitmapSize <= 0) return; // BaseIconFactory hasn't run yet on this thread
                        PackageItemInfo info = (PackageItemInfo) param.args[1];
                        Drawable icon = info.loadIcon(mContext.getPackageManager());
                        param.setResult(new GoogleMonochromeIconFactory(icon, mIconBitmapSize));
                    } catch (Throwable t) {
                        log("hookThemedIcons (getIconThemeDrawable fallback) failed: " + t);
                    }
                }
            });
        } catch (Throwable t) {
            log("UxIconLoaderHelper not found: " + t);
        }

        hookAllMethods(AdaptiveIconDrawable.class, "getMonochrome", new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    if (param.getResult() != null || !mForceThemedIcons || mAlternativeMono) return;
                    if (mIconBitmapSize <= 0) return; // BaseIconFactory hasn't run yet on this thread
                    // Skip if this call came from IconProvider.getIconWithOverrides — monochrome
                    // is already handled there, forcing it again here would double up.
                    StackTraceElement[] trace = new Throwable().getStackTrace();
                    if (trace.length > 4 && trace[4].getMethodName().toLowerCase().contains("override")) return;

                    GoogleMonochromeIconFactory mono = (GoogleMonochromeIconFactory)
                            getAdditionalInstanceField(param.thisObject, "mMonoFactoryObsidian");
                    if (mono == null) {
                        mono = new GoogleMonochromeIconFactory((AdaptiveIconDrawable) param.thisObject, mIconBitmapSize);
                        setAdditionalInstanceField(param.thisObject, "mMonoFactoryObsidian", mono);
                    }
                    param.setResult(mono);
                } catch (Throwable ignored) {}
            }
        });

        // Dove applicare le icone a tema — indipendente da Forza/Alternativa, controlla solo se
        // il tema (quando già disponibile nativamente) viene applicato in ciascuna superficie.
        // Gated by "Mappa Personalizzata" (mAllowCustomIconMap) — off by default, matching OC.
        try {
            Class<?> bubbleTextView = findClass("com.android.launcher3.OplusBubbleTextView", cl);
            hookAllMethods(bubbleTextView, "applyIconAndLabel", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    try {
                        if (!mAllowCustomIconMap) return;
                        Object itemInfoWithIcon = param.args[0];
                        if (itemInfoWithIcon == null) return;
                        boolean alreadyEdited = getBooleanField(itemInfoWithIcon, "mIsIconEdited");
                        if (alreadyEdited) return;
                        int display = getIntField(param.thisObject, "mDisplay");
                        Object newIcon = callMethod(itemInfoWithIcon, "newIcon", mContext, shouldUseTheme(display) ? 1 : 0);
                        callMethod(param.thisObject, "setIcon", newIcon);
                    } catch (Throwable t) {
                        log("hookThemedIcons (applyIconAndLabel) failed: " + t);
                    }
                }
            });
        } catch (Throwable t) {
            log("OplusBubbleTextView not found: " + t);
        }

        // Anteprime nelle cartelle
        try {
            Class<?> previewItemManager = findClass("com.android.launcher3.folder.PreviewItemManager", cl);
            hookAllMethods(previewItemManager, "getNewIcon", new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    param.args[1] = mWorkspaceMonochrome ? 1 : 0;
                }
            });
        } catch (Throwable t) {
            log("PreviewItemManager not found: " + t);
        }
    }

    private boolean shouldUseTheme(int display) {
        switch (display) {
            case DISPLAY_ALL_APPS: return mDrawerMonochrome;
            case DISPLAY_FOLDER: return mFolderMonochrome;
            case DISPLAY_SEARCH_RESULT:
            case DISPLAY_SEARCH_RESULT_SMALL: return mSearchMonochrome;
            case DISPLAY_TASKBAR: return mTaskbarMonochrome;
            default: return mWorkspaceMonochrome;
        }
    }

    @Override
    public boolean listensTo(String packageName) {
        return LAUNCHER.equals(packageName);
    }
}

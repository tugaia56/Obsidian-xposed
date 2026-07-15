package it.tugaia56.obsidian.xposed.hooks.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllConstructors;
import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedBridge.hookMethod;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static it.tugaia56.obsidian.utils.Constants.Packages.SYSTEM_UI;
import static it.tugaia56.obsidian.xposed.XPrefs.Xprefs;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.GestureDetector;
import android.view.MotionEvent;

import java.lang.reflect.Method;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import it.tugaia56.obsidian.xposed.XposedMods;

/**
 * Status bar icon mods:
 *  - hide_bluetooth_when_disconnected
 *  - hide_inout_wifi
 *  - hide_inout_mobile
 *  - double_tap_sleep_statusbar
 */
public class StatusbarIcons extends XposedMods {

    private boolean mHideBluetooth      = false;
    private boolean mHideWifiActivity   = false;
    private boolean mHideMobileActivity = false;
    private boolean mDoubleTapToSleep   = false;

    private Object mNotifPanelVC = null;
    private GestureDetector mDtSleepDetector;

    public StatusbarIcons(Context context) { super(context); }

    @Override
    public void updatePrefs(String... Key) {
        if (Xprefs == null) return;
        mHideBluetooth      = Xprefs.getBoolean("hide_bluetooth_when_disconnected", false);
        mHideWifiActivity   = Xprefs.getBoolean("hide_inout_wifi",                  false);
        mHideMobileActivity = Xprefs.getBoolean("hide_inout_mobile",                false);
        mDoubleTapToSleep   = Xprefs.getBoolean("double_tap_sleep_statusbar",       false);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) throws Throwable {
        if (!SYSTEM_UI.equals(lp.packageName)) return;

        // GestureDetector must use the main Looper — handleLoadPackage is called from
        // a background thread (waitForXprefsLoad), so Looper.myLooper() would be null
        // and new GestureDetector(ctx, listener) would throw RuntimeException, silently
        // skipping ALL hooks in this file.
        mDtSleepDetector = new GestureDetector(mContext,
                new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        if (!mDoubleTapToSleep) return false;
                        try {
                            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
                            callMethod(pm, "goToSleep", SystemClock.uptimeMillis());
                        } catch (Throwable ignored) {}
                        return true;
                    }
                }, new Handler(Looper.getMainLooper()));

        try { hookBluetooth(lp);       } catch (Throwable t) { log("[ Obsidian ] StatusbarIcons BT hook failed: " + t); }
        try { hookWifiActivity(lp);    } catch (Throwable t) { log("[ Obsidian ] StatusbarIcons WiFi hook failed: " + t); }
        try { hookMobileActivity(lp);  } catch (Throwable t) { log("[ Obsidian ] StatusbarIcons Mobile hook failed: " + t); }
        try { hookDoubleTapToSleep(lp);} catch (Throwable t) { log("[ Obsidian ] StatusbarIcons DT hook failed: " + t); }
    }

    // ── Bluetooth ──────────────────────────────────────────────────────────────

    private void hookBluetooth(XC_LoadPackage.LoadPackageParam lp) {
        Class<?> cls = tryFindClass(lp,
                "com.oplus.systemui.statusbar.phone.OplusPhoneStatusBarPolicyExImpl", // OOS 15-14
                "com.oplusos.systemui.statusbar.phone.PhoneStatusBarPolicyEx");        // OOS 13
        if (cls == null) return;

        hookAllMethods(cls, "updateBluetoothIcon", new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (!mHideBluetooth) return;
                try {
                    boolean enabled = (boolean) p.args[3];
                    if (!enabled) return;
                    Object btCtrl = getObjectField(p.thisObject,
                            Build.VERSION.SDK_INT >= 34 ? "bluetoothController" : "mBluetooth");
                    boolean connected = (boolean) callMethod(btCtrl, "isBluetoothConnected");
                    if (!connected) p.setResult(null);
                } catch (Throwable t) {
                    log("[ Obsidian ] StatusbarIcons BT: " + t.getMessage());
                }
            }
        });
    }

    // ── WiFi activity arrows ───────────────────────────────────────────────────

    private void hookWifiActivity(XC_LoadPackage.LoadPackageParam lp) {
        Class<?> cls = tryFindClass(lp,
                "com.oplus.systemui.statusbar.pipeline.OplusWifiSignalExImpl",                    // OOS 15 (A16)
                "com.oplus.systemui.statusbar.phone.signal.OplusStatusBarSignalPolicyExImpl",     // OOS 14
                "com.oplusos.systemui.statusbar.phone.StatusBarSignalPolicyEx");                  // OOS 13
        if (cls == null) return;

        if (Build.VERSION.SDK_INT >= 35) {
            XC_MethodHook actHook = new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    if (mHideWifiActivity && p.args.length > 1) p.args[1] = 0;
                }
            };
            // Try exact Kotlin-lambda method name (OOS15 / A15)
            int hooked = hookAllMethods(cls, "bindEx$updateActivityIcon", actHook).size();
            if (hooked == 0) {
                // OOS16 may rename the lambda — scan all methods containing "ActivityIcon"
                for (Method m : cls.getDeclaredMethods()) {
                    if (m.getName().contains("ActivityIcon") && m.getParameterCount() >= 2) {
                        hookMethod(m, actHook);
                        hooked++;
                    }
                }
            }
            if (hooked == 0) {
                log("[ Obsidian ] StatusbarIcons: no WiFi ActivityIcon method found in " + cls.getName());
            }
        } else {
            hookAllMethods(cls, "getWifiActivityId", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    if (mHideWifiActivity) p.setResult(0);
                }
            });
        }
    }

    // ── Mobile data activity arrows ────────────────────────────────────────────

    private void hookMobileActivity(XC_LoadPackage.LoadPackageParam lp) {
        Class<?> cls = tryFindClass(lp,
                "com.oplus.systemui.statusbar.pipeline.mobile.ui.view.OplusStatusBarMobileViewBinder", // OOS 15 (A16)
                "com.oplus.systemui.statusbar.phone.signal.OplusStatusBarMobileViewExImpl");            // OOS 14-13
        if (cls == null) return;

        if (Build.VERSION.SDK_INT >= 35) {
            XC_MethodHook dataHook = new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    if (mHideMobileActivity && p.args.length > 1) p.args[1] = 0;
                }
            };
            // Try exact Kotlin-lambda method name (OOS15 / A15)
            int hooked = hookAllMethods(cls, "bindCustEx$updateDataActivity", dataHook).size();
            if (hooked == 0) {
                // OOS16 may rename the lambda — scan all methods containing "DataActivity"
                for (Method m : cls.getDeclaredMethods()) {
                    if (m.getName().contains("DataActivity") && m.getParameterCount() >= 2) {
                        hookMethod(m, dataHook);
                        hooked++;
                    }
                }
            }
            if (hooked == 0) {
                log("[ Obsidian ] StatusbarIcons: no Mobile DataActivity method found in " + cls.getName());
            }
        } else {
            hookAllMethods(cls, "updateState", new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    if (!mHideMobileActivity) return;
                    try {
                        android.widget.ImageView mDataActivity =
                                (android.widget.ImageView) getObjectField(p.thisObject, "mDataActivity");
                        mDataActivity.setVisibility(android.view.View.GONE);
                    } catch (Throwable ignored) {}
                }
            });
        }
    }

    // ── Double-tap status bar to sleep ─────────────────────────────────────────

    private void hookDoubleTapToSleep(XC_LoadPackage.LoadPackageParam lp) {
        Class<?> npvcClass = tryFindClass(lp,
                "com.android.systemui.shade.NotificationPanelViewController",
                "com.android.systemui.statusbar.phone.NotificationPanelViewController");
        if (npvcClass != null) {
            hookAllConstructors(npvcClass, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    mNotifPanelVC = p.thisObject;
                }
            });
        }

        Class<?> psbvcClass = tryFindClass(lp,
                "com.android.systemui.statusbar.phone.PhoneStatusBarViewController");
        if (psbvcClass == null) {
            log("[ Obsidian ] StatusbarIcons: PhoneStatusBarViewController not found");
            return;
        }

        XC_MethodHook touchHook = new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam p) {
                if (!mDoubleTapToSleep) return;
                try {
                    if (mNotifPanelVC != null) {
                        boolean pulsing   = (boolean) getObjectField(mNotifPanelVC, "mPulsing");
                        boolean dozing    = (boolean) getObjectField(mNotifPanelVC, "mDozing");
                        boolean collapsed = (boolean) callMethod(mNotifPanelVC, "isFullyCollapsed");
                        if (pulsing || dozing || !collapsed) return;
                    }
                    MotionEvent ev = (MotionEvent) p.args[p.args.length - 1];
                    mDtSleepDetector.onTouchEvent(ev);
                } catch (Throwable ignored) {}
            }
        };
        hookAllMethods(psbvcClass, "onTouch",         touchHook);
        hookAllMethods(psbvcClass, "handleTouchEvent", touchHook);
    }

    // ── Utility ────────────────────────────────────────────────────────────────

    private Class<?> tryFindClass(XC_LoadPackage.LoadPackageParam lp, String... names) {
        for (String name : names) {
            try { return findClass(name, lp.classLoader); }
            catch (Throwable ignored) {}
        }
        log("[ Obsidian ] StatusbarIcons: none of " + java.util.Arrays.toString(names) + " found");
        return null;
    }

    @Override public boolean listensTo(String packageName) { return SYSTEM_UI.equals(packageName); }
}

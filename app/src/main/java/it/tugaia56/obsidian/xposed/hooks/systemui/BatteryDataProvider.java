package it.tugaia56.obsidian.xposed.hooks.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getBooleanField;
import static de.robv.android.xposed.XposedHelpers.getIntField;

import android.annotation.SuppressLint;
import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import it.tugaia56.obsidian.utils.Constants;
import it.tugaia56.obsidian.xposed.XposedMods;

public class BatteryDataProvider extends XposedMods {

    private static final String listenPackage = Constants.Packages.SYSTEM_UI;

    @SuppressLint("StaticFieldLeak")
    private static BatteryDataProvider instance = null;

    private final List<BatteryInfoCallback> mInfoCallbacks = new ArrayList<>();
    private boolean mCharging = false;
    private int mCurrentLevel = 0;
    private boolean mPowerSave = false;

    public BatteryDataProvider(Context context) {
        super(context);
        instance = this;
    }

    // ── Static accessors ───────────────────────────────────────────────────────

    public static void registerInfoCallback(BatteryInfoCallback callback) {
        if (instance != null) instance.mInfoCallbacks.add(callback);
    }

    public static void unregisterInfoCallback(BatteryInfoCallback callback) {
        if (instance != null) instance.mInfoCallbacks.remove(callback);
    }

    public static boolean isCharging() {
        return instance != null && instance.mCharging;
    }

    public static int getCurrentLevel() {
        return instance != null ? instance.mCurrentLevel : 0;
    }

    public static boolean isPowerSaving() {
        return instance != null && instance.mPowerSave;
    }

    // ── XposedMods ─────────────────────────────────────────────────────────────

    @Override
    public void updatePrefs(String... Key) { /* no prefs */ }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        Class<?> BatteryControllerImpl = findClass(
                "com.android.systemui.statusbar.policy.BatteryControllerImpl",
                lpparam.classLoader);

        XC_MethodHook batteryHook = new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try { mCurrentLevel = getIntField(param.thisObject, "mLevel"); } catch (Throwable ignored) {}
                try {
                    mCharging = getBooleanField(param.thisObject, "mPluggedIn")
                             || getBooleanField(param.thisObject, "mCharging")
                             || getBooleanField(param.thisObject, "mWirelessCharging");
                } catch (Throwable ignored) {}
                try { mPowerSave = getBooleanField(param.thisObject, "mPowerSave"); } catch (Throwable ignored) {}
                dispatchInfo();
            }
        };

        hookAllMethods(BatteryControllerImpl, "fireBatteryLevelChanged", batteryHook);
        hookAllMethods(BatteryControllerImpl, "firePowerSaveChanged",   batteryHook);
        hookAllMethods(BatteryControllerImpl, "setPowerSave",           batteryHook);
    }

    @Override
    public boolean listensTo(String packageName) {
        return listenPackage.equals(packageName);
    }

    private void dispatchInfo() {
        for (BatteryInfoCallback cb : new ArrayList<>(mInfoCallbacks)) {
            try { cb.onBatteryInfoChanged(); } catch (Throwable ignored) {}
        }
    }

    public interface BatteryInfoCallback {
        void onBatteryInfoChanged();
    }
}

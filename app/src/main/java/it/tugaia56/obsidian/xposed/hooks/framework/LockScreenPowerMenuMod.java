package it.tugaia56.obsidian.xposed.hooks.framework;

import static de.robv.android.xposed.XposedBridge.hookMethod;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findMethodExact;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;
import static it.tugaia56.obsidian.utils.Constants.Packages.FRAMEWORK;
import static it.tugaia56.obsidian.xposed.XPrefs.Xprefs;

import android.content.Context;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import it.tugaia56.obsidian.xposed.XposedMods;

/**
 * lockscreen_hide_power_menu: suppresses the power/global-actions menu while the
 * device is on a secure lock screen (mirrors OC's disable_power_on_lockscreen).
 */
public class LockScreenPowerMenuMod extends XposedMods {

    private boolean mHidePowerMenu = false;

    public LockScreenPowerMenuMod(Context context) { super(context); }

    @Override
    public void updatePrefs(String... Key) {
        if (Xprefs == null) return;
        mHidePowerMenu = Xprefs.getBoolean("lockscreen_hide_power_menu", false);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) throws Throwable {
        if (!FRAMEWORK.equals(lp.packageName)) return;
        Class<?> extImpl;
        try {
            extImpl = findClass("com.android.server.policy.PhoneWindowManagerExtImpl", lp.classLoader);
            XposedBridge.log("[ Obsidian ] LockScreenPowerMenuMod: found PhoneWindowManagerExtImpl");
        } catch (Throwable t) {
            XposedBridge.log("[ Obsidian ] LockScreenPowerMenuMod: PhoneWindowManagerExtImpl not found: " + t);
            return;
        }

        try {
            hookMethod(findMethodExact(extImpl, "overrideShowGlobalActionsInternal"), new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    XposedBridge.log("[ Obsidian ] LockScreenPowerMenuMod: overrideShowGlobalActionsInternal FIRED, mHidePowerMenu=" + mHidePowerMenu);
                    if (!mHidePowerMenu) return;
                    try {
                        Object base = getObjectField(p.thisObject, "mBase");
                        if (base == null) { XposedBridge.log("[ Obsidian ] LockScreenPowerMenuMod: mBase is null"); return; }
                        int userId = getIntField(p.thisObject, "mCurrentUserId");
                        boolean keyguardOn = (boolean) callMethod(base, "keyguardOn");
                        boolean keyguardSecure = (boolean) callMethod(base, "isKeyguardSecure", userId);
                        XposedBridge.log("[ Obsidian ] LockScreenPowerMenuMod: keyguardOn=" + keyguardOn + " keyguardSecure=" + keyguardSecure);
                        if (keyguardOn && keyguardSecure) p.setResult(null);
                    } catch (Throwable t) {
                        XposedBridge.log("[ Obsidian ] LockScreenPowerMenuMod: field/method access failed: " + t);
                    }
                }
            });
            XposedBridge.log("[ Obsidian ] LockScreenPowerMenuMod: hooked overrideShowGlobalActionsInternal");
        } catch (Throwable t) {
            XposedBridge.log("[ Obsidian ] LockScreenPowerMenuMod: overrideShowGlobalActionsInternal not found: " + t);
            StringBuilder sb = new StringBuilder();
            for (java.lang.reflect.Method m : extImpl.getDeclaredMethods()) {
                if (m.getName().toLowerCase().contains("global") || m.getName().toLowerCase().contains("power")) {
                    sb.append(m.getName()).append('(').append(m.getParameterCount()).append(") ");
                }
            }
            XposedBridge.log("[ Obsidian ] LockScreenPowerMenuMod: candidate methods on ExtImpl: " + sb);
        }
    }

    @Override public boolean listensTo(String packageName) { return FRAMEWORK.equals(packageName); }
}

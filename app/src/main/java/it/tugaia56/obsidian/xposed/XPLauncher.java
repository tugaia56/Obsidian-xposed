package it.tugaia56.obsidian.xposed;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static it.tugaia56.obsidian.xposed.XPrefs.Xprefs;

import android.annotation.SuppressLint;
import android.app.Instrumentation;
import android.content.Context;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import it.tugaia56.obsidian.BuildConfig;
import it.tugaia56.obsidian.utils.Constants;
import it.tugaia56.obsidian.xposed.hooks.framework.DstCpbStyle;
import it.tugaia56.obsidian.xposed.hooks.framework.DstDialogStyle;

public class XPLauncher {

    public static ArrayList<XposedMods> runningMods = new ArrayList<>();
    @SuppressLint("StaticFieldLeak") public static Context mContext = null;

    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        log("[ Obsidian ] handleLoadPackage: " + lpparam.packageName);
        Log.d("ObsidianXP", "handleLoadPackage: " + lpparam.packageName);

        if (lpparam.packageName.equals(Constants.Packages.FRAMEWORK)) {
            hookFramework(lpparam);
        } else {
            hookOtherPackage(lpparam);
        }
    }

    private void hookFramework(XC_LoadPackage.LoadPackageParam lpparam) {
        // Install Dialog style hook in every process — handleLoadPackage("android") fires in
        // each process that loads the android framework, which is every app process.
        DstDialogStyle.hookDialogInProcess();

        boolean hooked = false;
        try {
            Class<?> PWM = lpparam.classLoader.loadClass("com.android.server.policy.PhoneWindowManager");
            hookAllMethods(PWM, "init", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam p) {
                    if (mContext == null && p.args[0] instanceof Context) {
                        initContext((Context) p.args[0], lpparam);
                    }
                }
            });
            hooked = true;
            log("[ Obsidian ] hooked PhoneWindowManager.init");
        } catch (Throwable t) {
            log("[ Obsidian ] PhoneWindowManager not found: " + t.getMessage());
        }

        if (!hooked) {
            try {
                findAndHookMethod(Instrumentation.class, "newApplication",
                        ClassLoader.class, String.class, Context.class, new XC_MethodHook() {
                    @Override protected void afterHookedMethod(MethodHookParam p) {
                        if (mContext == null && p.args[2] instanceof Context) {
                            initContext((Context) p.args[2], lpparam);
                        }
                    }
                });
                log("[ Obsidian ] hooked Instrumentation.newApplication (framework fallback)");
            } catch (Throwable t) {
                log("[ Obsidian ] framework fallback hook failed: " + t.getMessage());
            }
        }
    }

    private void hookOtherPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if ("com.android.systemui".equals(lpparam.packageName)) {
            it.tugaia56.obsidian.xposed.hooks.systemui.DstNotifBgView.hook(lpparam);
        }
        Log.d("ObsidianXP", "hookOtherPackage: hooking Instrumentation for " + lpparam.packageName);
        try {
            findAndHookMethod(Instrumentation.class, "newApplication",
                    ClassLoader.class, String.class, Context.class, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    Log.d("ObsidianXP", "newApplication fired, mContext=" + mContext + " pkg=" + lpparam.packageName);
                    if (mContext == null && p.args[2] instanceof Context) {
                        initContext((Context) p.args[2], lpparam);
                    }
                }
            });
            Log.d("ObsidianXP", "hookOtherPackage: hook installed OK for " + lpparam.packageName);
        } catch (Throwable t) {
            Log.d("ObsidianXP", "hookOtherPackage FAILED for " + lpparam.packageName + ": " + t);
            log("[ Obsidian ] hookOtherPackage failed for " + lpparam.packageName + ": " + t.getMessage());
        }
    }

    private void initContext(Context ctx, XC_LoadPackage.LoadPackageParam lpparam) {
        Log.d("ObsidianXP", "initContext: " + lpparam.packageName);
        mContext = ctx;
        try {
            ResourceManager.modRes = ctx.createPackageContext(
                    BuildConfig.APPLICATION_ID, Context.CONTEXT_IGNORE_SECURITY).getResources();
            ModResHolder.modRes = ResourceManager.modRes;
        } catch (Throwable t) {
            log("[ Obsidian ] modRes init failed: " + t.getMessage());
        }

        // Re-register CPB and WiFi icon XResources replacements now that modRes is set.
        // This mirrors OC's initResources() called from updatePrefs():
        // late registration forces cache invalidation so newDrawable() fires on next request.
        try { DstCpbStyle.initResourcesLate(); } catch (Throwable ignored) {}
        try {
            it.tugaia56.obsidian.xposed.hooks.systemui.DstWifiIconStyle.initResourcesLate();
        } catch (Throwable ignored) {}
        try {
            it.tugaia56.obsidian.xposed.hooks.systemui.DstSignalIconStyle.initResourcesLate();
        } catch (Throwable ignored) {}

        XPrefs.init(ctx);

        // Phase 1 – install hooks immediately so they fire during SystemUI init.
        // Preference fields use preloaded/static values or Java defaults until Phase 2.
        installHooks(lpparam);

        // Phase 2 – background thread: wait for ContentProvider, then push live prefs.
        if (!ModPacks.getMods(lpparam.packageName).isEmpty()) {
            new Thread(() -> waitAndRefreshPrefs(lpparam)).start();
        }
    }

    /**
     * Phase 1: create each mod instance and install its hooks immediately.
     * updatePrefs() is NOT called here — Xprefs isn't ready yet.
     */
    private void installHooks(XC_LoadPackage.LoadPackageParam lpparam) {
        Log.d("ObsidianXP", "installHooks: " + lpparam.packageName
                + " mods=" + ModPacks.getMods(lpparam.packageName).size());
        for (Class<? extends XposedMods> mod : ModPacks.getMods(lpparam.packageName)) {
            try {
                Log.d("ObsidianXP", "installing: " + mod.getSimpleName());
                XposedMods inst = mod.getConstructor(Context.class).newInstance(mContext);
                if (!inst.listensTo(lpparam.packageName)) {
                    Log.d("ObsidianXP", mod.getSimpleName() + " listensTo=false, skipped");
                    continue;
                }
                inst.handleLoadPackage(lpparam);
                runningMods.add(inst);
                Log.d("ObsidianXP", "installed OK: " + mod.getSimpleName());
                log("[ Obsidian ] hooks installed: " + mod.getSimpleName()
                        + " for " + lpparam.packageName);
            } catch (Throwable t) {
                Log.d("ObsidianXP", "installHooks FAILED: " + mod.getSimpleName() + ": " + t);
                log("[ Obsidian ] installHooks failed: " + mod.getSimpleName()
                        + ": " + t.getMessage());
            }
        }
    }

    /**
     * Phase 2: waits for Obsidian's ContentProvider then calls updatePrefs() on every
     * running mod so they switch from default/preloaded values to the user's saved prefs.
     * On slow boots the app may start 10-30 s after SystemUI — we wait indefinitely.
     */
    private void waitAndRefreshPrefs(XC_LoadPackage.LoadPackageParam lpparam) {
        while (true) {
            try {
                Xprefs.getBoolean("LoadTestBooleanValue", false);
                break;
            } catch (Throwable ignored) {
                try { Thread.sleep(1000); } catch (Throwable ignored2) {}
            }
        }
        log("[ Obsidian ] v" + BuildConfig.VERSION_NAME
                + " Xprefs ready → " + lpparam.packageName);

        for (XposedMods mod : new ArrayList<>(runningMods)) {
            if (!mod.listensTo(lpparam.packageName)) continue;
            try { mod.updatePrefs(); } catch (Throwable ignored) {}
        }
    }
}

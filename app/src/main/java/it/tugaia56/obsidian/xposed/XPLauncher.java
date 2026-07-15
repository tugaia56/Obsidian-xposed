package it.tugaia56.obsidian.xposed;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedBridge.log;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static it.tugaia56.obsidian.xposed.XPrefs.Xprefs;

import android.annotation.SuppressLint;
import android.app.Instrumentation;
import android.content.Context;
import java.util.ArrayList;
import java.util.List;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import it.tugaia56.obsidian.BuildConfig;
import it.tugaia56.obsidian.utils.Constants;

public class XPLauncher {

    public static ArrayList<XposedMods> runningMods = new ArrayList<>();
    @SuppressLint("StaticFieldLeak") public static Context mContext = null;

    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        log("[ Obsidian ] handleLoadPackage: " + lpparam.packageName);

        if (lpparam.packageName.equals(Constants.Packages.FRAMEWORK)) {
            hookFramework(lpparam);
        } else {
            hookOtherPackage(lpparam);
        }
    }

    private void hookFramework(XC_LoadPackage.LoadPackageParam lpparam) {
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
        try {
            findAndHookMethod(Instrumentation.class, "newApplication",
                    ClassLoader.class, String.class, Context.class, new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam p) {
                    if (mContext == null && p.args[2] instanceof Context) {
                        initContext((Context) p.args[2], lpparam);
                    }
                }
            });
        } catch (Throwable t) {
            log("[ Obsidian ] hookOtherPackage failed for " + lpparam.packageName + ": " + t.getMessage());
        }
    }

    private void initContext(Context ctx, XC_LoadPackage.LoadPackageParam lpparam) {
        mContext = ctx;
        try {
            ResourceManager.modRes = ctx.createPackageContext(
                    BuildConfig.APPLICATION_ID, Context.CONTEXT_IGNORE_SECURITY).getResources();
        } catch (Throwable t) {
            log("[ Obsidian ] modRes init failed: " + t.getMessage());
        }
        XPrefs.init(ctx);
        new Thread(() -> waitForXprefsLoad(lpparam)).start();
    }

    private void waitForXprefsLoad(XC_LoadPackage.LoadPackageParam lpparam) {
        // Skip early if no mods listen to this package (e.g. android / system_server).
        // ContentProvider queries from system_server can deadlock — skip before trying.
        List<Class<? extends XposedMods>> mods = ModPacks.getMods(lpparam.packageName);
        if (mods.isEmpty()) {
            log("[ Obsidian ] no mods for " + lpparam.packageName + " — skip XPrefs");
            return;
        }

        // Wait indefinitely for Obsidian's ContentProvider to become available.
        // On slow boots the app may start 10-30 s after SystemUI; a hard timeout
        // would leave all hooks un-registered (root cause of "all switches broken").
        while (true) {
            try {
                Xprefs.getBoolean("LoadTestBooleanValue", false);
                break;
            } catch (Throwable ignored) {
                try { Thread.sleep(1000); } catch (Throwable ignored2) {}
            }
        }
        log("[ Obsidian ] v" + BuildConfig.VERSION_NAME + " → " + lpparam.packageName);
        loadModpacks(lpparam);
    }

    private void loadModpacks(XC_LoadPackage.LoadPackageParam lpparam) {
        for (Class<? extends XposedMods> mod : ModPacks.getMods(lpparam.packageName)) {
            try {
                XposedMods inst = mod.getConstructor(Context.class).newInstance(mContext);
                if (!inst.listensTo(lpparam.packageName)) continue;
                try { inst.updatePrefs(); } catch (Throwable ignored) {}
                inst.initResources();
                inst.handleLoadPackage(lpparam);
                runningMods.add(inst);
                log("[ Obsidian ] loaded " + mod.getSimpleName() + " for " + lpparam.packageName);
            } catch (Throwable t) {
                log("[ Obsidian ] failed to load " + mod.getSimpleName() + ": " + t.getMessage());
            }
        }
    }
}

package it.tugaia56.obsidian.xposed;

import android.content.res.Resources;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import it.tugaia56.obsidian.xposed.hooks.framework.DstDialogStyle;
import it.tugaia56.obsidian.xposed.hooks.framework.MonetFreeze;

public class ResourceManager implements IXposedHookInitPackageResources, IXposedHookZygoteInit {

    public final static HashMap<String, XC_InitPackageResources.InitPackageResourcesParam> resparams = new HashMap<>();
    public static Resources modRes;

    @Override
    public void initZygote(StartupParam startupParam) {
        // chmod PRIMA del tentativo di lettura, sempre, in ogni boot.
        // Se initZygote ha i permessi (root), i file diventano leggibili subito
        // e preloadFromFile() riesce gia in questo boot.
        try {
            boolean dirOk = new File("/data/user_de/0/it.tugaia56.obsidian")
                    .setExecutable(true, false);  // 700 -> 711
            boolean fileOk = new File("/data/user_de/0/it.tugaia56.obsidian/shared_prefs/it.tugaia56.obsidian_preferences.xml")
                    .setReadable(true, false);    // 660 -> 664
            XposedBridge.log("[ Obsidian ] initZygote: chmod dir=" + dirOk + " file=" + fileOk);
        } catch (Throwable t) {
            XposedBridge.log("[ Obsidian ] initZygote: chmod FAILED: " + t);
        }

        XposedBridge.log("[ Obsidian ] ResourceManager.initZygote: preloading prefs from disk");
        MonetFreeze.preloadFromFile();
        DstDialogStyle.preloadFromFile();
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) {
        resparams.put(resparam.packageName, resparam);
        MonetFreeze.applyPreloaded(resparam);
        DstDialogStyle.applyPreloaded(resparam);
        if (!XPLauncher.runningMods.isEmpty()) {
            for (XposedMods mod : new ArrayList<>(XPLauncher.runningMods)) {
                if (!mod.listensTo(resparam.packageName)) continue;
                XposedBridge.log("[ Obsidian ] ResourceManager: initResources -> " + resparam.packageName);
                try {
                    mod.initResources();
                } catch (Throwable t) {
                    XposedBridge.log("[ Obsidian ] initResources error in "
                            + mod.getClass().getSimpleName() + ": " + t.getMessage());
                }
            }
        }
    }
}

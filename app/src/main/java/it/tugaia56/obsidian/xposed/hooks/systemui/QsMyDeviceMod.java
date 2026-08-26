package it.tugaia56.obsidian.xposed.hooks.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static it.tugaia56.obsidian.utils.Constants.Packages.SYSTEM_UI;
import static it.tugaia56.obsidian.xposed.XPrefs.Xprefs;

import android.content.Context;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import it.tugaia56.obsidian.xposed.XposedMods;

/**
 * "Il mio dispositivo" — porting reale di OC's FeatureOption.isSupportMyDevice(). Non è un
 * hook di vista: forza a true il feature-flag OOS che decide se il pannello QS mostra la
 * card "il mio dispositivo" stock, lasciando OOS disegnarla normalmente. La versione stock
 * di OOS16 può spostare il metodo in una classe separata (QSFeatureOption) — agganciamo
 * entrambe se presenti, come fa OC.
 */
public class QsMyDeviceMod extends XposedMods {

    private static final String PREF_MY_DEVICE = "OBS_QS_MY_DEVICE";

    private boolean mShowMyDevice = false;

    public QsMyDeviceMod(Context context) { super(context); }

    @Override
    public void updatePrefs(String... Key) {
        if (Xprefs == null) return;
        mShowMyDevice = Xprefs.getBoolean(PREF_MY_DEVICE, false);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) throws Throwable {
        if (!SYSTEM_UI.equals(lp.packageName)) return;

        XC_MethodHook forceSupported = new XC_MethodHook() {
            @Override protected void beforeHookedMethod(MethodHookParam param) {
                if (mShowMyDevice) param.setResult(true);
            }
        };

        try {
            Class<?> featureOption = findClass("com.oplusos.systemui.common.feature.FeatureOption", lp.classLoader);
            hookAllMethods(featureOption, "isSupportMyDevice", forceSupported);
        } catch (Throwable ignored) {}

        try {
            Class<?> qsFeatureOption = findClass("com.oplusos.systemui.common.feature.QSFeatureOption", lp.classLoader);
            hookAllMethods(qsFeatureOption, "isSupportMyDevice", forceSupported);
        } catch (Throwable ignored) {}
    }

    @Override public boolean listensTo(String packageName) { return SYSTEM_UI.equals(packageName); }
}

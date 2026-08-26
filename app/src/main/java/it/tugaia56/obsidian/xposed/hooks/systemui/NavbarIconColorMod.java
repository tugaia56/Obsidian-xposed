package it.tugaia56.obsidian.xposed.hooks.systemui;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.setIntField;
import static it.tugaia56.obsidian.utils.Constants.Packages.SYSTEM_UI;
import static it.tugaia56.obsidian.xposed.XPrefs.Xprefs;

import android.content.Context;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import it.tugaia56.obsidian.xposed.XposedMods;

/**
 * Colore Icone Barra di Navigazione (Indietro/Home/Recenti) — le risorse sovrascritte da
 * NavbarStyleFragment (navigation_bar_home_handle_dark/light_color, navigation_bar_icon_color,
 * dark|light_mode_icon_color_single_tone) non bastano da sole: i tre tasti (KeyButtonDrawable,
 * via NavigationBarView.mLightIconColor/mDarkIconColor) leggono ?attr/singleToneColor, che
 * risolve sulle stesse due risorse single_tone lette ANCHE dalla barra di stato — overlay su
 * quelle risorse coloriva pure la statusbar, effetto indesiderato. Fix: hook diretto sui campi
 * (final, ma modificabili via reflection) di NavigationBarView, scope solo-navbar — la
 * statusbar non passa mai da questa classe. I valori "stock" vengono letti una volta subito
 * dopo il costruttore (prima di sovrascriverli) così il toggle OFF ripristina il colore
 * originale live, senza bisogno di riavviare SystemUI.
 */
public class NavbarIconColorMod extends XposedMods {

    private static final String PREF_ON = "DST_NAVCOLOR_on";
    private static final String PREF_COLOR = "DST_NAVCOLOR";

    private boolean enabled = false;
    private int color = 0xFFFFFFFF;

    private Object mNavBarView = null;
    private int mStockLight = 0;
    private int mStockDark = 0;

    public NavbarIconColorMod(Context context) {
        super(context);
    }

    @Override
    public void updatePrefs(String... Key) {
        if (Xprefs == null) return;
        enabled = Xprefs.getBoolean(PREF_ON, false);
        color = Xprefs.getInt(PREF_COLOR, 0xFFFFFFFF);

        if (Key.length == 0 || PREF_ON.equals(Key[0]) || PREF_COLOR.equals(Key[0])) {
            applyColor();
        }
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        Class<?> navBarView = findClass(
                "com.android.systemui.navigationbar.views.NavigationBarView", lpparam.classLoader);

        XposedBridge.hookAllConstructors(navBarView, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                try {
                    mNavBarView = param.thisObject;
                    mStockLight = getIntField(mNavBarView, "mLightIconColor");
                    mStockDark = getIntField(mNavBarView, "mDarkIconColor");
                    applyColor();
                } catch (Throwable ignored) {}
            }
        });
    }

    private void applyColor() {
        if (mNavBarView == null) return;
        try {
            setIntField(mNavBarView, "mLightIconColor", enabled ? color : mStockLight);
            setIntField(mNavBarView, "mDarkIconColor", enabled ? color : mStockDark);
            callMethod(mNavBarView, "updateMainIcons");
        } catch (Throwable ignored) {}
    }

    @Override
    public boolean listensTo(String packageName) {
        return SYSTEM_UI.equals(packageName);
    }
}

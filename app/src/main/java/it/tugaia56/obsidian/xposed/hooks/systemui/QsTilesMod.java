package it.tugaia56.obsidian.xposed.hooks.systemui;

import static de.robv.android.xposed.XposedBridge.hookAllMethods;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.setIntField;
import static it.tugaia56.obsidian.utils.Constants.Packages.SYSTEM_UI;
import static it.tugaia56.obsidian.xposed.XPrefs.Xprefs;

import android.content.Context;
import android.content.res.Configuration;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import it.tugaia56.obsidian.xposed.XposedMods;

/**
 * Numero di riquadri — porting reale di OC's QSTiles. Sovrascrive i campi che SystemUI
 * stesso legge per il layout del pannello QS "Classico" (com.android.systemui.qs.TileLayout/
 * QuickQSPanel — classi AOSP stock, non OOS-specifiche), lasciando poi che sia SystemUI a
 * finire il calcolo del layout con i valori nostri. Vale solo per lo stile "Classico": OC
 * stesso disabilita l'equivalente per lo stile "Separati" su ogni SDK diverso da 35, quindi
 * non esiste un riferimento funzionante da portare per SDK36/OOS16.
 */
public class QsTilesMod extends XposedMods {

    private static final String PREF_CUSTOMIZE   = "quick_settings_tiles_customize";
    private static final String PREF_QUICK_COUNT = "quick_settings_quick_tiles_seek";
    private static final String PREF_ROWS        = "quick_settings_tiles_rows_seek";
    private static final String PREF_COLUMNS     = "quick_settings_tiles_horizontal_columns_seek";
    private static final String PREF_COLUMNS_LS  = "quick_settings_tiles_vertical_columns_seek";

    private boolean mCustomize = false;
    private int mQuickCount = 5;
    private int mRows = 3;
    private int mColumns = 4;
    private int mColumnsLandscape = 4;

    public QsTilesMod(Context context) { super(context); }

    @Override
    public void updatePrefs(String... Key) {
        if (Xprefs == null) return;
        mCustomize = Xprefs.getBoolean(PREF_CUSTOMIZE, false);
        mQuickCount = Xprefs.getInt(PREF_QUICK_COUNT, 5);
        mRows = Xprefs.getInt(PREF_ROWS, 3);
        mColumns = Xprefs.getInt(PREF_COLUMNS, 4);
        mColumnsLandscape = Xprefs.getInt(PREF_COLUMNS_LS, 4);
    }

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lp) throws Throwable {
        if (!SYSTEM_UI.equals(lp.packageName)) return;

        try {
            Class<?> quickQsPanel = findClass("com.android.systemui.qs.QuickQSPanel", lp.classLoader);
            hookAllMethods(quickQsPanel, "getNumQuickTiles", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    if (mCustomize) param.setResult(mQuickCount);
                }
            });
        } catch (Throwable ignored) {}

        try {
            Class<?> tileLayout = findClass("com.android.systemui.qs.TileLayout", lp.classLoader);

            hookAllMethods(tileLayout, "updateMaxRows", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    if (!mCustomize) return;
                    try {
                        boolean isPortrait = mContext.getResources().getConfiguration().orientation
                                == Configuration.ORIENTATION_PORTRAIT;
                        if (!isPortrait) return;
                        int current = getIntField(param.thisObject, "mRows");
                        setIntField(param.thisObject, "mRows", mRows);
                        param.setResult(current != mRows);
                    } catch (Throwable ignored) {}
                }
            });

            hookAllMethods(tileLayout, "updateColumns", new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    if (!mCustomize) return;
                    try {
                        boolean isPortrait = mContext.getResources().getConfiguration().orientation
                                == Configuration.ORIENTATION_PORTRAIT;
                        int wanted = isPortrait ? mColumns : mColumnsLandscape;
                        int current = getIntField(param.thisObject, "mColumns");
                        setIntField(param.thisObject, "mColumns", wanted);
                        param.setResult(current != wanted);
                    } catch (Throwable ignored) {}
                }
            });
        } catch (Throwable ignored) {}
    }

    @Override public boolean listensTo(String packageName) { return SYSTEM_UI.equals(packageName); }
}

package it.tugaia56.obsidian.xposed;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import it.tugaia56.obsidian.BuildConfig;
import it.tugaia56.obsidian.xposed.utils.ExtendedRemotePreferences;
public class XPrefs {
    @SuppressLint("StaticFieldLeak") public static ExtendedRemotePreferences Xprefs;
    private static final SharedPreferences.OnSharedPreferenceChangeListener listener = (sp, key) -> loadEverything(key);
    public static void init(Context context) {
        Xprefs = new ExtendedRemotePreferences(context, BuildConfig.APPLICATION_ID, BuildConfig.APPLICATION_ID + "_preferences", true);
        Xprefs.registerOnSharedPreferenceChangeListener(listener);
    }
    public static void loadEverything(String... key) {
        for (XposedMods mod : XPLauncher.runningMods) mod.updatePrefs(key);
    }
}

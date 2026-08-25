package it.tugaia56.obsidian.xposed;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import de.robv.android.xposed.XposedBridge;
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
        // Un mod che lancia un'eccezione (es. un pref con tipo cambiato tra versioni) non deve
        // impedire agli altri mod di aggiornarsi, né tantomeno destabilizzare SystemUI.
        for (XposedMods mod : XPLauncher.runningMods) {
            try {
                mod.updatePrefs(key);
            } catch (Throwable t) {
                XposedBridge.log("[ Obsidian ] " + mod.getClass().getSimpleName() + ".updatePrefs failed: " + t);
            }
        }
    }
}

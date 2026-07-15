package it.tugaia56.obsidian.utils;
import android.content.Context;
import it.tugaia56.obsidian.BuildConfig;
import it.tugaia56.obsidian.Obsidian;
import it.tugaia56.obsidian.xposed.utils.ExtendedSharedPreferences;
public class ObsidianPrefs {
    private static final ExtendedSharedPreferences prefs = ExtendedSharedPreferences.from(
            Obsidian.get().createDeviceProtectedStorageContext()
                    .getSharedPreferences(BuildConfig.APPLICATION_ID + "_preferences", Context.MODE_PRIVATE));
    private static final ExtendedSharedPreferences.Editor editor = prefs.edit();
    public static ExtendedSharedPreferences getPrefs() { return prefs; }
    public static void putBoolean(String key, boolean value) { editor.putBoolean(key, value).apply(); }
    public static void putInt(String key, int value)         { editor.putInt(key, value).apply(); }
    public static void putFloat(String key, float value)     { editor.putFloat(key, value).apply(); }
    public static void putString(String key, String value)   { editor.putString(key, value).apply(); }
    public static boolean getBoolean(String key, boolean d)  { return prefs.getBoolean(key, d); }
    public static int     getInt(String key, int d)          { return prefs.getInt(key, d); }
    public static float   getFloat(String key, float d)      { return prefs.getFloat(key, d); }
    public static String  getString(String key, String d)    { return prefs.getString(key, d); }
    public static void    clear()                            { editor.clear().apply(); }
}

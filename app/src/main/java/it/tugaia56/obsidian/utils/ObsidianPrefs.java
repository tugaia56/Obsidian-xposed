package it.tugaia56.obsidian.utils;
import android.content.Context;
import it.tugaia56.obsidian.BuildConfig;
import it.tugaia56.obsidian.Obsidian;
import it.tugaia56.obsidian.xposed.utils.ExtendedSharedPreferences;
public class ObsidianPrefs {
    private static final ExtendedSharedPreferences prefs = ExtendedSharedPreferences.from(
            Obsidian.get().createDeviceProtectedStorageContext()
                    .getSharedPreferences(BuildConfig.APPLICATION_ID + "_preferences", Context.MODE_PRIVATE));
    public static ExtendedSharedPreferences getPrefs() { return prefs; }
    // commit() è sincrono: garantisce la scrittura su disco immediatamente,
    // evitando la perdita di dati quando il processo viene killato durante il riavvio.
    public static void putBoolean(String key, boolean value) { prefs.edit().putBoolean(key, value).commit(); }
    public static void putInt(String key, int value)         { prefs.edit().putInt(key, value).commit(); }
    public static void putFloat(String key, float value)     { prefs.edit().putFloat(key, value).commit(); }
    public static void putString(String key, String value)   { prefs.edit().putString(key, value).commit(); }
    public static boolean getBoolean(String key, boolean d)  { return prefs.getBoolean(key, d); }
    public static int     getInt(String key, int d)          { return prefs.getInt(key, d); }
    public static float   getFloat(String key, float d)      { return prefs.getFloat(key, d); }
    public static String  getString(String key, String d)    { return prefs.getString(key, d); }
    public static void    remove(String key)                  { prefs.edit().remove(key).commit(); }
    public static void    clear()                             { prefs.edit().clear().commit(); }
}

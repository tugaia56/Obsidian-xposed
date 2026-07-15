package it.tugaia56.obsidian.utils;
import android.content.Context;
import android.content.SharedPreferences;
import it.tugaia56.obsidian.BuildConfig;
import it.tugaia56.obsidian.Obsidian;
public class Prefs {
    public static final String SharedPref = BuildConfig.APPLICATION_ID;
    public static SharedPreferences prefs = Obsidian.getAppContext()
            .getSharedPreferences(SharedPref, Context.MODE_PRIVATE);
    private static SharedPreferences.Editor editor = prefs.edit();
    public static void putBoolean(String key, boolean val) { editor.putBoolean(key, val).apply(); }
    public static void putInt(String key, int val)         { editor.putInt(key, val).apply(); }
    public static void putString(String key, String val)   { editor.putString(key, val).apply(); }
    public static boolean getBoolean(String key, boolean def) { return prefs.getBoolean(key, def); }
    public static int     getInt(String key, int def)         { return prefs.getInt(key, def); }
    public static String  getString(String key, String def)   { return prefs.getString(key, def); }
    public static void clearPrefs(String... keys) { for (String k : keys) editor.remove(k).apply(); }
}

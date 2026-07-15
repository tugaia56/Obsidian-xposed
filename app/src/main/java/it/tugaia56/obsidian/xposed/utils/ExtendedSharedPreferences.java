package it.tugaia56.obsidian.xposed.utils;
import android.content.SharedPreferences;
import java.util.Map;
import java.util.Set;
public class ExtendedSharedPreferences implements SharedPreferences {
    private final SharedPreferences mPrefs;
    private ExtendedSharedPreferences(SharedPreferences prefs) { this.mPrefs = prefs; }
    public static ExtendedSharedPreferences from(SharedPreferences prefs) { return new ExtendedSharedPreferences(prefs); }
    public Editor edit() { return new Editor(mPrefs.edit()); }
    @Override public Map<String, ?> getAll()                               { return mPrefs.getAll(); }
    @Override public String getString(String k, String d)                  { return mPrefs.getString(k, d); }
    @Override public Set<String> getStringSet(String k, Set<String> d)     { return mPrefs.getStringSet(k, d); }
    @Override public int     getInt(String k, int d)                       { return mPrefs.getInt(k, d); }
    @Override public long    getLong(String k, long d)                     { return mPrefs.getLong(k, d); }
    @Override public float   getFloat(String k, float d)                   { return mPrefs.getFloat(k, d); }
    @Override public boolean getBoolean(String k, boolean d)               { return mPrefs.getBoolean(k, d); }
    @Override public boolean contains(String k)                            { return mPrefs.contains(k); }
    @Override public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l)   { mPrefs.registerOnSharedPreferenceChangeListener(l); }
    @Override public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l) { mPrefs.unregisterOnSharedPreferenceChangeListener(l); }
    public static class Editor implements SharedPreferences.Editor {
        private final SharedPreferences.Editor e;
        public Editor(SharedPreferences.Editor e) { this.e = e; }
        @Override public Editor putString(String k, String v)           { e.putString(k, v); return this; }
        @Override public Editor putStringSet(String k, Set<String> v)   { e.putStringSet(k, v); return this; }
        @Override public Editor putInt(String k, int v)                 { e.putInt(k, v); return this; }
        @Override public Editor putLong(String k, long v)               { e.putLong(k, v); return this; }
        @Override public Editor putFloat(String k, float v)             { e.putFloat(k, v); return this; }
        @Override public Editor putBoolean(String k, boolean v)         { e.putBoolean(k, v); return this; }
        @Override public Editor remove(String k)                        { e.remove(k); return this; }
        @Override public Editor clear()                                 { e.clear(); return this; }
        @Override public boolean commit()                               { return e.commit(); }
        @Override public void   apply()                                 { e.apply(); }
    }
}

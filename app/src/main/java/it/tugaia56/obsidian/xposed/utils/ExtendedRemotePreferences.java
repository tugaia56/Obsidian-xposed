package it.tugaia56.obsidian.xposed.utils;
import android.content.Context;
import com.crossbowffs.remotepreferences.RemotePreferences;
public class ExtendedRemotePreferences extends RemotePreferences {
    public ExtendedRemotePreferences(Context context, String authority, String prefFileName) { super(context, authority, prefFileName); }
    public ExtendedRemotePreferences(Context context, String authority, String prefFileName, boolean strictMode) { super(context, authority, prefFileName, strictMode); }
    public int   getSliderInt(String key, int defVal)     { return getInt(key, defVal); }
    public float getSliderFloat(String key, float defVal) { return getFloat(key, defVal); }
}

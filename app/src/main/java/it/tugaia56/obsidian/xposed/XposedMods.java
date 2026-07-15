package it.tugaia56.obsidian.xposed;
import android.content.Context;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
public abstract class XposedMods {
    protected Context mContext;
    protected boolean mDebug = false;
    public XposedMods(Context context) { mContext = context; }
    public abstract void updatePrefs(String... Key);
    public abstract void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable;
    protected void initResources() {}
    public abstract boolean listensTo(String packageName);
    public void log(String message) {
        if (!mDebug) return;
        XposedBridge.log("[ Obsidian - " + getClass().getSimpleName() + " ] " + message);
    }
}

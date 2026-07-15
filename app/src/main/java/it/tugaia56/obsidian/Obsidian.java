package it.tugaia56.obsidian;
import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import com.topjohnwu.superuser.Shell;
public class Obsidian extends Application {
    @SuppressLint("StaticFieldLeak") private static Context appContext;
    private static Obsidian instance;
    static {
        Shell.enableVerboseLogging = false;
        Shell.setDefaultBuilder(Shell.Builder.create().setFlags(Shell.FLAG_REDIRECT_STDERR).setTimeout(20));
    }
    @Override public void onCreate() { super.onCreate(); instance = this; appContext = createDeviceProtectedStorageContext(); }
    public static Obsidian get() { return instance; }
    public static Context getAppContext() { return appContext; }
}

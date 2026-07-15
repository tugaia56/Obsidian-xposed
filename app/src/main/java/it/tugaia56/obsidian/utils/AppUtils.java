package it.tugaia56.obsidian.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.topjohnwu.superuser.Shell;

import it.tugaia56.obsidian.R;

public class AppUtils {

    public static void restartScope(String packageName) {
        Shell.cmd("killall " + packageName).submit();
    }

    /** Restart SystemUI silently (no toast). */
    public static void restartSystemUI() {
        restartScope(Constants.SYSTEM_UI);
    }

    /** Restart SystemUI and show a toast feedback on the main thread. */
    public static void restartSystemUI(Context context) {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(context.getApplicationContext(),
                        R.string.restart_systemui_toast, Toast.LENGTH_SHORT).show());
        restartScope(Constants.SYSTEM_UI);
    }
}

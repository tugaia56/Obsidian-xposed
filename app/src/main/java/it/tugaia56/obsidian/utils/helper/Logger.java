package it.tugaia56.obsidian.utils.helper;

import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

/**
 * Versione semplificata di OC's Logger — qui logga solo su logcat (niente scrittura file
 * su storage esterno, non serve: il compilatore overlay usa questo solo per diagnostica
 * quando qualcosa fallisce, consultabile con `adb logcat`).
 */
public class Logger {

    private static final String TAG = "Obsidian-OverlayCompiler";

    public static void writeLog(String tag, String header, List<String> details) {
        Log.e(TAG, tag + ": " + header + "\n" + String.join("\n", details));
    }

    public static void writeLog(String tag, String header, String details) {
        Log.e(TAG, tag + ": " + header + "\n" + details);
    }

    public static void writeLog(String tag, String header, Exception exception) {
        StringWriter writer = new StringWriter();
        exception.printStackTrace(new PrintWriter(writer));
        Log.e(TAG, tag + ": " + header + "\n" + writer);
    }
}

package it.tugaia56.obsidian.utils;

import com.topjohnwu.superuser.Shell;

import java.util.List;

/** Porting trimmed di OC's RootUtil — solo quanto serve qui (Obsidian assume già
 *  root/Magisk-KSU disponibile per il resto dell'app; niente rilevamento variante). */
public class RootUtil {

    public static void setPermissions(final int permission, final String filename) {
        Shell.cmd("chmod " + permission + ' ' + filename).exec();
    }

    public static boolean folderExists(String dir) {
        List<String> lines = Shell.cmd("test -d " + dir + " && echo '1'").exec().getOut();
        for (String line : lines) {
            if (line.contains("1")) return true;
        }
        return false;
    }

    public static boolean moduleExists(String moduleId) {
        return folderExists("/data/adb/modules/" + moduleId);
    }
}

package it.tugaia56.obsidian.utils.overlay;

import com.topjohnwu.superuser.Shell;

import java.util.List;

public class OverlayUtil {

    public static void enableOverlay(String packageName) {
        Shell.cmd("cmd overlay enable --user current " + packageName).submit();
    }

    public static void disableOverlay(String packageName) {
        Shell.cmd("cmd overlay disable --user current " + packageName).submit();
    }

    /** Bloccante (non .submit()) — i chiamanti girano già su thread in background e hanno
     *  bisogno che il comando sia effettivamente completato prima di verificare lo stato reale
     *  dell'overlay (es. isOverlayEnabled subito dopo). */
    public static void enableOverlays(String... pkgNames) {
        StringBuilder command = new StringBuilder();
        for (String pkgName : pkgNames) {
            command.append("cmd overlay enable --user current ").append(pkgName)
                    .append("; cmd overlay set-priority ").append(pkgName).append(" highest; ");
        }
        Shell.cmd(command.toString().trim()).exec();
    }

    public static void disableOverlays(String... pkgNames) {
        StringBuilder command = new StringBuilder();
        for (String pkgName : pkgNames) {
            command.append("cmd overlay disable --user current ").append(pkgName).append("; ");
        }
        Shell.cmd(command.toString().trim()).exec();
    }

    public static boolean isOverlayEnabled(String pkgName) {
        List<String> out = Shell.cmd("[[ $(cmd overlay list | grep -o '\\[x\\] " + pkgName + "') ]] && echo 1 || echo 0").exec().getOut();
        return !out.isEmpty() && out.get(0).equals("1");
    }
}

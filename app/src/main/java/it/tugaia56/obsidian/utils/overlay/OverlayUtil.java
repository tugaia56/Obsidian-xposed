package it.tugaia56.obsidian.utils.overlay;

import com.topjohnwu.superuser.Shell;

public class OverlayUtil {

    public static void enableOverlay(String packageName) {
        Shell.cmd("cmd overlay enable --user current " + packageName).submit();
    }

    public static void disableOverlay(String packageName) {
        Shell.cmd("cmd overlay disable --user current " + packageName).submit();
    }
}

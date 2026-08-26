package it.tugaia56.obsidian.utils;

import com.topjohnwu.superuser.Shell;

/** Porting di OC's SystemUtil — remount /system RW/RO per installare l'overlay
 *  compilato in /system/product/overlay. */
public class SystemUtil {

    public static void mountRW() {
        Shell.cmd("mount -o remount,rw /").exec();
        if (RootUtil.moduleExists("magisk_overlayfs")) {
            Shell.cmd("-mm -c magic_remount_rw").exec();
        } else if (RootUtil.moduleExists("overlayfs")) {
            Shell.cmd("/data/overlayfs/tmp/overlayrw -rw /system/product/overlay").exec();
        }
    }

    public static void mountRO() {
        Shell.cmd("mount -o remount,ro /").exec();
        if (RootUtil.moduleExists("magisk_overlayfs")) {
            Shell.cmd("-mm -c magic_remount_ro").exec();
        } else if (RootUtil.moduleExists("overlayfs")) {
            Shell.cmd("/data/overlayfs/tmp/overlayrw -ro /system/product/overlay").exec();
        }
    }
}

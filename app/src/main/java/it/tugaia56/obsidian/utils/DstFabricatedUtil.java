package it.tugaia56.obsidian.utils;

import com.topjohnwu.superuser.Shell;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import it.tugaia56.obsidian.Obsidian;
import it.tugaia56.obsidian.ui.models.DarkShadowItem;

/**
 * Applies every DST color resource as a Fabricated Overlay so Substratum and
 * RRO-based consumers can read them from the resource table.
 *
 * XResources.setReplacement() (MonetFreeze) is invisible at the resource-table
 * level. This class replicates OC's FabricatedUtil approach: one fabricated
 * overlay per resource, named ObsidianComponent<OVERLAYNAME>_<index>.
 *
 * IMPORTANT: applyThenRun() must be used instead of calling apply() and
 * restarting immediately after. SystemUI restart must happen ONLY after all
 * shell commands finish, otherwise the resource table still has the old value.
 */
public class DstFabricatedUtil {

    private static final String PREFIX = "ObsidianComponent";
    private static final String TYPE   = "0x1c"; // TYPE_COLOR

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Fabricate all overlays for this item on a background thread, then call
     * {@code onDone} (e.g. AppUtils::restartSystemUI) when all commands finish.
     * No-op if item.isEnabled() is false (onDone is NOT called in that case).
     *
     * Also persists DST colours to system properties so MonetFreeze can read
     * them at boot before the SharedPreferences file is world-readable.
     */
    public static void applyThenRun(DarkShadowItem item, Runnable onDone) {
        if (!item.isEnabled()) return;

        List<String> commands = buildApplyCommands(item);
        if (commands.isEmpty()) return;

        new Thread(() -> {
            Shell.cmd(String.join("; ", commands)).exec();
            saveBootProps(); // write persist.obsidian.dst.* for MonetFreeze boot fallback
            if (onDone != null) onDone.run();
        }).start();
    }

    /**
     * Re-applica tutti i FabricatedOverlay DST abilitati su un thread di sfondo,
     * poi chiama onDone (tipicamente AppUtils::restartSystemUI).
     * Usato dopo il ripristino del backup.
     */
    public static void reapplyAll(Runnable onDone) {
        new Thread(() -> {
            try {
                Context ctx = Obsidian.get();
                if (ctx != null) {
                    List<String> allCmds = new ArrayList<>();
                    for (DarkShadowItem item : DarkShadowUtils.getItems(ctx)) {
                        if (item.isEnabled()) allCmds.addAll(buildApplyCommands(item));
                    }
                    if (!allCmds.isEmpty()) Shell.cmd(String.join("; ", allCmds)).exec();
                    saveBootProps();
                }
            } catch (Throwable ignored) {}
            if (onDone != null) onDone.run();
        }).start();
    }

    /**
     * Fabricate overlays where each resource gets a different color (e.g. Accent Outline preset).
     * {@code resourceColors} is a LinkedHashMap preserving insertion order → index mapping.
     */
    public static void applyMultiColorThenRun(String overlayName, String pkg,
            LinkedHashMap<String, Integer> resourceColors, Runnable onDone) {
        List<String> commands = new ArrayList<>();
        int i = 0;
        for (Map.Entry<String, Integer> entry : resourceColors.entrySet()) {
            addFabricateCommands(commands, pkg, overlayName, i, entry.getKey(), toHex(entry.getValue()));
            i++;
        }
        if (commands.isEmpty()) { if (onDone != null) onDone.run(); return; }
        final String joined = String.join("; ", commands);
        new Thread(() -> { Shell.cmd(joined).exec(); if (onDone != null) onDone.run(); }).start();
    }

    /**
     * Disable all fabricated overlays for the given item on a background thread,
     * then call {@code onDone} when finished.
     */
    public static void disableThenRun(DarkShadowItem item, Runnable onDone) {
        List<String> commands = new ArrayList<>();
        int count = item.getResourceNames().size() + item.getAdjustColors().size();
        for (int i = 0; i < count; i++) {
            commands.add("cmd overlay disable --user current com.android.shell:"
                    + PREFIX + item.getOverlayName() + "_" + i);
        }
        if (commands.isEmpty()) {
            if (onDone != null) onDone.run();
            return;
        }
        final String joined = String.join("; ", commands);
        new Thread(() -> {
            Shell.cmd(joined).exec();
            saveBootProps(); // aggiorna props anche quando si disabilita
            if (onDone != null) onDone.run();
        }).start();
    }

    // ── Internals ──────────────────────────────────────────────────────────────

    private static List<String> buildApplyCommands(DarkShadowItem item) {
        int color = item.getColor();
        String pkg = item.getPackages().isEmpty() ? "android" : item.getPackages().get(0);
        String colorHex = toHex(color);

        List<String> commands = new ArrayList<>();
        int i = 0;

        for (String resName : item.getResourceNames()) {
            addFabricateCommands(commands, pkg, item.getOverlayName(), i, resName, colorHex);
            i++;
        }
        for (Map.Entry<String, Integer> entry : item.getAdjustColors().entrySet()) {
            String adjHex = toHex(ColorUtils.adjustColor(color, entry.getValue()));
            addFabricateCommands(commands, pkg, item.getOverlayName(), i, entry.getKey(), adjHex);
            i++;
        }
        return commands;
    }

    private static void addFabricateCommands(List<String> out,
                                             String target, String overlayName,
                                             int index, String resource, String hex) {
        String fullName = PREFIX + overlayName + "_" + index;
        out.add("cmd overlay fabricate --target " + target
                + " --name " + fullName
                + " " + target + ":color/" + resource
                + " " + TYPE + " " + hex);
        out.add("cmd overlay enable --user current com.android.shell:" + fullName);
    }

    /** 0xAARRGGBB hex string; mask ensures unsigned 32-bit even for signed int. */
    private static String toHex(int color) {
        return String.format("0x%08X", 0xFFFFFFFFL & color);
    }

    // ── Boot-time property persistence ────────────────────────────────────────

    /**
     * Writes current DST pref values to persist.obsidian.dst.* system properties
     * so MonetFreeze can read them at boot via SystemProperties.get() when the
     * SharedPreferences XML file is not yet world-readable (EACCES from system server).
     *
     * Must be called after every DST colour change (on a background thread).
     * Root access is required (setprop for persist.* props needs root).
     */
    public static void saveBootProps() {
        try {
            boolean a1On = ObsidianPrefs.getBoolean("DST_ACCENT1_on",    false);
            int     a1   = ObsidianPrefs.getInt(    "DST_ACCENT1",        0);
            boolean a2On = ObsidianPrefs.getBoolean("DST_ACCENT2_on",    false);
            int     a2   = ObsidianPrefs.getInt(    "DST_ACCENT2",        0);
            boolean a3On = ObsidianPrefs.getBoolean("DST_ACCENT3_on",    false);
            int     a3   = ObsidianPrefs.getInt(    "DST_ACCENT3",        0);
            boolean bgOn = ObsidianPrefs.getBoolean("DST_BACKGROUND_on", false);
            int     bg   = ObsidianPrefs.getInt(    "DST_BACKGROUND",     0);
            String  pin       = ObsidianPrefs.getString("DST_PIN",              "");
            String  pinNum    = ObsidianPrefs.getString("DST_PIN_NUM",          "");
            String  dlgPreset = ObsidianPrefs.getString("DST_DLG_PRESET_NAME",  "");

            // setprop accepts any string value; store ints as decimal (signed ok)
            Shell.cmd(
                "setprop persist.obsidian.dst.a1_on      " + (a1On ? "1" : "0"),
                "setprop persist.obsidian.dst.a1         " + a1,
                "setprop persist.obsidian.dst.a2_on      " + (a2On ? "1" : "0"),
                "setprop persist.obsidian.dst.a2         " + a2,
                "setprop persist.obsidian.dst.a3_on      " + (a3On ? "1" : "0"),
                "setprop persist.obsidian.dst.a3         " + a3,
                "setprop persist.obsidian.dst.bg_on      " + (bgOn ? "1" : "0"),
                "setprop persist.obsidian.dst.bg         " + bg,
                "setprop persist.obsidian.dst.pin         \"" + (pin       == null ? "" : pin)       + "\"",
                "setprop persist.obsidian.dst.pin_num     \"" + (pinNum    == null ? "" : pinNum)    + "\"",
                "setprop persist.obsidian.dst.dlg_preset  \"" + (dlgPreset == null ? "" : dlgPreset) + "\""
            ).exec();

            // Also chmod the prefs file world-readable so future boots can read it
            // directly (faster than props fallback). Best-effort — may fail if file
            // was just atomically-rewritten by SharedPreferences with fresh 0600 perms.
            Shell.cmd("chmod 644 /data/user_de/0/it.tugaia56.obsidian/shared_prefs/"
                    + "it.tugaia56.obsidian_preferences.xml").exec();
        } catch (Throwable ignored) {}
    }
}

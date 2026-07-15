package it.tugaia56.obsidian.xposed;

import java.util.ArrayList;
import java.util.List;

import it.tugaia56.obsidian.utils.Constants;
import it.tugaia56.obsidian.xposed.hooks.framework.MonetFreeze;
import it.tugaia56.obsidian.xposed.hooks.systemui.QsBackground;
import it.tugaia56.obsidian.xposed.hooks.systemui.StatusbarClock;
import it.tugaia56.obsidian.xposed.hooks.systemui.StatusbarIcons;
import it.tugaia56.obsidian.xposed.hooks.systemui.StatusbarMods;

public class ModPacks {
    public static List<Class<? extends XposedMods>> getMods(String packageName) {
        List<Class<? extends XposedMods>> mods = new ArrayList<>();
        if (Constants.Packages.SYSTEM_UI.equals(packageName)) {
            mods.add(MonetFreeze.class);
            mods.add(QsBackground.class);
            mods.add(StatusbarMods.class);
            mods.add(StatusbarIcons.class);
            mods.add(StatusbarClock.class);
        }
        return mods;
    }
}

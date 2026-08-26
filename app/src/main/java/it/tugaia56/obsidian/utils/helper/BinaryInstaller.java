package it.tugaia56.obsidian.utils.helper;

import static it.tugaia56.obsidian.utils.Dynamic.AAPT2;
import static it.tugaia56.obsidian.utils.Dynamic.AAPT2LIB;
import static it.tugaia56.obsidian.utils.Dynamic.ZIPALIGN;
import static it.tugaia56.obsidian.utils.Dynamic.ZIPALIGNLIB;
import static it.tugaia56.obsidian.utils.ModuleConstants.BIN_DIR;

import com.topjohnwu.superuser.Shell;

/**
 * Collega libaapt2.so/libzipalign.so (già presenti in nativeLibraryDir grazie a
 * jniLibs.useLegacyPackaging=true) come binari eseguibili "aapt2"/"zipalign" in una
 * cartella privata dell'app — porting semplificato di OC's BinaryInstaller (qui non
 * serve la copia extra da assets/Tools: i .so sono già estratti su disco dal sistema
 * all'installazione).
 */
public class BinaryInstaller {

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void symLinkBinaries() {
        Shell.cmd("mkdir -p " + BIN_DIR).exec();
        if (AAPT2.exists()) AAPT2.delete();
        if (ZIPALIGN.exists()) ZIPALIGN.delete();
        Shell.cmd(
            "ln -sf " + AAPT2LIB.getAbsolutePath() + ' ' + AAPT2.getAbsolutePath(),
            "ln -sf " + ZIPALIGNLIB.getAbsolutePath() + ' ' + ZIPALIGN.getAbsolutePath(),
            "chmod 755 " + AAPT2.getAbsolutePath(),
            "chmod 755 " + ZIPALIGN.getAbsolutePath()
        ).exec();
    }
}

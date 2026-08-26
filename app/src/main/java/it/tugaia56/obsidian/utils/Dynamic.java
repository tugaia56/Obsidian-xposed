package it.tugaia56.obsidian.utils;

import static it.tugaia56.obsidian.utils.ModuleConstants.BIN_DIR;

import java.io.File;

import it.tugaia56.obsidian.Obsidian;

/** Percorsi degli strumenti aapt2/zipalign — porting trimmed di OC's Dynamic.java
 *  (qui servono solo per il compilatore overlay icon pack, non per i contatori
 *  di overlay tema/navbar/ecc. che Obsidian non ha). */
public class Dynamic {
    public static final String NATIVE_LIBRARY_DIR = Obsidian.getAppContext().getApplicationInfo().nativeLibraryDir;
    public static final File AAPT2LIB    = new File(NATIVE_LIBRARY_DIR, "libaapt2.so");
    public static final File AAPT2       = new File(BIN_DIR, "aapt2");
    public static final File ZIPALIGNLIB = new File(NATIVE_LIBRARY_DIR, "libzipalign.so");
    public static final File ZIPALIGN    = new File(BIN_DIR, "zipalign");
}

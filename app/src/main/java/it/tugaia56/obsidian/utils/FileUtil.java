package it.tugaia56.obsidian.utils;

import android.content.Context;

import com.topjohnwu.superuser.Shell;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import it.tugaia56.obsidian.Obsidian;

/** Porting trimmed di OC's FileUtil — solo quanto serve al compilatore overlay
 *  (estrarre un asset folder nella cartella dati dell'app). */
public class FileUtil {

    public static final String DATA_DIR = Obsidian.getAppContext().getFilesDir().toString();

    public static void copyAssets(String assetFolder) throws IOException {
        cleanDir(assetFolder);
        new File(DATA_DIR + "/" + assetFolder + "/").mkdirs();
        copyFileOrDirectory(Obsidian.getAppContext(), assetFolder, DATA_DIR + "/" + assetFolder);
    }

    public static void cleanDir(String dirName) {
        Shell.cmd("rm -rf " + DATA_DIR + "/" + dirName).exec();
    }

    private static void copyFileOrDirectory(Context context, String dirName, String outPath) throws IOException {
        String[] srcFiles = context.getAssets().list(dirName);
        if (srcFiles == null) return;

        for (String srcFileName : srcFiles) {
            String outFileName = outPath + File.separator + srcFileName;
            String inFileName = dirName.isEmpty() ? srcFileName : dirName + File.separator + srcFileName;
            try (InputStream inputStream = context.getAssets().open(inFileName)) {
                copyAndClose(inputStream, Files.newOutputStream(Paths.get(outFileName)));
            } catch (IOException e) {
                new File(outFileName).mkdir();
                copyFileOrDirectory(context, inFileName, outFileName);
            }
        }
    }

    private static void copyAndClose(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[8192];
        int n;
        while ((n = input.read(buffer)) != -1) output.write(buffer, 0, n);
        input.close();
        output.close();
    }
}

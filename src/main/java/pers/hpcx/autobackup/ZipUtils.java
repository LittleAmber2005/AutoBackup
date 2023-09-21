package pers.hpcx.autobackup;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public final class ZipUtils {
    
    public static final int BUFFER_SIZE = 8192;
    
    private ZipUtils() {
    }
    
    public static void zip(File[] srcFiles, File zipFile) throws IOException {
        makeFile(zipFile);
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile), StandardCharsets.UTF_8);
        BufferedOutputStream bos = new BufferedOutputStream(zos);
        try (zos; bos) {
            for (File srcFile : srcFiles) {
                putEntry(srcFile, "", zos, bos);
            }
        }
    }
    
    public static void unzip(File srcFile, File unzipDir) throws IOException {
        ZipFile zipFile = new ZipFile(srcFile, StandardCharsets.UTF_8);
        try (zipFile) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                File file = new File(unzipDir.getAbsolutePath() + File.separator + entry.getName());
                if (entry.isDirectory()) {
                    if (!file.exists() && !file.mkdirs()) {
                        throw new IOException("Can not make directory " + file.getAbsolutePath());
                    }
                } else {
                    makeFile(file);
                    InputStream is = zipFile.getInputStream(entry);
                    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
                    try (is; bos) {
                        transfer(is, bos);
                    }
                }
            }
        }
    }
    
    private static void makeFile(File file) throws IOException {
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (!parent.exists() && !parent.mkdirs() || !file.createNewFile()) {
                throw new IOException("Can not make file " + file.getAbsolutePath());
            }
        }
    }
    
    private static void putEntry(File srcFile, String parent, ZipOutputStream zos, BufferedOutputStream bos) throws IOException {
        if (!srcFile.exists()) {
            return;
        }
        File[] files = srcFile.listFiles();
        if (files != null) {
            parent += srcFile.getName() + File.separator;
            for (File file : files) {
                putEntry(file, parent, zos, bos);
            }
        } else {
            zos.putNextEntry(new ZipEntry(parent + srcFile.getName()));
            try (InputStream is = new BufferedInputStream(new FileInputStream(srcFile))) {
                transfer(is, bos);
            }
            zos.closeEntry();
        }
    }
    
    private static void transfer(InputStream is, OutputStream os) throws IOException {
        int len;
        byte[] buf = new byte[BUFFER_SIZE];
        while ((len = is.read(buf)) >= 0) {
            os.write(buf, 0, len);
        }
        os.flush();
    }
}

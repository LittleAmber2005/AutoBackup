package pers.hpcx.autobackup;

import org.apache.commons.compress.archivers.zip.ParallelScatterZipCreator;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.parallel.InputStreamSupplier;
import org.apache.commons.io.input.NullInputStream;
import org.slf4j.Logger;

import java.io.*;
import java.util.concurrent.*;
import java.util.zip.Deflater;

public final class ZipCompressUtils {
    
    private ZipCompressUtils() {
    }
    
    public static void compress(File[] srcFiles, File zipFile, Logger logger) throws IOException, ExecutionException, InterruptedException {
        LinkedBlockingQueue<Runnable> queue = new LinkedBlockingQueue<>(32);
        ThreadFactory factory = Executors.defaultThreadFactory();
        ThreadPoolExecutor.CallerRunsPolicy handler = new ThreadPoolExecutor.CallerRunsPolicy();
        int processors = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = new ThreadPoolExecutor(processors, processors * 2, 60, TimeUnit.SECONDS, queue, factory, handler);
        compress(srcFiles, zipFile, executor, Deflater.BEST_SPEED, logger);
        executor.shutdown();
    }
    
    public static void compress(File[] srcFiles, File zipFile, ExecutorService executor, int level, Logger logger) throws IOException, ExecutionException, InterruptedException {
        makeFile(zipFile);
        FileOutputStream fos = new FileOutputStream(zipFile);
        ParallelScatterZipCreator creator = new ParallelScatterZipCreator(executor);
        try (fos; ZipArchiveOutputStream zos = new ZipArchiveOutputStream(fos)) {
            zos.setLevel(level);
            zos.setEncoding("UTF-8");
            for (File path : srcFiles) {
                addArchiveEntry(creator, path, path.getName(), logger);
            }
            creator.writeTo(zos);
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
    
    private static void addArchiveEntry(ParallelScatterZipCreator creator, File file, String path, Logger logger) {
        if (file == null) {
            return;
        }
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) {
                return;
            }
            for (File file0 : files) {
                addArchiveEntry(creator, file0, path + File.separator + file0.getName(), logger);
            }
        } else {
            InputStreamSupplier supplier = () -> {
                try {
                    return new FileInputStream(file);
                } catch (FileNotFoundException e) {
                    if (logger != null) {
                        logger.error("file not found", e);
                    }
                    return new NullInputStream(0);
                }
            };
            ZipArchiveEntry entry = new ZipArchiveEntry(path);
            entry.setMethod(ZipArchiveEntry.DEFLATED);
            entry.setSize(file.length());
            creator.addArchiveEntry(entry, supplier);
        }
    }
}

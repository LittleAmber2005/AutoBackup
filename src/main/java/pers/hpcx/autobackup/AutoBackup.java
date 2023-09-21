package pers.hpcx.autobackup;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AutoBackup implements ModInitializer, ServerLifecycleEvents.ServerStopped {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
    public static final Path GAME_DIR = FabricLoader.getInstance().getGameDir();
    public static final Path BACKUP_PATH = GAME_DIR.resolve("backup");
    public static final String[] BACKUP_FILES = {"world"};
    
    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STOPPED.register(this);
    }
    
    @Override
    public void onServerStopped(MinecraftServer server) {
        File[] srcFiles = new File[BACKUP_FILES.length];
        for (int i = 0; i < BACKUP_FILES.length; i++) {
            srcFiles[i] = GAME_DIR.resolve(BACKUP_FILES[i]).toFile();
        }
        File zipFile = BACKUP_PATH.resolve(TIME_FORMATTER.format(LocalDateTime.now()) + ".zip").toFile();
        try {
            LOGGER.info("Backup started...");
            long cost = -System.currentTimeMillis();
            ZipUtils.zip(srcFiles, zipFile);
            cost += System.currentTimeMillis();
            LOGGER.info("Backup successfully");
            LOGGER.info("File: %s, Size: %dbytes, Cost: %dms".formatted(zipFile.getName(), zipFile.length(), cost));
        } catch (IOException e) {
            LOGGER.error("Failed to backup", e);
        }
    }
}

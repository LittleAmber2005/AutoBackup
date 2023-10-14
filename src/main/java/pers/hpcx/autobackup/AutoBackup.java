package pers.hpcx.autobackup;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Predicate;

import static com.mojang.brigadier.arguments.BoolArgumentType.bool;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class AutoBackup implements ModInitializer {
    
    public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
    public static final Path GAME_DIR = FabricLoader.getInstance().getGameDir();
    public static final Path BACKUP_PATH = GAME_DIR.resolve("backup");
    public static final String[] BACKUP_FILES = {"world"};
    
    private static final Logger LOGGER = LogUtils.getLogger();
    
    public boolean enableBackup = true;
    
    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register(commandRegistrationCallback);
        ServerLifecycleEvents.SERVER_STOPPED.register(backupServer);
    }
    
    public final CommandRegistrationCallback commandRegistrationCallback = (dispatcher, registryAccess, environment) -> {
        Predicate<ServerCommandSource> isOperator = source -> source.hasPermissionLevel(4) || "Server".equals(source.getName());
        
        dispatcher.register(literal("backup").requires(isOperator).then(literal("enable").then(argument("enable", bool()).executes(context -> {
            enableBackup = BoolArgumentType.getBool(context, "enable");
            MutableText message = Text.literal("Auto backup ").formatted(Formatting.GREEN);
            if (enableBackup) {
                message.append(Text.literal("enabled").formatted(Formatting.LIGHT_PURPLE));
            } else {
                message.append(Text.literal("disabled").formatted(Formatting.GRAY));
            }
            context.getSource().sendMessage(message);
            return 1;
        }))));
    };
    
    public final ServerLifecycleEvents.ServerStopped backupServer = server -> {
        if (!enableBackup) {
            LOGGER.info("Backup skipped");
            return;
        }
        LOGGER.info("Backup started...");
        
        File zipFile = BACKUP_PATH.resolve(TIME_FORMATTER.format(LocalDateTime.now()) + ".zip").toFile();
        File[] srcFiles = new File[BACKUP_FILES.length];
        for (int i = 0; i < BACKUP_FILES.length; i++) {
            srcFiles[i] = GAME_DIR.resolve(BACKUP_FILES[i]).toFile();
        }
        
        long cost = -System.currentTimeMillis();
        try {
            ZipCompressUtils.compress(srcFiles, zipFile, LOGGER);
        } catch (Exception e) {
            LOGGER.error("Failed to backup", e);
            return;
        }
        cost += System.currentTimeMillis();
        
        LOGGER.info("Backup finished");
        LOGGER.info("Output: %s, Size: %d bytes, Cost: %d ms".formatted(zipFile.getName(), zipFile.length(), cost));
    };
}

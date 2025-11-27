package cn.lemwood.geyserupdater.fabric;

import cn.lemwood.geyserupdater.common.GeyserUpdaterCommon;
import cn.lemwood.geyserupdater.common.platform.PlatformAdapter;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static net.minecraft.server.command.CommandManager.literal;
import net.minecraft.text.Text;

import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeyserUpdaterFabric implements ModInitializer, PlatformAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger("GeyserUpdater");
    private final ExecutorService scheduler = Executors.newCachedThreadPool();
    private GeyserUpdaterCommon common;

    @Override
    public void onInitialize() {
        this.common = new GeyserUpdaterCommon(this);
        this.common.onEnable();
        
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("geyserupdater")
                .requires(source -> source.hasPermissionLevel(4)) 
                .then(literal("check")
                    .executes(context -> {
                        context.getSource().sendMessage(Text.of(
                            common.getConfig().getMessage("prefix").replace("&", "§") + "Checking for updates..."
                        ));
                        common.checkAll();
                        return 1;
                    })
                )
            );
        });
    }

    @Override
    public Path getDataDirectory() {
        return FabricLoader.getInstance().getConfigDir().resolve("GeyserUpdater");
    }

    @Override
    public void info(String message) {
        LOGGER.info(message);
    }

    @Override
    public void warn(String message) {
        LOGGER.warn(message);
    }

    @Override
    public void error(String message) {
        LOGGER.error(message);
    }

    @Override
    public void error(String message, Throwable t) {
        LOGGER.error(message, t);
    }

    @Override
    public void runAsync(Runnable runnable) {
        scheduler.submit(runnable);
    }

    @Override
    public String getModrinthLoader() {
        return "fabric";
    }

    @Override
    public Path getUpdateFolder() {
        return FabricLoader.getInstance().getGameDir().resolve("mods");
    }

    @Override
    public String getInstalledVersion(String projectId) {
        String modId = switch (projectId) {
            case "geyser" -> "geyser-fabric";
            case "floodgate" -> "floodgate";
            case "geyserextras" -> "geyserextras";
            default -> null;
        };
        
        if (modId == null) return null;
        
        return FabricLoader.getInstance().getModContainer(modId)
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse(null);
    }
}

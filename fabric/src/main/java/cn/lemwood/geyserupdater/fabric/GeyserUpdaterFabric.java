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

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;

public class GeyserUpdaterFabric implements ModInitializer, PlatformAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger("GeyserUpdater");
    private final ExecutorService scheduler = Executors.newCachedThreadPool();
    private GeyserUpdaterCommon common;
    private MinecraftServer server;

    @Override
    public void onInitialize() {
        this.common = new GeyserUpdaterCommon(this);
        this.common.onEnable();
        
        ServerLifecycleEvents.SERVER_STARTING.register(server -> this.server = server);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> this.server = null);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("geyserupdater")
                .requires(source -> source.hasPermissionLevel(4)) 
                .then(literal("check")
                    .executes(context -> {
                        context.getSource().sendMessage(Text.of(
                            common.getConfig().getMessage("prefix").replace("&", "§") + 
                            common.getConfig().getMessage("check-start").replace("&", "§")
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
    public Path getDownloadFolder(String projectId, boolean isUpdate) {
        return FabricLoader.getInstance().getGameDir().resolve("mods");
    }

    @Override
    public String getInstalledVersion(String projectId) {
        String[] possibleModIds = switch (projectId) {
            case "geyser" -> new String[]{"geyser-fabric", "geyser", "Geyser"};
            case "floodgate" -> new String[]{"floodgate", "Floodgate"};
            case "geyserextras" -> new String[]{"geyserextras", "GeyserExtras"};
            default -> new String[]{projectId};
        };
        
        for (String modId : possibleModIds) {
            var container = FabricLoader.getInstance().getModContainer(modId);
            if (container.isPresent()) {
                return container.get().getMetadata().getVersion().getFriendlyString();
            }
        }
        
        return null;
    }

    @Override
    public boolean compareVersion(String projectId, String remoteVersion) {
        String[] possibleModIds = switch (projectId) {
            case "geyser" -> new String[]{"geyser-fabric", "geyser", "Geyser"};
            case "floodgate" -> new String[]{"floodgate", "Floodgate"};
            case "geyserextras" -> new String[]{"geyserextras", "GeyserExtras"};
            default -> new String[]{projectId};
        };
        
        for (String modId : possibleModIds) {
            var container = FabricLoader.getInstance().getModContainer(modId);
            if (container.isPresent()) {
                try {
                    Version current = container.get().getMetadata().getVersion();
                    Version remote = Version.parse(remoteVersion);
                    return current.compareTo(remote) < 0; // Update if current < remote
                } catch (VersionParsingException e) {
                    // Fallback to string comparison if parsing fails
                    return !container.get().getMetadata().getVersion().getFriendlyString().equals(remoteVersion);
                } catch (Exception e) {
                    return true;
                }
            }
        }
        
        return true; // Not found, assume update needed
    }

    @Override
    public void shutdown() {
        if (server != null) {
            server.stop(false);
        }
    }
}

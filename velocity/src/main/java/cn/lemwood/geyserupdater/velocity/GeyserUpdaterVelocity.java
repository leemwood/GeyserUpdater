package cn.lemwood.geyserupdater.velocity;

import cn.lemwood.geyserupdater.common.GeyserUpdaterCommon;
import cn.lemwood.geyserupdater.common.platform.PlatformAdapter;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.google.inject.Inject;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
        id = "geyserupdater",
        name = "GeyserUpdater",
        version = "1.0.0",
        description = "A plugin to automatically update Geyser, Floodgate, and GeyserExtras using Modrinth API.",
        authors = {"lemwood"}
)
public class GeyserUpdaterVelocity implements PlatformAdapter {
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private GeyserUpdaterCommon common;

    @Inject
    public GeyserUpdaterVelocity(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        this.common = new GeyserUpdaterCommon(this);
        this.common.onEnable();
        
        server.getCommandManager().register(
                server.getCommandManager().metaBuilder("geyserupdater").build(),
                new GeyserUpdaterCommand(common)
        );
    }

    @Override
    public Path getDataDirectory() {
        return dataDirectory;
    }

    @Override
    public void info(String message) {
        logger.info(message);
    }

    @Override
    public void warn(String message) {
        logger.warn(message);
    }

    @Override
    public void error(String message) {
        logger.error(message);
    }

    @Override
    public void error(String message, Throwable t) {
        logger.error(message, t);
    }

    @Override
    public void runAsync(Runnable runnable) {
        server.getScheduler().buildTask(this, runnable).schedule();
    }

    @Override
    public String getModrinthLoader() {
        return "velocity";
    }

    @Override
    public Path getUpdateFolder() {
        return dataDirectory.getParent(); 
    }

    @Override
    public String getInstalledVersion(String projectId) {
        String pluginId = switch (projectId) {
            case "geyser" -> "geyser";
            case "floodgate" -> "floodgate";
            case "geyserextras" -> "geyserextras";
            default -> null;
        };
        
        if (pluginId == null) return null;
        
        return server.getPluginManager().getPlugin(pluginId)
                .map(container -> container.getDescription().getVersion().orElse("unknown"))
                .orElse(null);
    }
}

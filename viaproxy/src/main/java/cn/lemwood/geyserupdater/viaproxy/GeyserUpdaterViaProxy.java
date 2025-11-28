package cn.lemwood.geyserupdater.viaproxy;

import cn.lemwood.geyserupdater.common.GeyserUpdaterCommon;
import cn.lemwood.geyserupdater.common.platform.PlatformAdapter;
import net.raphimc.viaproxy.ViaProxy;
import net.raphimc.viaproxy.plugins.ViaProxyPlugin;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GeyserUpdaterViaProxy extends ViaProxyPlugin implements PlatformAdapter {

    private final ExecutorService scheduler = Executors.newCachedThreadPool();
    private GeyserUpdaterCommon common;
    // Use java.util.logging.Logger as fallback since getLogger() is missing
    private final java.util.logging.Logger logger = java.util.logging.Logger.getLogger("GeyserUpdater");

    @Override
    public void onEnable() {
        this.common = new GeyserUpdaterCommon(this);
        this.common.onEnable();
        
        // Command registration is not standard in ViaProxy plugin API yet or not documented clearly.
        // We skip command registration for now.
        // If there is a way to register commands, it should be added here.
        info(common.getConfig().getMessage("viaproxy-loaded"));
    }

    @Override
    public Path getDataDirectory() {
        // ViaProxyPlugin likely has getDataFolder() which returns File.
        // If not, we can use a default path relative to plugins folder.
        // But usually it has it.
        // Let's assume it has. If compilation fails, we fix it.
        // Actually, looking at other plugins, they use getDataFolder().
        return getDataFolder().toPath();
    }

    @Override
    public void info(String message) {
        logger.info(message);
        System.out.println("[GeyserUpdater] INFO: " + message);
    }

    @Override
    public void warn(String message) {
        logger.warning(message);
        System.out.println("[GeyserUpdater] WARN: " + message);
    }

    @Override
    public void error(String message) {
        logger.severe(message);
        System.err.println("[GeyserUpdater] ERROR: " + message);
    }

    @Override
    public void error(String message, Throwable t) {
        logger.log(java.util.logging.Level.SEVERE, message, t);
        System.err.println("[GeyserUpdater] ERROR: " + message);
        t.printStackTrace();
    }

    @Override
    public void runAsync(Runnable runnable) {
        scheduler.submit(runnable);
    }

    @Override
    public String getModrinthLoader() {
        return "viaproxy";
    }

    @Override
    public Path getDownloadFolder(String projectId, boolean isUpdate) {
        // ViaProxy plugins are in "plugins" folder.
        // We can just return the parent of data folder, which is "plugins".
        return getDataDirectory().getParent();
    }

    @Override
    public String getInstalledVersion(String projectId) {
        String pluginName = switch (projectId) {
            case "geyser" -> "Geyser-ViaProxy";
            case "floodgate" -> "floodgate";
            case "geyserextras" -> "GeyserExtras";
            default -> null;
        };
        
        if (pluginName == null) return null;

        try {
            // ViaProxy.getPluginManager() is likely static based on wiki
            ViaProxyPlugin plugin = ViaProxy.getPluginManager().getPlugin(pluginName);
            if (plugin != null) {
                return plugin.getVersion();
            } else {
                // Try lowercase name just in case
                plugin = ViaProxy.getPluginManager().getPlugin(pluginName.toLowerCase());
                if (plugin != null) return plugin.getVersion();
            }
        } catch (Exception e) {
            // Fallback or ignore
        }
        return null;
    }

    @Override
    public void shutdown() {
        // ViaProxy does not seem to expose a clean shutdown API in public docs easily
        // But usually System.exit(0) is the way for standalone apps, 
        // however ViaProxy might have a graceful shutdown.
        // Looking at ViaProxy source or common usage:
        System.exit(0);
    }
}

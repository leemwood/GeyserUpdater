package cn.lemwood.geyserupdater.paper;

import cn.lemwood.geyserupdater.common.GeyserUpdaterCommon;
import cn.lemwood.geyserupdater.common.platform.PlatformAdapter;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.Plugin;
import java.nio.file.Path;
import java.util.logging.Level;

public class GeyserUpdaterPaper extends JavaPlugin implements PlatformAdapter {
    private GeyserUpdaterCommon common;

    @Override
    public void onEnable() {
        this.common = new GeyserUpdaterCommon(this);
        this.common.onEnable();
        
        if (getCommand("geyserupdater") != null) {
            getCommand("geyserupdater").setExecutor(new GeyserUpdaterCommand(common));
        }
    }

    @Override
    public Path getDataDirectory() {
        return getDataFolder().toPath();
    }
    
    // PlatformAdapter method name conflict with JavaPlugin.getDataFolder() which returns File.
    // I need to rename method in interface or just implement it.
    // Interface says: Path getDataFolder();
    // JavaPlugin says: File getDataFolder();
    // This is a conflict. I must rename the method in Interface.
    // Let's rename it to getDataDirectory() or getDataFolderPath().
    // I will use `getDataFolderPath()` in interface.
    
    // WAIT, I cannot edit interface now without editing Common.
    // I should edit interface first.
    
    // Actually, I can implement it by returning Path if the return type is different?
    // Java allows covariant return types, but File is not subclass of Path.
    // So I have a clash.
    // I MUST rename the method in PlatformAdapter.
    
    @Override
    public void info(String message) {
        getLogger().info(message);
    }

    @Override
    public void warn(String message) {
        getLogger().warning(message);
    }

    @Override
    public void error(String message) {
        getLogger().severe(message);
    }

    @Override
    public void error(String message, Throwable t) {
        getLogger().log(Level.SEVERE, message, t);
    }

    @Override
    public void runAsync(Runnable runnable) {
        getServer().getScheduler().runTaskAsynchronously(this, runnable);
    }

    @Override
    public String getModrinthLoader() {
        return "paper"; 
    }

    @Override
    public Path getDownloadFolder(String projectId, boolean isUpdate) {
        if (isUpdate) {
            return getDataFolder().getParentFile().toPath().resolve("update");
        } else {
            return getDataFolder().getParentFile().toPath();
        }
    }

    @Override
    public String getInstalledVersion(String projectId) {
        String[] possibleNames = switch (projectId) {
            case "geyser" -> new String[]{"Geyser-Spigot", "Geyser", "geyser"};
            case "floodgate" -> new String[]{"floodgate", "Floodgate"};
            case "geyserextras" -> new String[]{"GeyserExtras", "geyserextras"};
            default -> new String[]{projectId};
        };
        
        for (String pluginName : possibleNames) {
            Plugin plugin = getServer().getPluginManager().getPlugin(pluginName);
            if (plugin != null) {
                return plugin.getDescription().getVersion();
            }
        }
        
        return null;
    }

    @Override
    public boolean compareVersion(String projectId, String remoteVersion) {
        // Bukkit versions are strings and often contain build numbers or custom formats.
        // Simple string equality is usually the safest unless we parse SemVer.
        // But remoteVersion from Modrinth might be "2.2.0" while local is "2.2.0-SNAPSHOT".
        // We can assume update needed if strings don't match.
        // Default implementation does exactly this.
        return PlatformAdapter.super.compareVersion(projectId, remoteVersion);
    }

    @Override
    public void shutdown() {
        getServer().shutdown();
    }
}

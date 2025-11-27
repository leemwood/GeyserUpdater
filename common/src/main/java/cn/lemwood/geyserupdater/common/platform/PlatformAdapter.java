package cn.lemwood.geyserupdater.common.platform;

import java.nio.file.Path;

public interface PlatformAdapter {
    Path getDataDirectory();
    void info(String message);
    void warn(String message);
    void error(String message);
    void error(String message, Throwable t);
    void runAsync(Runnable runnable);
    
    /**
     * Returns the Modrinth loader filter for this platform.
     * e.g., "paper", "fabric", "velocity"
     */
    String getModrinthLoader();
    
    /**
     * Returns the folder where updates should be placed.
     * Usually "plugins" or "mods".
     */
    Path getUpdateFolder();

    /**
     * Gets the installed version of a project.
     * @param projectId "geyser", "floodgate", or "geyserextras"
     * @return version string or null if not installed
     */
    String getInstalledVersion(String projectId);
}

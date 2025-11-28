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
     * Returns the folder where the project file should be placed.
     * @param projectId The project ID (geyser, floodgate, etc.)
     * @param isUpdate Whether this is an update to an existing installation
     */
    Path getDownloadFolder(String projectId, boolean isUpdate);

    /**
     * Gets the installed version of a project.
     * @param projectId "geyser", "floodgate", or "geyserextras"
     * @return version string or null if not installed
     */
    String getInstalledVersion(String projectId);

    /**
     * Shuts down the server.
     */
    void shutdown();
}

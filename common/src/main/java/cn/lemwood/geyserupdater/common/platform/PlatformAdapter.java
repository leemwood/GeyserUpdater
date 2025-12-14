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
     * Checks if a project is installed using platform API.
     * @param projectId "geyser", "floodgate", or "geyserextras"
     * @return true if installed, false otherwise
     */
    default boolean isProjectInstalled(String projectId) {
        return getInstalledVersion(projectId) != null;
    }

    /**
     * Compares the installed version with the remote version.
     * @param projectId The project ID
     * @param remoteVersion The version string from remote API
     * @return true if the versions are different (update needed), false otherwise
     */
    default boolean compareVersion(String projectId, String remoteVersion) {
        String installed = getInstalledVersion(projectId);
        if (installed == null) return true; // Assume update needed if unknown
        return !installed.equals(remoteVersion);
    }

    /**
     * Finds the installed jar file for a project in the given directory.
     * @param projectId The project ID
     * @param searchDir The directory to search in
     * @return Path to the found jar file, or null if not found
     */
    default Path findInstalledJar(String projectId, Path searchDir) {
        String[] possiblePrefixes = switch (projectId) {
            case "geyser" -> new String[]{"Geyser", "geyser"};
            case "floodgate" -> new String[]{"floodgate", "Floodgate"};
            case "geyserextras" -> new String[]{"GeyserExtras", "geyserextras"};
            default -> new String[]{projectId};
        };
        
        try {
            return java.nio.file.Files.list(searchDir)
                .filter(p -> p.toString().endsWith(".jar"))
                .filter(p -> {
                    String name = p.getFileName().toString().toLowerCase();
                    return java.util.Arrays.stream(possiblePrefixes)
                        .anyMatch(prefix -> name.startsWith(prefix.toLowerCase()));
                })
                .findFirst()
                .orElse(null);
        } catch (java.io.IOException e) {
            return null;
        }
    }

    /**
     * Shuts down the server.
     */
    void shutdown();
}

package cn.lemwood.geyserupdater.common;

import cn.lemwood.geyserupdater.common.config.ConfigManager;
import cn.lemwood.geyserupdater.common.modrinth.ModrinthClient;
import cn.lemwood.geyserupdater.common.platform.PlatformAdapter;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;

public class GeyserUpdaterCommon {
    private final PlatformAdapter platform;
    private final ConfigManager config;
    private final ModrinthClient client;
    private final String[] PROJECTS = {"geyser", "floodgate", "geyserextras"};

    public GeyserUpdaterCommon(PlatformAdapter platform) {
        this.platform = platform;
        this.config = new ConfigManager(platform.getDataDirectory());
        this.client = new ModrinthClient(platform, config);
    }

    public void onEnable() {
        config.load();
        
        if (!"MANUAL".equalsIgnoreCase(config.getUpdateStrategy())) {
            platform.runAsync(this::checkAll);
        }
    }

    public void checkAll() {
        for (String project : PROJECTS) {
            String installed = platform.getInstalledVersion(project);
            if (installed == null) {
                if (config.isDebug()) {
                    platform.info(project + " is not installed, skipping.");
                }
                continue;
            }
            
            checkProject(project, installed);
        }
    }

    private void checkProject(String project, String installedVersion) {
        platform.info("Checking updates for " + project + "...");
        client.getLatestVersion(project).thenAccept(version -> {
            if (version == null) return;
            
            // Normalize versions for comparison if possible, but simple inequality is safer to detect change
            if (!version.versionNumber.equals(installedVersion)) {
                platform.info(config.getMessage("update-found")
                        .replace("{project}", project)
                        .replace("{version}", version.versionNumber));
                
                if ("AUTO".equalsIgnoreCase(config.getUpdateStrategy())) {
                    downloadUpdate(project, version);
                }
            } else {
                 if (config.isDebug()) {
                     platform.info(config.getMessage("no-update").replace("{project}", project));
                 }
            }
        });
    }

    public void downloadUpdate(String project, ModrinthClient.ModrinthVersion version) {
        platform.info(config.getMessage("downloading").replace("{project}", project));
        
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(version.downloadUrl)).build();
        
        httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        Path tempFile = null;
                        try (InputStream in = response.body()) {
                            tempFile = Files.createTempFile("geyserupdater-", ".tmp");
                            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
                            
                            Path updateFolder = platform.getUpdateFolder();
                            Path target = updateFolder.resolve(version.filename);
                            Files.createDirectories(target.getParent());
                            
                            try {
                                Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
                                platform.info(config.getMessage("success").replace("{project}", project));
                            } catch (java.io.IOException e) {
                                // Fallback for file locking
                                platform.warn("Failed to overwrite file (possibly locked): " + e.getMessage());
                                Path fallback = target.resolveSibling(target.getFileName().toString() + ".new");
                                platform.info("Saving as " + fallback.getFileName() + " instead...");
                                Files.move(tempFile, fallback, StandardCopyOption.REPLACE_EXISTING);
                                platform.info("Update saved to " + fallback.getFileName() + ". Please manually replace it after server restart.");
                            }
                        } catch (Exception e) {
                            platform.error(config.getMessage("error")
                                    .replace("{project}", project)
                                    .replace("{error}", e.getMessage()), e);
                        } finally {
                             if (tempFile != null) {
                                 try { Files.deleteIfExists(tempFile); } catch (Exception ignored) {}
                             }
                        }
                    } else {
                        platform.error("Failed to download: " + response.statusCode());
                    }
                });
    }
    
    public ConfigManager getConfig() {
        return config;
    }
}

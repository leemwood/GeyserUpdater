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
                        try (InputStream in = response.body()) {
                            Path updateFolder = platform.getUpdateFolder();
                            
                            // If filename is different, we might end up with duplicates if we don't delete old one.
                            // But deleting old one might be impossible if locked.
                            // For Paper 'update' folder, it automatically replaces file with same name or whatever is in update folder?
                            // Actually Spigot update folder mechanism: On startup, files in 'update' are moved to 'plugins', replacing existing ones.
                            // So we should save as 'Geyser-Spigot.jar' (or whatever the plugin file is named) inside 'update' folder.
                            // But we don't know the exact filename of the installed plugin easily without scanning.
                            // However, we can use the filename from Modrinth.
                            // If Modrinth filename is 'Geyser-Spigot.jar', and installed is 'Geyser-Spigot.jar', it works.
                            // If installed is 'geyser-spigot-custom.jar', Spigot might not replace it unless we map it.
                            // But usually people keep standard names.
                            
                            Path target = updateFolder.resolve(version.filename);
                            Files.createDirectories(target.getParent());
                            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                            
                            platform.info(config.getMessage("success").replace("{project}", project));
                        } catch (Exception e) {
                            platform.error(config.getMessage("error")
                                    .replace("{project}", project)
                                    .replace("{error}", e.getMessage()), e);
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

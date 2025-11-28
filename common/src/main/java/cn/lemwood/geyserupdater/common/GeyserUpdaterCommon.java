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
import java.nio.charset.Charset;

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
                if (config.isAutoInstallEnabled(project)) {
                    if (config.isDebug()) {
                        platform.info(project + " is not installed, but auto-install is enabled. Checking for latest version...");
                    }
                    checkProject(project, null);
                } else if (config.isDebug()) {
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
            if (installedVersion == null || !version.versionNumber.equals(installedVersion)) {
                if (installedVersion == null) {
                    platform.info("Found latest version for " + project + ": " + version.versionNumber);
                } else {
                    platform.info(config.getMessage("update-found")
                            .replace("{project}", project)
                            .replace("{version}", version.versionNumber));
                }
                
                if ("AUTO".equalsIgnoreCase(config.getUpdateStrategy())) {
                    downloadUpdate(project, version, installedVersion != null);
                }
            } else {
                 if (config.isDebug()) {
                     platform.info(config.getMessage("no-update").replace("{project}", project));
                 }
            }
        });
    }

    public void downloadUpdate(String project, ModrinthClient.ModrinthVersion version, boolean isUpdate) {
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
                            
                            Path target;
                            if (isUpdate) {
                                target = platform.getUpdateFolder().resolve(version.filename);
                            } else {
                                // Install to plugin directory directly for fresh install
                                target = platform.getDataDirectory().getParent().resolve(version.filename);
                            }
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
                                
                                if (config.isShutdownScriptEnabled()) {
                                    createShutdownScript(fallback, target);
                                } else {
                                    platform.info("Update saved to " + fallback.getFileName() + ". Please manually replace it after server restart.");
                                }
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

    private void createShutdownScript(Path source, Path target) {
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.contains("win")) {
                createWindowsScript(source, target);
            } else {
                createUnixScript(source, target);
            }
        } catch (java.io.IOException e) {
            platform.warn("Failed to create shutdown script: " + e.getMessage());
        }
    }

    private void createWindowsScript(Path source, Path target) throws java.io.IOException {
        Path scriptPath = platform.getDataDirectory().resolve("update-geyser.bat");
        String content = "@echo off\r\n" +
                "chcp 65001 > NUL\r\n" +
                "timeout /t 5 /nobreak > NUL\r\n" +
                "move /y \"" + source.toAbsolutePath() + "\" \"" + target.toAbsolutePath() + "\"\r\n" +
                "del \"%~f0\"\r\n";
        Files.writeString(scriptPath, content);
        platform.info("Shutdown script generated: " + scriptPath.getFileName());
        platform.info("Executing this script after server shutdown will apply the update.");
        
        try {
            Runtime.getRuntime().exec("cmd /c start /min \"\" \"" + scriptPath.toAbsolutePath() + "\"");
            platform.info("Scheduled shutdown update script execution.");
        } catch (Exception e) {
            platform.warn("Failed to auto-schedule script: " + e.getMessage());
        }
    }

    private void createUnixScript(Path source, Path target) throws java.io.IOException {
        Path scriptPath = platform.getDataDirectory().resolve("update-geyser.sh");
        String content = "#!/bin/sh\n" +
                "sleep 5\n" +
                "mv -f \"" + source.toAbsolutePath() + "\" \"" + target.toAbsolutePath() + "\"\n" +
                "rm -- \"$0\"\n";
        Files.writeString(scriptPath, content);
        scriptPath.toFile().setExecutable(true);
        platform.info("Shutdown script generated: " + scriptPath.getFileName());
        
        try {
            Runtime.getRuntime().exec(new String[]{"sh", "-c", "nohup \"" + scriptPath.toAbsolutePath() + "\" > /dev/null 2>&1 &"});
            platform.info("Scheduled shutdown update script execution.");
        } catch (Exception e) {
            platform.warn("Failed to auto-schedule script: " + e.getMessage());
        }
    }
}

package cn.lemwood.geyserupdater.common;

import cn.lemwood.geyserupdater.common.api.UpdateClient;
import cn.lemwood.geyserupdater.common.config.ConfigManager;
import cn.lemwood.geyserupdater.common.geyser.GeyserDownloadClient;
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

import java.security.MessageDigest;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;
import java.util.ArrayList;

public class GeyserUpdaterCommon {
    private final PlatformAdapter platform;
    private final ConfigManager config;
    private final UpdateClient client;
    private final String[] projects;
    private final AtomicBoolean restartRequired = new AtomicBoolean(false);

    public GeyserUpdaterCommon(PlatformAdapter platform) {
        this(platform, new String[]{"geyser", "floodgate", "geyserextras"});
    }

    public GeyserUpdaterCommon(PlatformAdapter platform, String[] projects) {
        this.platform = platform;
        this.config = new ConfigManager(platform.getDataDirectory());
        
        // Select client based on platform
        if ("geyser".equals(platform.getModrinthLoader())) {
            // For Geyser Standalone extension, prefer Geyser Downloads API
            this.client = new GeyserDownloadClient(platform);
        } else {
            this.client = new ModrinthClient(platform, config);
        }
        
        this.projects = projects;
    }

    public void onEnable() {
        config.load();
        
        if (!"MANUAL".equalsIgnoreCase(config.getUpdateStrategy())) {
            platform.runAsync(this::checkAll);
        }
    }

    public void checkAll() {
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (String project : projects) {
            String installed = platform.getInstalledVersion(project);
            if (installed == null) {
                if (config.isAutoInstallEnabled(project)) {
                    if (config.isDebug()) {
                        platform.info(config.getMessage("auto-install-checking").replace("{project}", project));
                    }
                    futures.add(checkProject(project, null));
                } else if (config.isDebug()) {
                    platform.info(config.getMessage("not-installed-skipping").replace("{project}", project));
                }
                continue;
            }
            
            futures.add(checkProject(project, installed));
        }
        
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenRun(() -> {
                if (restartRequired.get()) {
                    scheduleRestart();
                }
            });
    }

    private CompletableFuture<Void> checkProject(String project, String installedVersion) {
        platform.info(config.getMessage("checking-updates").replace("{project}", project));
        return client.getLatestVersion(project).thenCompose(version -> {
            if (version == null) return CompletableFuture.completedFuture(null);
            
            // If installed version is null (missing file), we should treat it as an update if auto-install is enabled
            boolean isUpdate = installedVersion != null;
            boolean shouldDownload = false;
            
            if (!isUpdate) {
                // File missing
                platform.info(config.getMessage("found-latest")
                        .replace("{project}", project)
                        .replace("{version}", version.versionNumber));
                shouldDownload = true;
            } else if (!version.versionNumber.equals(installedVersion)) {
                // Version mismatch
                platform.info(config.getMessage("update-found")
                        .replace("{project}", project)
                        .replace("{version}", version.versionNumber));
                shouldDownload = true;
            } else {
                // Version matches string-wise, but let's verify file hash if available to be sure
                // This handles cases where version string is same but file changed (e.g. snapshots with same build num but different hash?)
                // Or if user manually replaced file with older version but kept filename?
                // Actually, if version matches, usually we skip.
                // But user mentioned "Plugin exists but still downloads".
                // Let's check SHA256 if available.
                
                // Wait, the user said "Plugin already exists, result is just requesting api to download file".
                // This implies we are re-downloading even if version matches?
                // My previous logic was:
                // if (installedVersion == null || !version.versionNumber.equals(installedVersion)) { ... download ... }
                // So if version equals, it should skip.
                
                // Perhaps installedVersion is not what we expect?
                // getInstalledVersion returns version string from plugin.yml/fabric.mod.json.
                // If the remote version is "2.2.0-SNAPSHOT" and local is "2.2.0-SNAPSHOT", it should be equal.
                
                // However, maybe for Geyser Standalone, we return null in getInstalledVersion?
                // Yes! In GeyserExtensionAdapter.java:
                // public String getInstalledVersion(String projectId) { return null; }
                // That's why it always updates!
                
                // We need to implement getInstalledVersion for Geyser Standalone or rely on hash check.
                // Since we can't easily get internal version of running Geyser (it doesn't expose it easily via API?),
                // checking the file hash on disk is the best way.
                
                // So, if installedVersion is null OR mismatch, we proceed to check hash (if file exists).
                
                shouldDownload = true;
            }
            
            if (shouldDownload) {
                if ("AUTO".equalsIgnoreCase(config.getUpdateStrategy())) {
                    // Check hash before downloading
                    if (isFileUpToDate(project, version, isUpdate)) {
                        if (config.isDebug()) {
                            platform.info(config.getMessage("no-update").replace("{project}", project));
                        }
                    } else {
                        return downloadUpdate(project, version, isUpdate);
                    }
                }
            } else {
                 if (config.isDebug()) {
                     platform.info(config.getMessage("no-update").replace("{project}", project));
                 }
            }
            return CompletableFuture.completedFuture(null);
        });
    }

    private boolean isFileUpToDate(String project, UpdateClient.UpdateVersion version, boolean isUpdate) {
        if (version.sha256 == null) return false; // No hash to compare, assume update needed
        
        // Get target file path
        // We need to know where the file IS to compare.
        // getDownloadFolder returns the folder.
        // If it's an update, we look at the running jar?
        // But running jar might be locked or different name.
        // PlatformAdapter doesn't give us the running jar path directly, only download folder.
        // But usually download folder contains the running jar or the update target.
        
        // For Geyser Standalone:
        // Geyser Core -> root
        // Extensions -> extensions folder
        
        Path targetFolder = platform.getDownloadFolder(project, isUpdate);
        // We don't know the exact filename of the INSTALLED file, only the new one.
        // But if we are updating, we assume we are replacing a file.
        // If installedVersion is null, we might still have the file on disk (e.g. Geyser Standalone case).
        
        // Let's try to find a file that looks like the project?
        // Or better, if we are in "isUpdate=true" mode, we imply something is installed.
        // But for Geyser Extension, getInstalledVersion returns null, so isUpdate=false.
        // But the file might exist.
        
        // Let's check if the target file already exists and matches hash.
        Path targetFile = targetFolder.resolve(version.filename);
        if (Files.exists(targetFile)) {
            try {
                String localHash = calculateSha256(targetFile);
                if (localHash.equalsIgnoreCase(version.sha256)) {
                    return true;
                }
            } catch (Exception e) {
                platform.warn("Failed to calculate hash for existing file: " + e.getMessage());
            }
        }
        
        return false;
    }
    
    private String calculateSha256(Path path) throws java.io.IOException, java.security.NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream fis = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192];
            int n = 0;
            while ((n = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, n);
            }
        }
        byte[] hash = digest.digest();
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public CompletableFuture<Void> downloadUpdate(String project, UpdateClient.UpdateVersion version, boolean isUpdate) {
        platform.info(config.getMessage("downloading").replace("{project}", project));
        
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(version.downloadUrl)).build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        Path tempFile = null;
                        try (InputStream in = response.body()) {
                            tempFile = Files.createTempFile("geyserupdater-", ".tmp");
                            Files.copy(in, tempFile, StandardCopyOption.REPLACE_EXISTING);
                            
                            Path target = platform.getDownloadFolder(project, isUpdate).resolve(version.filename);
                            Files.createDirectories(target.getParent());
                            
                            try {
                                Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
                                platform.info(config.getMessage("success").replace("{project}", project));
                                
                                if (config.isAutoRestartEnabled() && config.isRestartTrigger(project)) {
                                    platform.info(config.getMessage("restart-trigger").replace("{project}", project));
                                    restartRequired.set(true);
                                }
                            } catch (java.io.IOException e) {
                                // Fallback for file locking
                                platform.warn(config.getMessage("file-locked-warning").replace("{error}", e.getMessage()));
                                Path fallback = target.resolveSibling(target.getFileName().toString() + ".new");
                                platform.info(config.getMessage("saving-fallback").replace("{file}", fallback.getFileName().toString()));
                                Files.move(tempFile, fallback, StandardCopyOption.REPLACE_EXISTING);
                                
                                if (config.isShutdownScriptEnabled() || (config.isAutoRestartEnabled() && config.isRestartTrigger(project))) {
                                    if (config.isAutoRestartEnabled() && config.isRestartTrigger(project)) {
                                        restartRequired.set(true);
                                    }
                                    createShutdownScript(fallback, target);
                                } else {
                                    platform.info(config.getMessage("saved-fallback-manual").replace("{file}", fallback.getFileName().toString()));
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
                        platform.error(config.getMessage("download-failed").replace("{status}", String.valueOf(response.statusCode())));
                    }
                });
    }
    
    public ConfigManager getConfig() {
        return config;
    }

    private void scheduleRestart() {
        // This method might be called multiple times if multiple projects update.
        // We should ensure we only schedule once. 
        // But since runAsync usually runs immediately, we might want to delay the actual shutdown.
        
        // Simple implementation: Just start a delayed task.
        // But we need to handle concurrency. 
        // Actually, since we just set restartRequired = true, we can check if a task is already running?
        // For now, let's just spawn a thread that sleeps and then shuts down.
        // If another update comes in, it might spawn another thread... that's bad.
        // But Updates are sequential in checkAll loop? 
        // No, downloadUpdate is async callback.
        
        // Let's assume we only want one shutdown timer.
        // We can use a static flag or synchronized block, but restartRequired is instance field.
        // Let's just trigger it. If multiple trigger, multiple shutdowns might be called, but usually server stops on first.
        
        int delay = config.getRestartDelay();
        platform.info(config.getMessage("restart-countdown").replace("{seconds}", String.valueOf(delay)));
        
        // Use a dedicated thread instead of platform scheduler to ensure it survives shutdown init
        // and doesn't get blocked by platform shutdown logic.
        Thread restartThread = new Thread(() -> {
            try {
                Thread.sleep(delay * 1000L);
                platform.info(config.getMessage("restarting"));
                
                // If shutdown script is needed for file move, we ensure it's created.
                // createShutdownScript is called during download success if fallback is needed.
                // If fallback wasn't needed, but we want restart, we might need a script ONLY for restart?
                // The user requirement says "Provide an auto restart script".
                // If we rely on the user having a loop script, we just need to stop.
                
                // However, if "restart-script" is configured in config.yml, we should try to run it?
                // If the server is running in a loop, stopping it is enough.
                // If "restart-script" is configured, it means the user wants us to run it.
                // But we can't run a start script while server is running (port bind error).
                // We must run it AFTER shutdown.
                // So we MUST generate/use the update-geyser script to chain the start command.
                
                if (!config.getRestartScript().isEmpty()) {
                     // If we didn't create a script yet (because no file lock issue), we should create one now just for restart?
                     // Or we can update existing script logic.
                     // But createShutdownScript takes source/target for file move.
                     // We need a generic createRestartScript.
                     createRestartScript();
                }
                
                platform.shutdown();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "GeyserUpdater-Restart-Thread");
        
        restartThread.setDaemon(false); // Ensure thread keeps running until it finishes (shuts down server)
        restartThread.start();
    }

    private void createRestartScript() {
        // Only needed if we have a restart command to chain and we haven't created a script yet?
        // Or just overwrite it? 
        // If we created a script for file move, it's already there.
        // We need to append to it?
        // The current createShutdownScript overwrites the file.
        // This is tricky.
        
        // Let's simplify:
        // If file move was needed, createShutdownScript was called.
        // If we want to support restart script chaining, createShutdownScript needs to know about it.
        // So we should modify createShutdownScript to include restart logic.
        
        // But what if file move was NOT needed, but we still want to use restart-script?
        // Then we need to create a dummy script that just runs the restart command.
        
        Path scriptPath = platform.getDataDirectory().resolve(
            System.getProperty("os.name").toLowerCase().contains("win") ? "update-geyser.bat" : "update-geyser.sh"
        );
        
        if (!Files.exists(scriptPath)) {
             // Create a script that just executes the restart command
             try {
                 if (System.getProperty("os.name").toLowerCase().contains("win")) {
                     createWindowsRestartScript(scriptPath);
                 } else {
                     createUnixRestartScript(scriptPath);
                 }
             } catch (java.io.IOException e) {
                 platform.warn(config.getMessage("script-creation-failed").replace("{error}", e.getMessage()));
             }
        }
    }

    private void createWindowsRestartScript(Path scriptPath) throws java.io.IOException {
        String restartCmd = config.getRestartScript();
        if (restartCmd.isEmpty()) return;
        
        String content = "@echo off\r\n" +
                "chcp 65001 > NUL\r\n" +
                "timeout /t 5 /nobreak > NUL\r\n" +
                "call " + restartCmd + "\r\n" +
                "del \"%~f0\"\r\n";
        Files.writeString(scriptPath, content);
        scheduleScriptExecution(scriptPath, true);
    }

    private void createUnixRestartScript(Path scriptPath) throws java.io.IOException {
        String restartCmd = config.getRestartScript();
        if (restartCmd.isEmpty()) return;

        String content = "#!/bin/sh\n" +
                "sleep 5\n" +
                restartCmd + "\n" +
                "rm -- \"$0\"\n";
        Files.writeString(scriptPath, content);
        scriptPath.toFile().setExecutable(true);
        scheduleScriptExecution(scriptPath, false);
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
            platform.warn(config.getMessage("script-creation-failed").replace("{error}", e.getMessage()));
        }
    }

    private void createWindowsScript(Path source, Path target) throws java.io.IOException {
        Path scriptPath = platform.getDataDirectory().resolve("update-geyser.bat");
        String restartCmd = config.getRestartScript();
        
        String content = "@echo off\r\n" +
                "chcp 65001 > NUL\r\n" +
                "timeout /t 5 /nobreak > NUL\r\n" +
                "move /y \"" + source.toAbsolutePath() + "\" \"" + target.toAbsolutePath() + "\"\r\n";
        
        if (!restartCmd.isEmpty() && config.isAutoRestartEnabled()) {
            content += "call " + restartCmd + "\r\n";
        }
        
        content += "del \"%~f0\"\r\n";
        
        Files.writeString(scriptPath, content);
        platform.info(config.getMessage("script-generated").replace("{file}", scriptPath.getFileName().toString()));
        platform.info(config.getMessage("script-instructions"));
        
        scheduleScriptExecution(scriptPath, true);
    }

    private void createUnixScript(Path source, Path target) throws java.io.IOException {
        Path scriptPath = platform.getDataDirectory().resolve("update-geyser.sh");
        String restartCmd = config.getRestartScript();
        
        String content = "#!/bin/sh\n" +
                "sleep 5\n" +
                "mv -f \"" + source.toAbsolutePath() + "\" \"" + target.toAbsolutePath() + "\"\n";
                
        if (!restartCmd.isEmpty() && config.isAutoRestartEnabled()) {
            content += restartCmd + "\n";
        }
        
        content += "rm -- \"$0\"\n";
        
        Files.writeString(scriptPath, content);
        scriptPath.toFile().setExecutable(true);
        platform.info(config.getMessage("script-generated").replace("{file}", scriptPath.getFileName().toString()));
        
        scheduleScriptExecution(scriptPath, false);
    }
    
    private void scheduleScriptExecution(Path scriptPath, boolean isWindows) {
        try {
            if (isWindows) {
                Runtime.getRuntime().exec("cmd /c start /min \"\" \"" + scriptPath.toAbsolutePath() + "\"");
            } else {
                Runtime.getRuntime().exec(new String[]{"sh", "-c", "nohup \"" + scriptPath.toAbsolutePath() + "\" > /dev/null 2>&1 &"});
            }
            platform.info(config.getMessage("script-scheduled"));
        } catch (Exception e) {
            platform.warn(config.getMessage("script-schedule-failed").replace("{error}", e.getMessage()));
        }
    }
}

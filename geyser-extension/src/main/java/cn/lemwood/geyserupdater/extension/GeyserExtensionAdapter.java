package cn.lemwood.geyserupdater.extension;

import cn.lemwood.geyserupdater.common.platform.PlatformAdapter;
import org.geysermc.geyser.api.extension.Extension;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import org.geysermc.geyser.api.GeyserApi;

public class GeyserExtensionAdapter implements PlatformAdapter {
    private final Extension extension;

    public GeyserExtensionAdapter(Extension extension) {
        this.extension = extension;
    }

    @Override
    public Path getDataDirectory() {
        return extension.dataFolder();
    }

    @Override
    public void info(String message) {
        extension.logger().info(message);
    }

    @Override
    public void warn(String message) {
        extension.logger().warning(message);
    }

    @Override
    public void error(String message) {
        extension.logger().severe(message);
    }

    @Override
    public void error(String message, Throwable t) {
        extension.logger().severe(message);
        t.printStackTrace();
    }

    @Override
    public void runAsync(Runnable runnable) {
        CompletableFuture.runAsync(runnable);
    }

    @Override
    public String getModrinthLoader() {
        return "geyser";
    }

    @Override
    public Path getDownloadFolder(String projectId, boolean isUpdate) {
        if ("geyser".equals(projectId)) {
            // Geyser Core is in the root directory
            return extension.dataFolder().getParent().getParent();
        } else {
            // Extensions are in the extensions directory
            return extension.dataFolder().getParent();
        }
    }

    @Override
    public String getInstalledVersion(String projectId) {
        // 目前无法轻易获取 Geyser Standalone 的确切版本号（API限制）
        // 返回 null 将触发更新检查
        return null;
    }

    @Override
    public boolean compareVersion(String projectId, String remoteVersion) {
        // For Geyser Standalone, we don't have a reliable way to get installed version.
        // So we always return true to force a hash check.
        return true;
    }

    @Override
    public void shutdown() {
        // GeyserApi does not have shutdown() in some versions?
        // It might be System.exit(0) for standalone or we need to find the specific method.
        // Geyser Standalone uses a command loop. "stop" command triggers shutdown.
        // We can't inject commands easily via API maybe?
        // Actually, System.exit(0) is what "stop" command eventually does.
        System.exit(0);
    }
}

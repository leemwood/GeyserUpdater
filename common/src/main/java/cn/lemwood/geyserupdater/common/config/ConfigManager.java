package cn.lemwood.geyserupdater.common.config;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.LinkedHashMap;

public class ConfigManager {
    private final Path configPath;
    private final Path messagesPath;
    private Map<String, Object> config;
    private Map<String, Object> messages;

    public ConfigManager(Path dataFolder) {
        this.configPath = dataFolder.resolve("config.yml");
        this.messagesPath = dataFolder.resolve("messages.yml");
    }

    public void load() {
        if (!Files.exists(configPath)) {
            saveResource("config.yml", configPath);
        }
        if (!Files.exists(messagesPath)) {
            saveResource("messages.yml", messagesPath);
        }
        
        try (InputStream in = new FileInputStream(configPath.toFile())) {
            Yaml yaml = new Yaml();
            config = yaml.load(in);
            if (config == null) config = new HashMap<>();
        } catch (Exception e) {
            e.printStackTrace();
            config = new HashMap<>();
        }

        try (InputStream in = new FileInputStream(messagesPath.toFile())) {
            Yaml yaml = new Yaml();
            messages = yaml.load(in);
            if (messages == null) messages = new HashMap<>();
        } catch (Exception e) {
            e.printStackTrace();
            messages = new HashMap<>();
        }
    }

    private void saveResource(String resourceName, Path targetPath) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourceName)) {
            if (in == null) {
                // Fallback if resource not found (should not happen if build is correct)
                if (resourceName.equals("config.yml")) saveDefaultConfigFallback();
                else if (resourceName.equals("messages.yml")) saveDefaultMessagesFallback();
                return;
            }
            Files.createDirectories(targetPath.getParent());
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveDefaultConfigFallback() {
        Map<String, Object> defaultMap = new LinkedHashMap<>();
        defaultMap.put("debug", false);
        defaultMap.put("update-strategy", "AUTO"); 
        defaultMap.put("allow-alpha", true);
        defaultMap.put("allow-beta", true);
        defaultMap.put("enable-shutdown-script", false);
        
        Map<String, Object> autoInstall = new LinkedHashMap<>();
        autoInstall.put("geyser", false);
        autoInstall.put("floodgate", false);
        autoInstall.put("geyserextras", false);
        defaultMap.put("auto-install", autoInstall);
        
        saveYaml(configPath, defaultMap);
    }

    private void saveDefaultMessagesFallback() {
        Map<String, Object> defaultMap = new LinkedHashMap<>();
        defaultMap.put("prefix", "&8[&bGeyserUpdater&8] &r");
        defaultMap.put("update-found", "&aFound new version for {project}: {version}");
        defaultMap.put("downloading", "&eDownloading {project}...");
        defaultMap.put("success", "&aDownloaded {project} successfully. Please restart server.");
        defaultMap.put("error", "&cError updating {project}: {error}");
        defaultMap.put("no-update", "&a{project} is up to date.");
        saveYaml(messagesPath, defaultMap);
    }

    private void saveYaml(Path path, Map<String, Object> data) {
        try {
            Files.createDirectories(path.getParent());
            Yaml yaml = new Yaml();
            try (FileWriter writer = new FileWriter(path.toFile())) {
                yaml.dump(data, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isDebug() {
        return (boolean) config.getOrDefault("debug", false);
    }

    public boolean isShutdownScriptEnabled() {
        return (boolean) config.getOrDefault("enable-shutdown-script", false);
    }

    public String getUpdateStrategy() {
        return (String) config.getOrDefault("update-strategy", "AUTO");
    }

    public boolean isAllowAlpha() {
        return (boolean) config.getOrDefault("allow-alpha", true);
    }

    public boolean isAllowBeta() {
        return (boolean) config.getOrDefault("allow-beta", true);
    }

    public boolean isAutoInstallEnabled(String project) {
        Object obj = config.get("auto-install");
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            return Boolean.TRUE.equals(map.get(project));
        }
        return false;
    }

    public String getMessage(String key) {
        String prefix = (String) messages.getOrDefault("prefix", "");
        String msg = (String) messages.getOrDefault(key, key);
        return (prefix + msg).replace("&", "§"); 
    }
}

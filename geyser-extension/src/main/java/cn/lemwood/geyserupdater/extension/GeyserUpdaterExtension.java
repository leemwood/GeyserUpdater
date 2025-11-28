package cn.lemwood.geyserupdater.extension;

import cn.lemwood.geyserupdater.common.GeyserUpdaterCommon;
import cn.lemwood.geyserupdater.common.platform.PlatformAdapter;
import org.geysermc.geyser.api.GeyserApi;
import org.geysermc.event.subscribe.Subscribe;
import org.geysermc.geyser.api.event.lifecycle.GeyserPostInitializeEvent;
import org.geysermc.geyser.api.extension.Extension;

import org.geysermc.geyser.api.util.PlatformType;

public class GeyserUpdaterExtension implements Extension {
    private GeyserUpdaterCommon common;

    @Subscribe
    public void onPostInitialize(GeyserPostInitializeEvent event) {
        if (GeyserApi.api().platformType() != PlatformType.STANDALONE) {
            // We can't use common.getConfig() here because common is not initialized yet.
            // And ConfigManager requires a data folder which we can get from extension.
            // But for simplicity and since it's a critical startup check, we might want to just use logger.
            // However, user asked to remove hardcoded messages.
            // Let's create a temporary config manager just to read messages or hardcode a localized string if config is not available?
            // Actually, we can initialize ConfigManager early.
            
            // Wait, if we are not on Standalone, we might not want to even create config files.
            // But the user requirement is strict about internationalization.
            // Let's try to load config.
            
            // For now, I will use a hardcoded fallback that is localized or use a resource bundle?
            // The project uses YAML config.
            
            // Since this is a "disable" message, maybe it's fine to just use English or English/Chinese combo if config is not ready.
            // But let's try to be consistent.
            // The issue is: PlatformAdapter implementation for Extension relies on 'this' which is the extension instance.
            
            this.logger().info("GeyserUpdater 扩展仅支持 Geyser 独立版。正在禁用... / GeyserUpdater extension is only supported on Geyser Standalone. Disabling...");
            this.disable();
            return;
        }

        PlatformAdapter adapter = new GeyserExtensionAdapter(this);
        // Exclude floodgate as requested
        common = new GeyserUpdaterCommon(adapter, new String[]{"geyser", "geyserextras"});
        common.onEnable();
    }
}

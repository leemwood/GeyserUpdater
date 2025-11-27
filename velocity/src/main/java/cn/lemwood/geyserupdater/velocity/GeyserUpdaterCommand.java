package cn.lemwood.geyserupdater.velocity;

import cn.lemwood.geyserupdater.common.GeyserUpdaterCommon;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;

public class GeyserUpdaterCommand implements SimpleCommand {
    private final GeyserUpdaterCommon common;

    public GeyserUpdaterCommand(GeyserUpdaterCommon common) {
        this.common = common;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (!source.hasPermission("geyserupdater.admin")) {
            source.sendMessage(Component.text("§cYou do not have permission to use this command."));
            return;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("check")) {
            source.sendMessage(Component.text(common.getConfig().getMessage("prefix").replace("&", "§") + "Checking for updates..."));
            common.checkAll();
            return;
        }

        source.sendMessage(Component.text("§cUsage: /geyserupdater check"));
    }
}

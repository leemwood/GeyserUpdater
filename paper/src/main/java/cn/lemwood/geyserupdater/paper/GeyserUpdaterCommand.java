package cn.lemwood.geyserupdater.paper;

import cn.lemwood.geyserupdater.common.GeyserUpdaterCommon;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class GeyserUpdaterCommand implements CommandExecutor {
    private final GeyserUpdaterCommon common;

    public GeyserUpdaterCommand(GeyserUpdaterCommon common) {
        this.common = common;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("geyserupdater.admin")) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("check")) {
            sender.sendMessage(common.getConfig().getMessage("prefix").replace("&", "§") + "Checking for updates...");
            common.checkAll();
            return true;
        }

        sender.sendMessage("§cUsage: /geyserupdater check");
        return true;
    }
}

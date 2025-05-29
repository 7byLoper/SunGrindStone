package ru.loper.sungrindstone.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.command.AdvancedSmartCommandExecutor;
import ru.loper.suncore.api.command.SmartCommandExecutor;
import ru.loper.sungrindstone.command.impl.ReloadSubCommand;
import ru.loper.sungrindstone.config.PluginConfigManager;
import ru.loper.sungrindstone.menu.GrindStoneMenu;


public class GrindStoneCommand extends AdvancedSmartCommandExecutor {
    private final PluginConfigManager configManager;

    public GrindStoneCommand(PluginConfigManager configManager) {
        this.configManager = configManager;
        addSubCommand(new ReloadSubCommand(configManager), new Permission("sungrindstone.command.reload"), "reload");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player && player.hasPermission("sungrindstone.command.open")) {
                new GrindStoneMenu(configManager).show(player);
            }
            return true;
        }

        SmartCommandExecutor.SubCommandWrapper subCommand = this.getCommandByLabel(args[0]);
        if (subCommand == null) return true;

        if (!sender.hasPermission(subCommand.getPermission())) {
            sender.sendMessage(this.getDontPermissionMessage());
            return true;
        }

        subCommand.getCommand().onCommand(sender, args);
        return true;

    }

    @Override
    public String getDontPermissionMessage() {
        return configManager.getNoPermissionMessage();
    }
}

package ru.loper.sungrindstone.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.command.executor.BaseCommandExecutor;
import ru.loper.suncore.api.command.register.CommandRegister;
import ru.loper.sungrindstone.SunGrindStone;
import ru.loper.sungrindstone.command.impl.ReloadSubCommand;
import ru.loper.sungrindstone.config.PluginConfigManager;
import ru.loper.sungrindstone.menu.grindstone.GrindStoneMenu;

@CommandRegister(name = "grindstone", permission = "sungrindstone.command.use")
public class GrindStoneCommand extends BaseCommandExecutor {
    private final PluginConfigManager configManager;

    public GrindStoneCommand(SunGrindStone plugin) {
        this.configManager = plugin.getConfigManager();
    }

    public void handleNoArguments(@NotNull CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return;
        }

        new GrindStoneMenu(configManager).show(player);
    }

    @Override
    public String getNoPermissionMessage() {
        return configManager.getNoPermissionMessage();
    }

    @Override
    public void registerWrappers() {
        addSubCommand(new ReloadSubCommand(configManager));
    }
}

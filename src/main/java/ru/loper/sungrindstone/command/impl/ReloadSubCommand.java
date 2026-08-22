package ru.loper.sungrindstone.command.impl;

import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.command.BuildableCommand;
import ru.loper.suncore.api.command.register.SubCommandRegister;
import ru.loper.sungrindstone.config.PluginConfigManager;

@RequiredArgsConstructor
@SubCommandRegister(permission = "grindstone.command.reload", aliases = "reload")
public class ReloadSubCommand implements BuildableCommand {
    private final PluginConfigManager configManager;

    @Override
    public void handle(CommandSender commandSender, String[] args) {
        configManager.reloadAll();
        commandSender.sendMessage(configManager.getReloadMessage());
    }

    @Override
    public List<String> tabComplete(@NotNull CommandSender commandSender, String[] args) {
        return Collections.emptyList();
    }
}

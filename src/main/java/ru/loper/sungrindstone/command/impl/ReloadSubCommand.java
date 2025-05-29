package ru.loper.sungrindstone.command.impl;

import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import ru.loper.suncore.api.command.SubCommand;
import ru.loper.sungrindstone.config.PluginConfigManager;

import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public class ReloadSubCommand implements SubCommand {
    private final PluginConfigManager configManager;

    @Override
    public void onCommand(CommandSender commandSender, String[] args) {
        configManager.reloadAll();
        commandSender.sendMessage(configManager.getReloadMessage());
    }

    @Override
    public List<String> onTabCompleter(CommandSender commandSender, String[] args) {
        return Collections.emptyList();
    }
}

package ru.loper.sungrindstone;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.loper.sungrindstone.command.GrindStoneCommand;
import ru.loper.sungrindstone.config.PluginConfigManager;
import ru.loper.sungrindstone.listeners.ClickListener;

import java.util.Optional;

@Getter
public final class SunGrindStone extends JavaPlugin {
    @Getter
    private static SunGrindStone instance;
    private PluginConfigManager configManager;

    @Override
    public void onEnable() {
        instance = this;
        configManager = new PluginConfigManager(this);
        Bukkit.getPluginManager().registerEvents(new ClickListener(this), this);
        Optional.ofNullable(getCommand("grindstone")).orElseThrow().setExecutor(new GrindStoneCommand(configManager));
    }

    @Override
    public void onDisable() {
    }

}

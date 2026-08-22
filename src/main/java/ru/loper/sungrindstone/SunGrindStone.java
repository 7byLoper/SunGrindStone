package ru.loper.sungrindstone;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.loper.sungrindstone.command.GrindStoneCommand;
import ru.loper.sungrindstone.config.PluginConfigManager;
import ru.loper.sungrindstone.listeners.GrindStoneListener;

@Getter
public final class SunGrindStone extends JavaPlugin {
    @Getter
    private static SunGrindStone instance;

    private PluginConfigManager configManager;

    @Override
    public void onEnable() {
        instance = this;
        configManager = new PluginConfigManager(this);

        Bukkit.getPluginManager().registerEvents(new GrindStoneListener(this), this);

        new GrindStoneCommand(this).registerWrappers();
    }
}

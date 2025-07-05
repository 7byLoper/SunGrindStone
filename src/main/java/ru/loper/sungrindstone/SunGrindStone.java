package ru.loper.sungrindstone;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import ru.loper.sungrindstone.command.GrindStoneCommand;
import ru.loper.sungrindstone.config.PluginConfigManager;
import ru.loper.sungrindstone.listeners.GrindStoneListener;

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
        Bukkit.getPluginManager().registerEvents(new GrindStoneListener(this), this);
        Optional.ofNullable(getCommand("grindstone")).orElseThrow().setExecutor(new GrindStoneCommand(configManager));
    }

    public static void giveOrDropItem(Player player, ItemStack item) {
        if (player.getInventory().firstEmpty() != -1) {
            player.getInventory().addItem(item);
            return;
        }

        Location dropLocation = player.getLocation().add(0, 1, 0);
        player.getWorld().dropItemNaturally(dropLocation, item);
    }
}

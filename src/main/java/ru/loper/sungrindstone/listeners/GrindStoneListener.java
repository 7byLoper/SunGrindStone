package ru.loper.sungrindstone.listeners;

import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import ru.loper.sungrindstone.SunGrindStone;
import ru.loper.sungrindstone.menu.GrindStoneMenu;

@RequiredArgsConstructor
public class GrindStoneListener implements Listener {
    private final SunGrindStone plugin;

    @EventHandler(ignoreCancelled = true)
    private void onOpenInventory(InventoryOpenEvent event) {
        Inventory inventory = event.getInventory();
        Player player = (Player) event.getPlayer();

        if (!inventory.getType().equals(InventoryType.GRINDSTONE)) {
            return;
        }

        event.setCancelled(true);
        new GrindStoneMenu(plugin.getConfigManager()).show(player);
    }
}

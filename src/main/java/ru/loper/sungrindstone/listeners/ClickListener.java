package ru.loper.sungrindstone.listeners;

import lombok.RequiredArgsConstructor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import ru.loper.sungrindstone.SunGrindStone;
import ru.loper.sungrindstone.menu.GrindStoneMenu;

@RequiredArgsConstructor
public class ClickListener implements Listener {
    private final SunGrindStone plugin;

    @EventHandler(ignoreCancelled = true)
    private void onClick(PlayerInteractEvent event) {
        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (block == null) return;

        if (block.getType().equals(Material.GRINDSTONE)) {
            event.setCancelled(true);
            new GrindStoneMenu(plugin.getConfigManager()).show(player);
        }
    }
}

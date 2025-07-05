package ru.loper.sungrindstone.api.event;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ru.loper.sungrindstone.manager.GrindStoneEnchantment;
import ru.loper.sungrindstone.menu.GrindStoneMenu;

import java.util.Set;

@Getter
public class GrindStoneEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    private final GrindStoneMenu grindStoneMenu;
    private final Set<GrindStoneEnchantment> removeEnchantments;
    @Setter
    private ItemStack grindStoneItem;

    public GrindStoneEvent(GrindStoneMenu grindStoneMenu, Set<GrindStoneEnchantment> removeEnchantments,ItemStack grindStoneItem) {
        this.removeEnchantments = removeEnchantments;
        this.grindStoneMenu = grindStoneMenu;
        this.grindStoneItem = grindStoneItem.clone();
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public ItemStack getGrindStoneItem() {
        return grindStoneItem.clone();
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        cancelled = b;
    }
}

package ru.loper.sungrindstone.menu.grindstone.button;

import lombok.Getter;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import ru.loper.suncore.api.menu.button.Button;
import ru.loper.sungrindstone.manager.GrindStoneEnchantment;
import ru.loper.sungrindstone.menu.grindstone.GrindStoneMenu;

public class EnchantmentButton extends Button {
    private final GrindStoneEnchantment enchantment;
    private final GrindStoneMenu menu;

    @Getter
    private final int slot;

    public EnchantmentButton(GrindStoneEnchantment enchantment, GrindStoneMenu menu, int slot) {
        super(buildItem(enchantment, menu), slot);
        this.enchantment = enchantment;
        this.menu = menu;
        this.slot = slot;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        change();
        menu.refreshAfterEnchantmentChange(slot, enchantment);
    }

    public void change() {
        if (menu.getRemoveEnchantments().contains(enchantment)) {
            activateEnchantment();
            return;
        }

        deactivateEnchantment();
    }

    public void activateEnchantment() {
        menu.getRemoveEnchantments().remove(enchantment);
    }

    public void deactivateEnchantment() {
        menu.getRemoveEnchantments().add(enchantment);
    }

    private static ItemStack buildItem(GrindStoneEnchantment enchantment, GrindStoneMenu menu) {
        return menu.getRemoveEnchantments().contains(enchantment)
                ? enchantment.deactiveBuilder().build()
                : enchantment.activeBuilder().build();
    }
}

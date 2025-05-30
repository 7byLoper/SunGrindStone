package ru.loper.sungrindstone.menu;

import lombok.Getter;
import org.bukkit.event.inventory.InventoryClickEvent;
import ru.loper.suncore.api.gui.Button;
import ru.loper.sungrindstone.manager.GrindStoneEnchantment;

public class EnchantmentButton extends Button {
    private final GrindStoneEnchantment enchantment;
    private final GrindStoneMenu menu;
    @Getter
    private final int slot;

    public EnchantmentButton(GrindStoneEnchantment enchantment, GrindStoneMenu menu, int slot) {
        super(enchantment.activeBuilder().build(), slot);
        this.enchantment = enchantment;
        this.menu = menu;
        this.slot = slot;
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        change();
        menu.setConfirmButton();
        menu.setChangeStatusButton();
    }

    public void change() {
        if (menu.getRemoveEnchantments().contains(enchantment)) activateEnchantment();
        else deactivateEnchantment();
    }

    //Активация зачарования (выключение продажи)
    public void activateEnchantment() {
        menu.getRemoveEnchantments().remove(enchantment);
        menu.getInventory().setItem(slot, enchantment.activeBuilder().build());
    }

    //Деактивация зачарования (включение продажи)
    public void deactivateEnchantment() {
        menu.getRemoveEnchantments().add(enchantment);
        menu.getInventory().setItem(slot, enchantment.deactiveBuilder().build());
    }
}

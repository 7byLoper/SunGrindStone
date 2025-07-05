package ru.loper.sungrindstone.menu;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.loper.suncore.api.config.CustomConfig;
import ru.loper.suncore.api.gui.Button;
import ru.loper.suncore.api.gui.Menu;
import ru.loper.suncore.api.items.ItemBuilder;
import ru.loper.sungrindstone.SunGrindStone;
import ru.loper.sungrindstone.api.event.GrindStoneEvent;
import ru.loper.sungrindstone.config.ItemsConfig;
import ru.loper.sungrindstone.config.PluginConfigManager;
import ru.loper.sungrindstone.manager.GrindStoneEnchantment;

import java.util.*;
import java.util.stream.Collectors;

public class GrindStoneMenu extends Menu {
    private final PluginConfigManager configManager;
    private final CustomConfig config;
    private final List<Integer> enchantmentSlots;

    @Getter
    private final Set<GrindStoneEnchantment> removeEnchantments;
    @Getter
    private final Set<GrindStoneEnchantment> itemEnchantments;
    private final Set<EnchantmentButton> enchantmentButtons;
    private ItemStack grindStoneItem;

    public GrindStoneMenu(PluginConfigManager configManager) {
        this.configManager = configManager;
        this.config = configManager.getGrindStoneMenuConfig();
        this.enchantmentSlots = config.getConfig().getIntegerList("enchants_slots");
        this.removeEnchantments = new HashSet<>();
        this.itemEnchantments = new HashSet<>();
        this.enchantmentButtons = new HashSet<>();
    }

    @Override
    public @Nullable String getTitle() {
        return config.configMessage("title");
    }

    @Override
    public int getSize() {
        return config.getConfig().getInt("rows", 5) * 9;
    }

    @Override
    public void getItemsAndButtons() {
        loadDecoration();
        setConfirmButton();
        setChangeStatusButton();
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event) {
        int slot = event.getSlot();

        if (slot == configManager.getGrindItemSlot()) {
            handleGrindItemSlotClick(event);
            return;
        }

        buttons.stream()
                .filter(button -> button.getSlots().contains(slot))
                .findFirst()
                .ifPresent(button -> button.onClick(event));

        event.setCancelled(true);
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player player && grindStoneItem != null) {
            SunGrindStone.giveOrDropItem(player, grindStoneItem);
        }
    }

    @Override
    public void onBottomInventoryClick(@NotNull InventoryClickEvent event) {
        if (event.isShiftClick()) {
            event.setCancelled(true);
        }
    }

    private void handleGrindItemSlotClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack cursorItem = event.getCursor();
        if (cursorItem != null && !cursorItem.equals(grindStoneItem)) {
            handleNewItemPlacement(player, cursorItem, event);
        } else if (cursorItem == null && grindStoneItem != null) {
            handleItemRemoval();
        }

        updateInventoryState(player);
    }

    private void handleNewItemPlacement(Player player, ItemStack cursorItem, InventoryClickEvent event) {
        if (configManager.hasBlockedMaterial(cursorItem.getType())) {
            player.sendMessage(configManager.getBlockedMaterialMessage());
            event.setCancelled(true);
            return;
        }

        setChangeStatusButton();
        Bukkit.getScheduler().runTaskLater(SunGrindStone.getInstance(), this::loadGrindStoneItem, 1L);
    }

    private void handleItemRemoval() {
        grindStoneItem = null;
        clearEnchantSlots();
        setChangeStatusButton();
    }

    private void updateInventoryState(Player player) {
        setConfirmButton();
        Bukkit.getScheduler().runTaskLater(SunGrindStone.getInstance(), player::updateInventory, 2L);
    }

    private void loadGrindStoneItem() {
        grindStoneItem = inventory.getItem(configManager.getGrindItemSlot());
        loadEnchantmentButtons(grindStoneItem);
    }

    private void loadEnchantmentButtons(ItemStack itemStack) {
        clearEnchantSlots();

        if (itemStack == null || !itemStack.hasItemMeta() || itemStack.getEnchantments().isEmpty()) {
            return;
        }

        ItemsConfig itemsConfig = configManager.getItemsConfig();
        int index = 0;

        for (Map.Entry<Enchantment, Integer> enchantmentEntry : itemStack.getEnchantments().entrySet()) {
            if (index >= enchantmentSlots.size()) break;
            if (configManager.hasBlockedEnchant(enchantmentEntry.getKey())) continue;

            createAndAddEnchantmentButton(itemsConfig, enchantmentEntry, index++);
        }
    }

    private void createAndAddEnchantmentButton(ItemsConfig itemsConfig, Map.Entry<Enchantment, Integer> enchantmentEntry, int index) {
        String enchantName = configManager.getEnchantName(enchantmentEntry.getKey());
        String level = configManager.convertToRomanNumerals(enchantmentEntry.getValue());
        int price = configManager.getEnchantPrice(enchantmentEntry.getKey());

        String replacedActiveName = replaceEnchantPlaceholders(itemsConfig.getActiveItemName(), enchantName, level, price);
        String replacedDeactiveName = replaceEnchantPlaceholders(itemsConfig.getDeactiveItemName(), enchantName, level, price);

        List<String> replacedActiveLore = replaceLorePlaceholders(itemsConfig.getActiveItemLore(), enchantName, level, price);
        List<String> replacedDeactiveLore = replaceLorePlaceholders(itemsConfig.getDeactiveItemLore(), enchantName, level, price);

        GrindStoneEnchantment enchantment = new GrindStoneEnchantment(
                enchantmentEntry.getKey(),
                enchantName,
                level,
                price,
                new ItemBuilder(itemsConfig.getActiveBuilder().build()).name(replacedActiveName).lore(replacedActiveLore),
                new ItemBuilder(itemsConfig.getDeactiveBuilder().build()).name(replacedDeactiveName).lore(replacedDeactiveLore)
        );

        addEnchantmentButton(enchantmentSlots.get(index), enchantment);
    }

    private List<String> replaceLorePlaceholders(List<String> lore, String enchant, String level, int price) {
        return lore.stream()
                .map(line -> replaceEnchantPlaceholders(line, enchant, level, price))
                .collect(Collectors.toList());
    }

    private void addEnchantmentButton(int slot, GrindStoneEnchantment grindStoneEnchantment) {
        inventory.setItem(slot, grindStoneEnchantment.activeBuilder().build());
        itemEnchantments.add(grindStoneEnchantment);

        EnchantmentButton button = new EnchantmentButton(grindStoneEnchantment, this, slot);
        enchantmentButtons.add(button);
        buttons.add(button);
    }

    public void setChangeStatusButton() {
        ItemsConfig itemsConfig = configManager.getItemsConfig();
        int enableSlot = itemsConfig.getEnableAllItemSlot();
        int disableSlot = itemsConfig.getDisableAllItemSlot();

        removeChangeStatusButton(enableSlot, disableSlot);

        if (itemEnchantments.size() > removeEnchantments.size() || itemEnchantments.isEmpty()) {
            addDisableAllButton(itemsConfig, disableSlot);
        } else {
            addEnableAllButton(itemsConfig, enableSlot);
        }
    }

    private void addEnableAllButton(ItemsConfig itemsConfig, int slot) {
        ItemStack itemStack = itemsConfig.getDisableAllItemBuilder().build();
        inventory.setItem(slot, itemStack);

        buttons.add(new Button(itemStack, slot) {
            @Override
            public void onClick(InventoryClickEvent event) {
                enchantmentButtons.forEach(EnchantmentButton::activateEnchantment);
                setChangeStatusButton();
                setConfirmButton();
            }
        });
    }

    private void addDisableAllButton(ItemsConfig itemsConfig, int slot) {
        ItemStack itemStack = itemsConfig.getEnableAllItemBuilder().build();
        inventory.setItem(slot, itemStack);

        buttons.add(new Button(itemStack, slot) {
            @Override
            public void onClick(InventoryClickEvent event) {
                if (itemEnchantments.isEmpty()) return;
                enchantmentButtons.forEach(EnchantmentButton::deactivateEnchantment);
                setChangeStatusButton();
                setConfirmButton();
            }
        });
    }

    private void removeChangeStatusButton(int enableSlot, int disableSlot) {
        buttons.removeIf(button -> {
            boolean shouldRemove = button.getSlots().contains(enableSlot) || button.getSlots().contains(disableSlot);
            if (shouldRemove) {
                inventory.setItem(button.getSlots().get(0), null);
            }
            return shouldRemove;
        });
    }

    public void setConfirmButton() {
        ItemsConfig itemsConfig = configManager.getItemsConfig();
        int slot = removeEnchantments.isEmpty() ?
                itemsConfig.getErrorConfirmItemSlot() :
                itemsConfig.getConfirmItemSlot();

        if (removeEnchantments.isEmpty()) {
            inventory.setItem(slot, itemsConfig.getErrorConfirmBuilder().build());
            return;
        }

        createConfirmButton(itemsConfig, slot);
    }

    private void createConfirmButton(ItemsConfig itemsConfig, int slot) {
        buttons.removeIf(button -> button.getSlots().contains(slot));

        ItemBuilder builder = new ItemBuilder(itemsConfig.getConfirmBuilder().build());
        List<String> updatedLore = buildConfirmButtonLore(itemsConfig);

        buttons.add(new Button(builder.lore(updatedLore).build(), slot) {
            @Override
            public void onClick(InventoryClickEvent event) {
                handleConfirmButtonClick(event);
            }
        });

        inventory.setItem(slot, builder.build());
    }

    private List<String> buildConfirmButtonLore(ItemsConfig itemsConfig) {
        List<String> enchants = removeEnchantments.stream()
                .map(e -> configManager.getEnchantmentForm()
                        .replace("{enchant}", e.name())
                        .replace("{level}", e.level())
                        .replace("{price}", String.valueOf(e.price())))
                .toList();

        int totalPrice = removeEnchantments.stream()
                .mapToInt(GrindStoneEnchantment::price)
                .sum();

        List<String> updatedLore = new ArrayList<>();
        for (String line : itemsConfig.getConfirmItemLore()) {
            if (line.contains("{enchants}")) {
                updatedLore.addAll(enchants);
            } else {
                updatedLore.add(line.replace("{exp}", String.valueOf(totalPrice)));
            }
        }

        return updatedLore;
    }

    private void handleConfirmButtonClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player) || grindStoneItem == null) return;

        GrindStoneEvent grindStoneEvent = new GrindStoneEvent(this, removeEnchantments, grindStoneItem);
        Bukkit.getPluginManager().callEvent(grindStoneEvent);
        if (grindStoneEvent.isCancelled()) return;

        ItemStack updatedItem = grindStoneEvent.getGrindStoneItem();

        grindStoneEvent.getRemoveEnchantments().forEach(e -> {
            updatedItem.removeEnchantment(e.enchantment());
            player.giveExp(e.price());
        });

        grindStoneItem = updatedItem;
        event.getInventory().setItem(configManager.getGrindItemSlot(), updatedItem);
        loadEnchantmentButtons(updatedItem);
        setConfirmButton();
        player.updateInventory();
    }

    private void clearEnchantSlots() {
        for (EnchantmentButton button : enchantmentButtons) {
            inventory.setItem(button.getSlot(), null);
            buttons.remove(button);
        }

        removeEnchantments.clear();
        itemEnchantments.clear();
        enchantmentButtons.clear();

        setConfirmButton();
    }

    private String replaceEnchantPlaceholders(String text, String enchant, String level, int price) {
        return text.replace("{enchant}", enchant)
                .replace("{level}", level)
                .replace("{price}", String.valueOf(price));
    }

    private void loadDecoration() {
        ConfigurationSection decorSection = config.getConfig().getConfigurationSection("decor");
        if (decorSection != null) {
            addDecorFromSection(decorSection);
        }
    }
}
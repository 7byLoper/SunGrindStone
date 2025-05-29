package ru.loper.sungrindstone.menu;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.loper.suncore.api.config.CustomConfig;
import ru.loper.suncore.api.gui.Button;
import ru.loper.suncore.api.gui.Menu;
import ru.loper.suncore.api.items.ItemBuilder;
import ru.loper.sungrindstone.SunGrindStone;
import ru.loper.sungrindstone.config.ItemsConfig;
import ru.loper.sungrindstone.config.PluginConfigManager;
import ru.loper.sungrindstone.manager.GrindStoneEnchantment;

import java.util.*;
import java.util.stream.Collectors;

public class GrindStoneMenu extends Menu {
    private final PluginConfigManager configManager;
    private final CustomConfig config;
    private final List<Integer> enchantmentSlots;
    private final Set<GrindStoneEnchantment> removeEnchantments;
    private ItemStack grindStoneItem;

    public GrindStoneMenu(PluginConfigManager configManager) {
        this.configManager = configManager;
        this.removeEnchantments = new HashSet<>();
        this.config = configManager.getGrindStoneMenuConfig();
        this.enchantmentSlots = config.getConfig().getIntegerList("enchants_slots");
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
        loadInfoItem();
        loadDecoration();
        setConfirmButton();
    }

    private void loadDecoration() {
        ConfigurationSection decorSection = config.getConfig().getConfigurationSection("decor");
        if (decorSection != null) {
            addDecorFromSection(decorSection);
        }
    }

    private void loadInfoItem() {
        ItemBuilder infoItemBuilder = configManager.getItemsConfig().getInfoItemBuilder();
        if (infoItemBuilder == null) return;
        items.put(configManager.getItemsConfig().getInfoItemSlot(), infoItemBuilder.build());
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

    private void handleGrindItemSlotClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        ItemStack cursorItem = event.getCursor();

        if (cursorItem != null && !cursorItem.equals(grindStoneItem)) {
            Bukkit.getScheduler().runTaskLater(SunGrindStone.getInstance(), this::loadGrindStoneItem, 1L);
        } else if (cursorItem == null && grindStoneItem != null) {
            grindStoneItem = null;
            clearEnchantSlots();
        }

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

            String enchantName = configManager.getEnchantName(enchantmentEntry.getKey());
            String level = configManager.convertToRomanNumerals(enchantmentEntry.getValue());
            int price = configManager.getEnchantPrice(enchantmentEntry.getKey());

            String replacedActiveName = replaceEnchantPlaceholders(itemsConfig.getActiveItemName(), enchantName, level, price);
            String replacedDeactiveName = replaceEnchantPlaceholders(itemsConfig.getDeactiveItemName(), enchantName, level, price);

            List<String> replacedActiveLore = itemsConfig
                    .getActiveItemLore()
                    .stream()
                    .map(line -> replaceEnchantPlaceholders(line, enchantName, level, price))
                    .collect(Collectors.toList());

            List<String> replacedDeactiveLore = itemsConfig
                    .getDeactiveItemLore()
                    .stream()
                    .map(line -> replaceEnchantPlaceholders(line, enchantName, level, price))
                    .collect(Collectors.toList());

            GrindStoneEnchantment enchantment = new GrindStoneEnchantment(
                    enchantmentEntry.getKey(),
                    enchantName,
                    level,
                    price,
                    new ItemBuilder(itemsConfig.getActiveBuilder().build()).name(replacedActiveName).lore(replacedActiveLore),
                    new ItemBuilder(itemsConfig.getDeactiveBuilder().build()).name(replacedDeactiveName).lore(replacedDeactiveLore)
            );

            addEnchantmentButton(enchantmentSlots.get(index++), enchantment);
        }
    }

    private void addEnchantmentButton(int slot, GrindStoneEnchantment grindStoneEnchantment) {
        inventory.setItem(slot, grindStoneEnchantment.activeBuilder().build());

        buttons.add(new Button(grindStoneEnchantment.activeBuilder().build(), slot) {
            @Override
            public void onClick(InventoryClickEvent event) {
                if (removeEnchantments.contains(grindStoneEnchantment)) {
                    removeEnchantments.remove(grindStoneEnchantment);
                    inventory.setItem(slot, grindStoneEnchantment.activeBuilder().build());
                } else {
                    removeEnchantments.add(grindStoneEnchantment);
                    inventory.setItem(slot, grindStoneEnchantment.deactiveBuilder().build());
                }

                setConfirmButton();
            }
        });
    }

    private void setConfirmButton() {
        boolean noEnchantsToRemove = removeEnchantments.isEmpty();

        ItemsConfig itemsConfig = configManager.getItemsConfig();

        int slot = noEnchantsToRemove ? itemsConfig.getErrorConfirmItemSlot() : itemsConfig.getConfirmItemSlot();

        if (noEnchantsToRemove) {
            inventory.setItem(slot, itemsConfig.getErrorConfirmBuilder().build());
            return;
        }
        ItemBuilder builder = new ItemBuilder(itemsConfig.getConfirmBuilder().build());

        buttons.removeIf(button -> button.getSlots().contains(slot));

        List<String> enchants = removeEnchantments.stream()
                .map(e -> configManager.getEnchantmentForm()
                        .replace("{enchant}", e.name())
                        .replace("{level}", e.level())
                        .replace("{price}", String.valueOf(e.price())))
                .toList();

        List<String> lore = itemsConfig.getConfirmItemLore();
        List<String> updatedLore = new ArrayList<>();

        for (String line : lore) {
            if (line.contains("{enchants}")) {
                updatedLore.addAll(enchants);
            } else {
                updatedLore.add(line);
            }
        }

        buttons.add(new Button(builder.lore(updatedLore).build(), slot) {
            @Override
            public void onClick(InventoryClickEvent event) {
                if (!(event.getWhoClicked() instanceof Player player)) return;
                if (grindStoneItem == null) return;
                ItemStack updatedItem = grindStoneItem.clone();

                removeEnchantments.forEach(e -> {
                    updatedItem.removeEnchantment(e.enchantment());
                    player.giveExp(e.price());
                });

                grindStoneItem = updatedItem;
                event.getInventory().setItem(configManager.getGrindItemSlot(), updatedItem);
                loadEnchantmentButtons(updatedItem);
                setConfirmButton();

                player.updateInventory();
            }
        });

        inventory.setItem(slot, builder.build());
    }

    private void clearEnchantSlots() {
        removeEnchantments.clear();

        Iterator<Button> iterator = buttons.iterator();
        while (iterator.hasNext()) {
            Button button = iterator.next();
            if (enchantmentSlots.contains(button.getSlots().get(0))) {
                inventory.setItem(button.getSlots().get(0), null);
                iterator.remove();
            }
        }

        setConfirmButton();
    }

    @Override
    public void onBottomInventoryClick(@NotNull InventoryClickEvent event) {
        if (event.isShiftClick()) {
            event.setCancelled(true);
        }
    }

    private String replaceEnchantPlaceholders(String text, String enchant, String level, int price) {
        return text.replace("{enchant}", enchant)
                .replace("{level}", level)
                .replace("{price}", String.valueOf(price));
    }
}
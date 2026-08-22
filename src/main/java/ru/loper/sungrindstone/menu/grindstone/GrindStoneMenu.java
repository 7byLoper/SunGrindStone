package ru.loper.sungrindstone.menu.grindstone;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.loper.suncore.api.itemstack.ItemBuilder;
import ru.loper.suncore.api.menu.MenuItem;
import ru.loper.suncore.api.menu.button.Button;
import ru.loper.suncore.api.menu.impl.SyncActionMenu;
import ru.loper.suncore.api.scheduler.SchedulerServices;
import ru.loper.sungrindstone.SunGrindStone;
import ru.loper.sungrindstone.api.event.GrindStoneEvent;
import ru.loper.sungrindstone.config.PluginConfigManager;
import ru.loper.sungrindstone.config.settings.GrindStoneMenuSettings;
import ru.loper.sungrindstone.manager.GrindStoneEnchantment;
import ru.loper.sungrindstone.menu.grindstone.button.EnchantmentButton;

public class GrindStoneMenu extends SyncActionMenu {
    private static final String ACTION_CONFIRM = "confirm";

    private final PluginConfigManager configManager;
    private final GrindStoneMenuSettings grindStoneMenuSettings;

    @Getter
    private final Set<GrindStoneEnchantment> removeEnchantments = new LinkedHashSet<>();

    @Getter
    private final Set<GrindStoneEnchantment> itemEnchantments = new LinkedHashSet<>();

    private final Map<Integer, GrindStoneEnchantment> enchantmentsBySlot = new LinkedHashMap<>();
    private final Set<EnchantmentButton> enchantmentButtons = new LinkedHashSet<>();

    private ItemStack grindStoneItem;
    private Integer confirmSlot;
    private ItemBuilder emptyConfirmItemBuilder;

    public GrindStoneMenu(PluginConfigManager configManager) {
        super(configManager.getGrindStoneMenuSettings());
        this.configManager = configManager;
        this.grindStoneMenuSettings = configManager.getGrindStoneMenuSettings();
    }

    @Override
    protected Button createButton(MenuItem menuItem) {
        ItemStack item = menuItem.getItemBuilder().build();

        return switch (menuItem.getActionName()) {
            case ACTION_CONFIRM -> createConfirmButton(menuItem);
            default -> createDefault(menuItem, item);
        };
    }

    private @Nullable Button createDefault(MenuItem menuItem, ItemStack itemStack) {
        menuItems.put(menuItem.getSlot(), itemStack);
        return null;
    }

    private Button createConfirmButton(MenuItem menuItem) {
        confirmSlot = menuItem.getSlot();
        emptyConfirmItemBuilder = menuItem.getItemBuilder();

        return new Button(buildCurrentConfirmItem(), confirmSlot) {
            @Override
            public void onClick(InventoryClickEvent event) {
                handleConfirmButtonClick(event);
            }
        };
    }

    @Override
    protected void onLoad() {
        if (isNotAir(grindStoneItem)) {
            menuItems.put(configManager.getGrindItemSlot(), grindStoneItem);
        }

        loadEnchantmentButtons();
        addChangeStatusButton();
    }

    @Override
    public void onClick(@NotNull InventoryClickEvent event) {
        if (event.getSlot() == configManager.getGrindItemSlot()) {
            handleGrindItemSlotClick(event);
            return;
        }

        super.onClick(event);
    }

    @Override
    public void onClose(@NotNull InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        ItemStack itemStack = menuInventory.getItem(configManager.getGrindItemSlot());
        menuInventory.setItem(configManager.getGrindItemSlot(), null);

        grindStoneItem = null;
        clearEnchantments();

        if (isNotAir(itemStack)) {
            giveItem(player, itemStack);
        }
    }

    @Override
    public void onBottomInventoryClick(@NotNull InventoryClickEvent event) {
        if (event.getClick().isShiftClick()
                || event.getClick() == ClickType.NUMBER_KEY
                || event.getClick() == ClickType.DOUBLE_CLICK) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onDrag(@NotNull InventoryDragEvent event) {
        int topSize = event.getView().getTopInventory().getSize();
        boolean draggingIntoTop = event.getRawSlots().stream().anyMatch(slot -> slot < topSize);

        if (draggingIntoTop) {
            event.setCancelled(true);
        }
    }

    private void handleGrindItemSlotClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (event.getClick().isShiftClick()
                || event.getClick() == ClickType.NUMBER_KEY
                || event.getClick() == ClickType.DOUBLE_CLICK) {
            event.setCancelled(true);
            return;
        }

        ItemStack cursorItem = event.getCursor();
        if (isNotAir(cursorItem) && configManager.hasBlockedMaterial(cursorItem.getType())) {
            player.sendMessage(configManager.getBlockedMaterialMessage());
            event.setCancelled(true);
            return;
        }

        SchedulerServices.clientScheduler()
                .runTaskLater(
                        SunGrindStone.getInstance(),
                        () -> {
                            grindStoneItem = menuInventory.getItem(configManager.getGrindItemSlot());
                            loadGrindStoneItem(grindStoneItem);
                            updateInventory();
                            player.updateInventory();
                        },
                        1L);
    }

    private void loadGrindStoneItem(ItemStack itemStack) {
        clearEnchantments();

        if (!isNotAir(itemStack)
                || !itemStack.hasItemMeta()
                || itemStack.getEnchantments().isEmpty()) {
            return;
        }

        int index = 0;
        for (Map.Entry<Enchantment, Integer> enchantmentEntry :
                itemStack.getEnchantments().entrySet()) {
            if (index >= grindStoneMenuSettings.getEnchantsSlots().size()) {
                break;
            }

            if (configManager.hasBlockedEnchant(enchantmentEntry.getKey())) {
                continue;
            }

            int slot = grindStoneMenuSettings.getEnchantsSlots().get(index++);
            GrindStoneEnchantment enchantment = createGrindStoneEnchantment(enchantmentEntry);

            itemEnchantments.add(enchantment);
            enchantmentsBySlot.put(slot, enchantment);
        }
    }

    private GrindStoneEnchantment createGrindStoneEnchantment(Map.Entry<Enchantment, Integer> enchantmentEntry) {
        String enchantName = configManager.getEnchantName(enchantmentEntry.getKey());
        String level = configManager.convertToRomanNumerals(enchantmentEntry.getValue());
        int price = configManager.getEnchantPrice(enchantmentEntry.getKey());

        return new GrindStoneEnchantment(
                enchantmentEntry.getKey(),
                enchantName,
                level,
                price,
                buildEnchantForm(grindStoneMenuSettings.getActiveEnchantForm(), enchantName, level, price),
                buildEnchantForm(grindStoneMenuSettings.getDeactivateEnchantForm(), enchantName, level, price));
    }

    private ItemBuilder buildEnchantForm(ItemBuilder form, String enchant, String level, int price) {
        ItemBuilder builder = new ItemBuilder(form.build());

        builder.name(replaceEnchantPlaceholders(builder.name(), enchant, level, price));
        builder.lore(builder.lore().stream()
                .map(line -> replaceEnchantPlaceholders(line, enchant, level, price))
                .toList());

        return builder;
    }

    private void loadEnchantmentButtons() {
        enchantmentButtons.clear();

        enchantmentsBySlot.forEach((slot, enchantment) -> {
            EnchantmentButton button = new EnchantmentButton(enchantment, this, slot);
            enchantmentButtons.add(button);
            menuButtons.add(button);
        });
    }

    public void refreshAfterEnchantmentChange(int slot, GrindStoneEnchantment enchantment) {
        renderEnchantmentButton(slot, enchantment);
        renderConfirmButton();
        renderChangeStatusButton();
    }

    public void setConfirmButton() {
        renderConfirmButton();
    }

    public void setChangeStatusButton() {
        renderChangeStatusButton();
    }

    private void addChangeStatusButton() {
        int slot = grindStoneMenuSettings.getChangeStatusSlot();
        menuButtons.add(createChangeStatusButton(slot, buildChangeStatusItem()));
    }

    private void renderDynamicButtons() {
        enchantmentsBySlot.forEach(this::renderEnchantmentButton);
        renderConfirmButton();
        renderChangeStatusButton();
    }

    private void renderEnchantmentButton(int slot, GrindStoneEnchantment enchantment) {
        ItemStack itemStack = removeEnchantments.contains(enchantment)
                ? enchantment.deactiveBuilder().build()
                : enchantment.activeBuilder().build();

        menuInventory.setItem(slot, itemStack);
    }

    private void renderConfirmButton() {
        if (confirmSlot == null) {
            return;
        }

        removeButtonsAtSlot(confirmSlot);

        ItemStack itemStack = buildCurrentConfirmItem();
        menuInventory.setItem(confirmSlot, itemStack);

        menuButtons.add(new Button(itemStack, confirmSlot) {
            @Override
            public void onClick(InventoryClickEvent event) {
                handleConfirmButtonClick(event);
            }
        });
    }

    private void renderChangeStatusButton() {
        int slot = grindStoneMenuSettings.getChangeStatusSlot();
        removeButtonsAtSlot(slot);

        ItemStack itemStack = buildChangeStatusItem();
        menuInventory.setItem(slot, itemStack);
        menuButtons.add(createChangeStatusButton(slot, itemStack));
    }

    private ItemStack buildChangeStatusItem() {
        boolean hasNotSelectedEnchantments = itemEnchantments.size() > removeEnchantments.size();
        return hasNotSelectedEnchantments || itemEnchantments.isEmpty()
                ? grindStoneMenuSettings.getChangeStatusEnableForm().build()
                : grindStoneMenuSettings.getChangeStatusDisableForm().build();
    }

    private Button createChangeStatusButton(int slot, ItemStack itemStack) {
        return new Button(itemStack, slot) {
            @Override
            public void onClick(InventoryClickEvent event) {
                if (itemEnchantments.isEmpty()) {
                    return;
                }

                if (itemEnchantments.size() > removeEnchantments.size()) {
                    removeEnchantments.addAll(itemEnchantments);
                } else {
                    removeEnchantments.clear();
                }

                renderDynamicButtons();
            }
        };
    }

    private void removeButtonsAtSlot(int slot) {
        menuButtons.removeIf(button -> button.getSlots().contains(slot));
    }

    private ItemStack buildCurrentConfirmItem() {
        if (removeEnchantments.isEmpty()) {
            return emptyConfirmItemBuilder == null
                    ? new ItemBuilder(Material.AIR).build()
                    : emptyConfirmItemBuilder.build();
        }

        return buildConfirmItem().build();
    }

    private ItemBuilder buildConfirmItem() {
        int totalPrice = removeEnchantments.stream()
                .mapToInt(GrindStoneEnchantment::price)
                .sum();

        List<String> enchantLines = removeEnchantments.stream()
                .map(enchantment -> replaceEnchantPlaceholders(
                        grindStoneMenuSettings.getEnchantLine(),
                        enchantment.name(),
                        enchantment.level(),
                        enchantment.price()))
                .toList();

        ItemBuilder builder =
                new ItemBuilder(grindStoneMenuSettings.getConfirmItemForm().build());
        builder.name(replaceCommonPlaceholders(builder.name(), totalPrice));

        List<String> lore = new ArrayList<>();
        for (String line : builder.lore()) {
            if (line.contains("{enchants}")) {
                lore.addAll(enchantLines);
                continue;
            }

            lore.add(replaceCommonPlaceholders(line, totalPrice));
        }

        builder.lore(lore);
        return builder;
    }

    private void handleConfirmButtonClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)
                || !isNotAir(grindStoneItem)
                || removeEnchantments.isEmpty()) {
            return;
        }

        GrindStoneEvent grindStoneEvent = new GrindStoneEvent(this, removeEnchantments, grindStoneItem);
        Bukkit.getPluginManager().callEvent(grindStoneEvent);
        if (grindStoneEvent.isCancelled()) {
            return;
        }

        ItemStack updatedItem = grindStoneEvent.getGrindStoneItem();
        grindStoneEvent.getRemoveEnchantments().forEach(enchantment -> {
            updatedItem.removeEnchantment(enchantment.enchantment());
            player.giveExp(enchantment.price());
        });

        grindStoneItem = updatedItem;
        loadGrindStoneItem(updatedItem);
        updateInventory();
        player.updateInventory();
    }

    private void clearEnchantments() {
        removeEnchantments.clear();
        itemEnchantments.clear();
        enchantmentsBySlot.clear();
        enchantmentButtons.clear();
    }

    private void giveItem(Player player, ItemStack itemStack) {
        player.getInventory()
                .addItem(itemStack)
                .values()
                .forEach(leftover -> player.getWorld().dropItemNaturally(player.getLocation(), leftover));
    }

    private boolean isNotAir(ItemStack itemStack) {
        return itemStack != null && !itemStack.getType().isAir();
    }

    private String replaceCommonPlaceholders(String text, int totalPrice) {
        return text == null ? "" : text.replace("{exp}", String.valueOf(totalPrice));
    }

    private String replaceEnchantPlaceholders(String text, String enchant, String level, int price) {
        return text == null
                ? ""
                : text.replace("{enchant}", enchant)
                        .replace("{level}", level)
                        .replace("{price}", String.valueOf(price));
    }

    protected void updateInventory() {
        menuInventory.clear();
        menuButtons.clear();
        menuItems.clear();

        getItemsAndButtons();
        setInventoryItems();
    }
}

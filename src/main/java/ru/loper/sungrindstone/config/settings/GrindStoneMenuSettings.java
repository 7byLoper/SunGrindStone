package ru.loper.sungrindstone.config.settings;

import java.util.*;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import ru.loper.suncore.api.itemstack.ItemBuilder;
import ru.loper.suncore.api.menu.MenuItem;
import ru.loper.suncore.api.menu.MenuSettings;

@Getter
public class GrindStoneMenuSettings extends MenuSettings {
    private final List<Integer> enchantsSlots;
    private final String enchantLine;

    private final ItemBuilder activeEnchantForm;
    private final ItemBuilder deactivateEnchantForm;
    private final ItemBuilder confirmItemForm;

    private final ItemBuilder changeStatusDisableForm;
    private final ItemBuilder changeStatusEnableForm;
    private final int changeStatusSlot;

    public GrindStoneMenuSettings(
            String title,
            int size,
            Map<Material, List<Integer>> decoration,
            List<MenuItem> items,
            ConfigurationSection section,
            List<Integer> enchantsSlots,
            String enchantLine,
            ItemBuilder activeEnchantForm,
            ItemBuilder deactivateEnchantForm,
            ItemBuilder confirmItemForm,
            ItemBuilder changeStatusDisableForm,
            ItemBuilder changeStatusEnableForm,
            int changeStatusSlot) {
        super(title, size, decoration, items, section);
        this.enchantsSlots = enchantsSlots;
        this.enchantLine = enchantLine;
        this.activeEnchantForm = activeEnchantForm;
        this.deactivateEnchantForm = deactivateEnchantForm;
        this.confirmItemForm = confirmItemForm;
        this.changeStatusDisableForm = changeStatusDisableForm;
        this.changeStatusEnableForm = changeStatusEnableForm;
        this.changeStatusSlot = changeStatusSlot;
    }

    public static GrindStoneMenuSettings fromSection(@NotNull ConfigurationSection section) {
        final String title = section.getString("title", "");
        final int size = section.getInt("size", 9);

        final Map<Material, List<Integer>> decoration = Optional.ofNullable(
                        section.getConfigurationSection("decoration"))
                .<Map<Material, List<Integer>>>map(decorSection -> decorSection.getKeys(false).stream()
                        .collect(
                                HashMap::new,
                                (map, key) -> {
                                    Material material = Material.getMaterial(key);
                                    if (material != null) {
                                        map.put(material, decorSection.getIntegerList(key));
                                    }
                                },
                                Map::putAll))
                .orElse(new HashMap<>());

        final List<MenuItem> items = Optional.ofNullable(section.getConfigurationSection("items"))
                .<List<MenuItem>>map(decorSection -> decorSection.getKeys(false).stream()
                        .collect(
                                ArrayList::new,
                                (list, key) -> {
                                    ConfigurationSection menuItemSection = decorSection.getConfigurationSection(key);
                                    if (menuItemSection != null) {
                                        list.add(MenuItem.fromSection(menuItemSection));
                                    }
                                },
                                List::addAll))
                .orElse(new ArrayList<>());

        final ConfigurationSection settingsSection = section.getConfigurationSection("settings");
        if (settingsSection == null) {
            return new GrindStoneMenuSettings(
                    title,
                    size,
                    decoration,
                    items,
                    section,
                    List.of(),
                    "",
                    new ItemBuilder(Material.AIR),
                    new ItemBuilder(Material.AIR),
                    new ItemBuilder(Material.AIR),
                    new ItemBuilder(Material.AIR),
                    new ItemBuilder(Material.AIR),
                    4);
        }

        final List<Integer> enchantsSlots = settingsSection.getIntegerList("enchants_slots");
        final String enchantLine = settingsSection.getString("enchant_line");

        final ItemBuilder activeEnchantForm =
                ItemBuilder.fromConfig(settingsSection.getConfigurationSection("enchant_forms.active"));
        final ItemBuilder deactivateEnchantForm =
                ItemBuilder.fromConfig(settingsSection.getConfigurationSection("enchant_forms.deactivate"));
        final ItemBuilder confirmItemForm =
                ItemBuilder.fromConfig(settingsSection.getConfigurationSection("confirm_item"));

        final ItemBuilder changeStatusDisableForm =
                ItemBuilder.fromConfig(settingsSection.getConfigurationSection("change_status.disable"));
        final ItemBuilder changeStatusEnableForm =
                ItemBuilder.fromConfig(settingsSection.getConfigurationSection("change_status.enable"));
        final int changeStatusSlot = settingsSection.getInt("change_status.slot");

        return new GrindStoneMenuSettings(
                title,
                size,
                decoration,
                items,
                section,
                enchantsSlots,
                enchantLine,
                activeEnchantForm,
                deactivateEnchantForm,
                confirmItemForm,
                changeStatusDisableForm,
                changeStatusEnableForm,
                changeStatusSlot);
    }
}

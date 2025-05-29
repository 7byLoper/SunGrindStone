package ru.loper.sungrindstone.config;

import lombok.Getter;
import org.bukkit.configuration.ConfigurationSection;
import ru.loper.suncore.api.config.CustomConfig;
import ru.loper.suncore.api.items.ItemBuilder;
import ru.loper.sungrindstone.SunGrindStone;

import java.util.List;

@Getter
public class ItemsConfig {
    private final PluginConfigManager configManager;

    private ItemBuilder activeBuilder, deactiveBuilder, confirmBuilder, errorConfirmBuilder, infoItemBuilder;

    private List<String> deactiveItemLore, activeItemLore, confirmItemLore;

    private String deactiveItemName, activeItemName;

    private int confirmItemSlot, errorConfirmItemSlot, infoItemSlot;

    public ItemsConfig(PluginConfigManager configManager) {
        this.configManager = configManager;
        loadValues();
    }

    public void loadValues() {
        CustomConfig config = configManager.getGrindStoneMenuConfig();

        ConfigurationSection activeEnchantItemSection = config.getConfig().getConfigurationSection("enchant_form_active");
        ConfigurationSection deactiveEnchantItemSection = config.getConfig().getConfigurationSection("enchant_form_deactive");

        if (activeEnchantItemSection == null || deactiveEnchantItemSection == null) {
            SunGrindStone.getInstance().getLogger().severe("Ошибка при загрузке зачарований. Заполните секции 'enchant_form_active' и 'enchant_form_deactive' в 'grind_stone_menu.yml'");
        } else {
            activeBuilder = ItemBuilder.fromConfig(activeEnchantItemSection);
            deactiveBuilder = ItemBuilder.fromConfig(deactiveEnchantItemSection);

            activeItemLore = activeEnchantItemSection.getStringList("lore");
            deactiveItemLore = deactiveEnchantItemSection.getStringList("lore");

            deactiveItemName = deactiveEnchantItemSection.getString("display_name", "");
            activeItemName = activeEnchantItemSection.getString("display_name", "");
        }

        ConfigurationSection errorConfirmItemSection = config.getConfig().getConfigurationSection("confirm_item_error");
        ConfigurationSection confirmItemSection = config.getConfig().getConfigurationSection("confirm_item");


        if (errorConfirmItemSection == null || confirmItemSection == null) {
            SunGrindStone.getInstance().getLogger().severe("Ошибка при загрузке кнопки подтверждения. Проверьте конфиг 'grind_stone_menu.yml'");
        } else {
            errorConfirmBuilder = ItemBuilder.fromConfig(errorConfirmItemSection);
            confirmBuilder = ItemBuilder.fromConfig(confirmItemSection);

            confirmItemLore = confirmItemSection.getStringList("lore");

            confirmItemSlot = confirmItemSection.getInt("slot");
            errorConfirmItemSlot = errorConfirmItemSection.getInt("slot");
        }

        ConfigurationSection infoItemSection = config.getConfig().getConfigurationSection("info_item");
        if (infoItemSection != null) {
            infoItemBuilder = ItemBuilder.fromConfig(infoItemSection);

            infoItemSlot = infoItemSection.getInt("slot");
        }
    }
}

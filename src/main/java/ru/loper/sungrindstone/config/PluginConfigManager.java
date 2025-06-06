package ru.loper.sungrindstone.config;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.plugin.Plugin;
import ru.loper.suncore.api.config.ConfigManager;
import ru.loper.suncore.api.config.CustomConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class PluginConfigManager extends ConfigManager {
    private final String[] romanSymbols = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
    private Map<Enchantment, Integer> enchantmentPrices;
    private Map<Enchantment, String> enchantmentNames;

    private int defaultEnchantPrice, grindItemSlot;
    private String enchantmentForm, noPermissionMessage, reloadMessage, blockedMaterialMessage;
    private ItemsConfig itemsConfig;
    private List<Material> blockedMaterials;
    private List<Enchantment> blockedEnchants;

    public PluginConfigManager(Plugin plugin) {
        super(plugin);
    }

    @Override
    public void loadConfigs() {
        addCustomConfig(new CustomConfig("grind_stone_menu.yml", plugin));
        addCustomConfig(new CustomConfig("translation.yml", plugin));
        addCustomConfig(new CustomConfig("messages.yml", plugin));
        addCustomConfig(new CustomConfig("prices.yml", plugin));
        plugin.saveDefaultConfig();
        itemsConfig = new ItemsConfig(this);
    }

    @Override
    public void loadValues() {
        itemsConfig.loadValues();

        enchantmentPrices = new HashMap<>();
        enchantmentNames = new HashMap<>();
        blockedMaterials = new ArrayList<>();
        blockedEnchants = new ArrayList<>();

        defaultEnchantPrice = getPricesConfig().getConfig().getInt("default_price");
        grindItemSlot = getGrindStoneMenuConfig().getConfig().getInt("item_slot");
        enchantmentForm = getGrindStoneMenuConfig().configMessage("enchant_form");

        noPermissionMessage = getMessagesConfig().configMessage("no_permissions");
        blockedMaterialMessage = getMessagesConfig().configMessage("blocked_material");
        reloadMessage = getMessagesConfig().configMessage("reload");

        loadEnchantNames();
        loadEnchantPrices();
        loadBlockedEnchants();
        loadBlockedMaterials();
    }

    private void loadBlockedMaterials() {
        for (String materialName : plugin.getConfig().getStringList("blocked_materials")) {
            try {
                Material material = Material.valueOf(materialName.toUpperCase());
                blockedMaterials.add(material);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().severe("Неизвестный материал: " + materialName);
            }
        }
    }

    private void loadBlockedEnchants() {
        for (String key : plugin.getConfig().getStringList("blocked_enchants")) {
            Enchantment enchantment = getEnchantmentByName(key);

            if (enchantment == null) {
                plugin.getLogger().warning("Неизвестное зачарование: " + key);
                continue;
            }

            blockedEnchants.add(enchantment);
        }
    }

    private void loadEnchantPrices() {
        CustomConfig config = getPricesConfig();
        ConfigurationSection enchantsSection = config.getConfig().getConfigurationSection("enchants");

        if (enchantsSection == null) {
            plugin.getLogger().warning("Секция 'enchants' не найдена в prices.yml");
            return;
        }

        for (String key : enchantsSection.getKeys(false)) {
            Enchantment enchantment = getEnchantmentByName(key);

            if (enchantment == null) {
                plugin.getLogger().warning("Неизвестное зачарование: " + key);
                continue;
            }

            int price = enchantsSection.getInt(key, defaultEnchantPrice);
            enchantmentPrices.put(enchantment, price);
        }
    }

    private void loadEnchantNames() {
        CustomConfig config = getTranslationConfig();
        ConfigurationSection enchantsSection = config.getConfig().getConfigurationSection("enchantments");
        if (enchantsSection == null) {
            plugin.getLogger().warning("Секция 'enchantments' не найдена в translation.yml");
            return;
        }

        for (String key : enchantsSection.getKeys(false)) {
            Enchantment enchantment = getEnchantmentByName(key);

            if (enchantment == null) {
                plugin.getLogger().warning("Неизвестное зачарование: " + key);
                continue;
            }

            enchantmentNames.put(enchantment, enchantsSection.getString(key));
        }
    }

    private Enchantment getEnchantmentByName(String name) {
        Enchantment enchantment;

        enchantment = Enchantment.getByKey(NamespacedKey.minecraft(name.toLowerCase()));
        if (enchantment != null) return enchantment;

        enchantment = Enchantment.getByKey(NamespacedKey.fromString(name.toLowerCase()));
        if (enchantment != null) return enchantment;

        return Enchantment.getByName(name.toUpperCase());
    }

    public String getEnchantName(Enchantment enchantment) {
        return enchantmentNames.getOrDefault(enchantment, enchantment.getKey().getKey());
    }

    public CustomConfig getPricesConfig() {
        return getCustomConfig("prices.yml");
    }

    public CustomConfig getMessagesConfig() {
        return getCustomConfig("messages.yml");
    }

    public CustomConfig getGrindStoneMenuConfig() {
        return getCustomConfig("grind_stone_menu.yml");
    }

    public CustomConfig getTranslationConfig() {
        return getCustomConfig("translation.yml");
    }

    public int getEnchantPrice(Enchantment enchant) {
        return enchantmentPrices.getOrDefault(enchant, defaultEnchantPrice);
    }

    public String convertToRomanNumerals(int level) {
        if (level < 1) return "";
        if (level > 10) return String.valueOf(level);

        return romanSymbols[level - 1];
    }

    public boolean hasBlockedEnchant(Enchantment enchantment) {
        return blockedEnchants.contains(enchantment);
    }

    public boolean hasBlockedMaterial(Material material) {
        return blockedMaterials.contains(material);
    }
}

package ru.loper.sungrindstone.manager;

import org.bukkit.enchantments.Enchantment;
import ru.loper.suncore.api.items.ItemBuilder;

public record GrindStoneEnchantment(Enchantment enchantment, String name, int level, int price,
                                     ItemBuilder activeBuilder,
                                     ItemBuilder deactiveBuilder) {
}

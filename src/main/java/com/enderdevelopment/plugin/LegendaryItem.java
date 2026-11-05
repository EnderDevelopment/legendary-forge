package com.enderdevelopment.plugin;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import java.util.ArrayList;
import java.util.List;

/**
 * Data model for a legendary item.
 * Carries the item id, display material, name, lore lines and a PDC tag.
 */
public class LegendaryItem {

    private final String id;
    private final Material material;
    private final Component displayName;
    private final List<Component> lore;
    private final String abilityOne;
    private final String abilityTwo;
    private final String backstory;

    public LegendaryItem(String id, Material material, Component displayName,
                         List<Component> lore, String abilityOne, String abilityTwo, String backstory) {
        this.id = id;
        this.material = material;
        this.displayName = displayName;
        this.lore = lore;
        this.abilityOne = abilityOne;
        this.abilityTwo = abilityTwo;
        this.backstory = backstory;
    }

    public String getId() {
        return id;
    }

    public Material getMaterial() {
        return material;
    }

    public Component getDisplayName() {
        return displayName;
    }

    public List<Component> getLore() {
        return lore;
    }

    public String getAbilityOne() {
        return abilityOne;
    }

    public String getAbilityTwo() {
        return abilityTwo;
    }

    public String getBackstory() {
        return backstory;
    }

    /**
     * Builds a physical ItemStack from this definition, with a PDC tag identifying it.
     */
    public ItemStack buildItem(Main plugin) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.displayName(displayName);
        meta.lore(lore);

        // PDC identification tag
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "legendary_id"),
                org.bukkit.persistence.PersistentDataType.STRING,
                id
        );

        // Give the item an unbreakable flag and mark it as containing a custom power
        meta.setUnbreakable(true);

        item.setItemMeta(meta);
        return item;
    }
}

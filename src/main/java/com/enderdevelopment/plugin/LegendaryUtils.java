package com.enderdevelopment.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Shared helpers for particle effects, sounds and lore formatting.
 */
public final class LegendaryUtils {

    private static final Random RANDOM = new Random();

    private LegendaryUtils() {
    }

    /**
     * Builds a single-line gradient text component.
     * @param text the full plain text to colour
     * @param from start colour
     * @param to end colour
     * @return a component with a per-character gradient
     */
    public static Component gradient(String text, TextColor from, TextColor to) {
        List<Component> parts = new ArrayList<>();
        int len = text.length();
        if (len == 0) {
            return Component.empty();
        }
        for (int i = 0; i < len; i++) {
            double t = len == 1 ? 0 : (double) i / (len - 1);
            TextColor c = lerpColor(from, to, t);
            parts.add(Component.text(String.valueOf(text.charAt(i))).color(c));
        }
        return Component.empty().append(parts.toArray(new Component[0]));
    }

    private static TextColor lerpColor(TextColor a, TextColor b, double t) {
        int ar = a.red(), ag = a.green(), ab = a.blue();
        int br = b.red(), bg = b.green(), bb = b.blue();
        int r = (int) Math.round(ar + (br - ar) * t);
        int g = (int) Math.round(ag + (bg - ag) * t);
        int bl = (int) Math.round(ab + (bb - ab) * t);
        return TextColor.color(r, g, bl);
    }

    /**
     * Builds the standard Ability Lore section.
     */
    public static List<Component> abilityLore(String abilityOne, String abilityTwo) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());
        lines.add(Component.text("§8§m                                  §r"));
        lines.add(Component.text("§6§l✦ §e§lAbility Lore"));
        lines.add(Component.text("§8» §7Power I: §f" + abilityOne));
        lines.add(Component.text("§8» §7Power II: §f" + abilityTwo));
        lines.add(Component.text("§8§m                                  §r"));
        return lines;
    }

    /**
     * Builds backstory lore section.
     */
    public static List<Component> backstoryLore(String backstory) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.text("§7§o\"" + backstory + "\""));
        return lines;
    }

    // ---- Particle helpers ----

    public static void spawnParticle(Location loc, Particle particle, int count) {
        World world = loc.getWorld();
        if (world != null) {
            world.spawnParticle(particle, loc, count);
        }
    }

    public static void spawnParticle(Location loc, Particle particle, int count, double dx, double dy, double dz) {
        World world = loc.getWorld();
        if (world != null) {
            world.spawnParticle(particle, loc, count, dx, dy, dz);
        }
    }

    public static void ringParticle(Location center, Particle particle, int count, double radius) {
        World world = center.getWorld();
        if (world == null) {
            return;
        }
        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            double x = radius * Math.cos(angle);
            double z = radius * Math.sin(angle);
            Location p = center.clone().add(x, 0.1, z);
            world.spawnParticle(particle, p, 1, 0, 0, 0, 0);
        }
    }

    // ---- Sound helpers ----

    public static void playSound(Player player, Sound sound, float volume, float pitch) {
        player.playSound(player.getLocation(), sound, volume, pitch);
    }

    // ---- Misc ----

    public static boolean isLegendary(ItemStack item, Main plugin) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(
                new org.bukkit.NamespacedKey(plugin, "legendary_id"),
                org.bukkit.persistence.PersistentDataType.STRING
        );
    }

    public static String getLegendaryId(ItemStack item, Main plugin) {
        if (!isLegendary(item, plugin)) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(
                new org.bukkit.NamespacedKey(plugin, "legendary_id"),
                org.bukkit.persistence.PersistentDataType.STRING
        );
    }
}

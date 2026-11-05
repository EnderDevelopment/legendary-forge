package com.enderdevelopment.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Executes the two powers of each legendary item. Cooldowns are tracked per
 * player per ability id, with action-bar notifications.
 */
public class AbilityLogic {

    private final Main plugin;
    private final Map<UUID, Map<String, Integer>> cooldowns = new HashMap<>();
    private final Map<UUID, Integer> bloodPactTicks = new HashMap<>();
    private final Map<UUID, Integer> phaseShiftTicks = new HashMap<>();

    public AbilityLogic(Main plugin) {
        this.plugin = plugin;
        Bukkit.getScheduler().runTaskTimer(plugin, this::tickPassives, 20L, 20L);
    }

    public void shutdown() {
        Bukkit.getScheduler().cancelTasks(plugin);
    }

    private boolean onCooldown(Player player, String abilityId) {
        Map<String, Integer> map = cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>());
        Integer ticks = map.get(abilityId);
        if (ticks == null || ticks <= 0) {
            map.put(abilityId, 0);
            return false;
        }
        return true;
    }

    private String remainingText(Player player, String abilityId) {
        Map<String, Integer> map = cooldowns.get(player.getUniqueId());
        if (map == null) {
            return "";
        }
        Integer ticks = map.get(abilityId);
        if (ticks == null || ticks <= 0) {
            return "";
        }
        return String.format("%.1fs", ticks / 20.0);
    }

    private void setCooldown(Player player, String abilityId, int seconds) {
        cooldowns.computeIfAbsent(player.getUniqueId(), k -> new HashMap<>()).put(abilityId, seconds * 20);
    }

    private void notifyCooldown(Player player, String prefix, String abilityId) {
        player.sendActionBar(Component.text(prefix + " is on cooldown: " + remainingText(player, abilityId))
                .color(NamedTextColor.RED));
    }

    private void tickPassives() {
        for (Map<String, Integer> map : cooldowns.values()) {
            for (Map.Entry<String, Integer> e : map.entrySet()) {
                if (e.getValue() > 0) {
                    e.setValue(e.getValue() - 1);
                }
            }
        }
        bloodPactTicks.entrySet().removeIf(e -> {
            int n = e.getValue() - 1;
            if (n <= 0) {
                Player pl = Bukkit.getPlayer(e.getKey());
                if (pl != null) {
                    pl.removePotionEffect(PotionEffectType.STRENGTH);
                    pl.sendMessage(Component.text("Your Blood Pact has faded.").color(NamedTextColor.GRAY));
                }
                return true;
            }
            e.setValue(n);
            return false;
        });
        phaseShiftTicks.entrySet().removeIf(e -> {
            int n = e.getValue() - 1;
            if (n <= 0) {
                Player pl = Bukkit.getPlayer(e.getKey());
                if (pl != null) {
                    pl.removePotionEffect(PotionEffectType.INVISIBILITY);
                    pl.sendMessage(Component.text("Phase Shift ends.").color(NamedTextColor.GRAY));
                }
                return true;
            }
            e.setValue(n);
            return false;
        });
        for (Player player : Bukkit.getOnlinePlayers()) {
            applyArmorPassives(player);
            applyPassiveWeapon(player);
        }
    }

    private void applyArmorPassives(Player player) {
        boolean fullSet = isWearingFullSet(player);
        if (isEquipped(player, EquipmentSlot.HEAD, "shadow_helmet") && !player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 280, 0, false, false));
            LegendaryUtils.spawnParticle(player.getLocation().add(0, 1, 0), Particle.PORTAL, 4, 0.3, 0.4, 0.3);
        }
        if (isEquipped(player, EquipmentSlot.LEGS, "shadow_leggings") && !player.hasPotionEffect(PotionEffectType.SPEED)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 280, 0, false, false));
        }
        if (isEquipped(player, EquipmentSlot.CHEST, "shadow_chestplate")
                && player.getHealth() < player.getMaxHealth() * 0.4 && !player.hasPotionEffect(PotionEffectType.RESISTANCE)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 40, 0, false, false));
        }
        if (isEquipped(player, EquipmentSlot.FEET, "shadow_boots") && !player.isOnGround()) {
            LegendaryUtils.spawnParticle(player.getLocation(), Particle.WITCH, 3, 0.2, 0.2, 0.2);
        }
        if (fullSet && !player.hasPotionEffect(PotionEffectType.REGENERATION)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 120, 0, false, false));
        }
    }

    private boolean isWearingFullSet(Player player) {
        return isEquipped(player, EquipmentSlot.HEAD, "shadow_helmet")
                && isEquipped(player, EquipmentSlot.CHEST, "shadow_chestplate")
                && isEquipped(player, EquipmentSlot.LEGS, "shadow_leggings")
                && isEquipped(player, EquipmentSlot.FEET, "shadow_boots");
    }

    private boolean isEquipped(Player player, EquipmentSlot slot, String itemId) {
        ItemStack item = player.getInventory().getItem(slot);
        return itemId.equals(LegendaryUtils.getLegendaryId(item, plugin));
    }

    private String itemIdInHand(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        String id = LegendaryUtils.getLegendaryId(main, plugin);
        if (id != null) {
            return id;
        }
        return LegendaryUtils.getLegendaryId(player.getInventory().getItemInOffHand(), plugin);
    }

    private void applyPassiveWeapon(Player player) {
        String id = itemIdInHand(player);
        if ("mace_wind".equals(id)) {
            if (!player.hasPotionEffect(PotionEffectType.SPEED)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 0, false, false));
            }
            if (!player.hasPotionEffect(PotionEffectType.JUMP_BOOST)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 60, 0, false, false));
            }
        }
    }

    // ------------------------------------------------------------------
    // Dispatchers (called from the listener)
    // ------------------------------------------------------------------
    public void onRightClick(Player player) {
        String id = itemIdInHand(player);
        if (id == null) {
            return;
        }
        switch (id) {
            case "shadow_chestplate" -> triggerPhaseShift(player);
            case "shadow_leggings" -> triggerShadowStep(player);
            case "shadow_boots" -> triggerAbyssalLeap(player);
            case "mace_crimson" -> triggerEmberRing(player);
            case "mace_storm" -> triggerLightningStrike(player);
            case "mace_frost" -> triggerFrostNova(player);
            case "mace_wind" -> triggerWindBurst(player);
            case "mace_void" -> triggerVoidBlink(player);
            case "spear_sun" -> triggerSolarFlare(player);
            case "spear_storm" -> triggerHarpoonStorm(player);
            case "spear_blood" -> triggerBloodPact(player);
            case "spear_ancient" -> triggerPillarStone(player);
            case "spear_wind" -> triggerWhirlwindDash(player);
            case "voidblade" -> triggerVoidSlash(player);
            default -> { }
        }
    }

    public void onAttack(Player attacker, LivingEntity victim) {
        String id = itemIdInHand(attacker);
        if (id == null) {
            return;
        }
        switch (id) {
            case "mace_crimson" -> triggerEmberflameHit(attacker, victim);
            case "mace_storm" -> triggerChainLightning(attacker, victim);
            case "mace_frost" -> triggerFrostChillHit(attacker, victim);
            case "mace_void" -> triggerLifeDrainHit(attacker, victim);
            case "spear_sun" -> triggerSolarBurnHit(attacker, victim);
            case "spear_storm" -> triggerSpearStormchain(attacker, victim);
            case "spear_blood" -> triggerBloodBleedHit(attacker, victim);
            case "spear_ancient" -> triggerPetrifyHit(attacker, victim);
            case "spear_wind" -> triggerWindpushHit(attacker, victim);
            case "voidblade" -> triggerShadowTeleportHit(attacker, victim);
            default -> { }
        }
    }

    /**
     * Crown of the Void: when the wearer takes damage, shroud the area in a
     * brief smoke screen that blinds nearby enemies.
     */
    public void onPlayerHurt(Player player) {
        if (onCooldown(player, "retaliate_smoke")) {
            return;
        }
        setCooldown(player, "retaliate_smoke", 6);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 0.9f, 0.4f);
        LegendaryUtils.spawnParticle(player.getLocation(), Particle.SMOKE, 30, 1.2, 1.2, 1.2);
        LegendaryUtils.spawnParticle(player.getLocation(), Particle.CLOUD, 20, 1.0, 1.0, 1.0);
        for (Entity e : player.getWorld().getNearbyEntities(player.getLocation(), 5, 3, 5)) {
            if (e instanceof LivingEntity le && !le.equals(player)) {
                le.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, false, true));
                LegendaryUtils.spawnParticle(le.getLocation(), Particle.SMOKE, 12, 0.3, 0.8, 0.3);
            }
        }
        player.sendActionBar(Component.text("A smoke screen shrouds your attackers!").color(NamedTextColor.DARK_GRAY));
    }

    // ===== ARMOR POWERS =====

    private void triggerPhaseShift(Player player) {
        if (onCooldown(player, "phase_shift")) {
            notifyCooldown(player, "Phase Shift", "phase_shift");
            return;
        }
        setCooldown(player, "phase_shift", 18);
        phaseShiftTicks.put(player.getUniqueId(), 60);
        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60, 4, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 60, 0, false, false));
        LegendaryUtils.spawnParticle(player.getLocation(), Particle.PORTAL, 40, 0.5, 1, 0.5);
        LegendaryUtils.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.4f);
        player.sendMessage(Component.text("You phase shift out of harm's way.").color(NamedTextColor.LIGHT_PURPLE));
    }

    private void triggerShadowStep(Player player) {
        if (onCooldown(player, "shadow_step")) {
            notifyCooldown(player, "Shadow Step", "shadow_step");
            return;
        }
        setCooldown(player, "shadow_step", 8);
        player.setVelocity(player.getLocation().getDirection().multiply(1.6).setY(0.2));
        LegendaryUtils.spawnParticle(player.getLocation(), Particle.SMOKE, 30, 0.3, 0.3, 0.3);
        LegendaryUtils.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.8f);
        player.sendMessage(Component.text("You shadow step forward.").color(NamedTextColor.LIGHT_PURPLE));
    }

    private void triggerAbyssalLeap(Player player) {
        if (onCooldown(player, "abyssal_leap")) {
            notifyCooldown(player, "Abyssal Leap", "abyssal_leap");
            return;
        }
        setCooldown(player, "abyssal_leap", 10);
        player.setVelocity(new Vector(0, 1.3, 0));
        LegendaryUtils.spawnParticle(player.getLocation(), Particle.PORTAL, 20, 0.2, 0.2, 0.2);
        LegendaryUtils.playSound(player, Sound.BLOCK_ENDER_CHEST_OPEN, 1.0f, 0.7f);
        player.sendMessage(Component.text("You leap into the air.").color(NamedTextColor.LIGHT_PURPLE));
    }

    // ===== MACE POWERS =====

    private void triggerEmberflameHit(Player attacker, LivingEntity victim) {
        String key = "emberflame_hit";
        if (onCooldown(attacker, key)) {
            notifyCooldown(attacker, "Emberflame Strike", key);
            return;
        }
        setCooldown(attacker, key, 5);
        victim.setFireTicks(60);
        victim.setVelocity(victim.getVelocity().add(attacker.getLocation().getDirection().multiply(1.5)).setY(0.3));
        LegendaryUtils.spawnParticle(victim.getLocation(), Particle.FLAME, 20, 0.4, 0.4, 0.4);
        victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_BLAZE_HURT, 0.9f, 0.6f);
        attacker.sendActionBar(Component.text("Emberflame Strike!").color(NamedTextColor.GOLD));
    }

    private void triggerEmberRing(Player player) {
        if (onCooldown(player, "ember_ring")) {
            notifyCooldown(player, "Ember Ring", "ember_ring");
            return;
        }
        setCooldown(player, "ember_ring", 15);
        for (double r = 2; r <= 6; r += 1.0) {
            LegendaryUtils.ringParticle(player.getLocation(), Particle.FLAME, (int) (r * 8), r);
        }
        for (Entity e : player.getWorld().getNearbyEntities(player.getLocation(), 6, 3, 6)) {
            if (e instanceof LivingEntity le && !e.equals(player)) {
                le.setFireTicks(100);
            }
        }
        LegendaryUtils.playSound(player, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.5f);
        player.sendActionBar(Component.text("Ember Ring!").color(NamedTextColor.GOLD));
    }

    private void triggerLightningStrike(Player player) {
        if (onCooldown(player, "lightning_strike")) {
            notifyCooldown(player, "Lightning Strike", "lightning_strike");
            return;
        }
        setCooldown(player, "lightning_strike", 12);
        Location loc;
        Entity target = player.getTargetEntity(30);
        if (target != null) {
            loc = target.getLocation();
        } else {
            loc = player.getTargetBlock(null, 30).getLocation().add(0, 1, 0);
        }
        player.getWorld().strikeLightning(loc);
        for (Entity e : player.getWorld().getNearbyEntities(loc, 4, 3, 4)) {
            if (e instanceof LivingEntity le && !e.equals(player)) {
                le.damage(6.0, player);
            }
        }
        LegendaryUtils.playSound(player, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
        player.sendActionBar(Component.text("Lightning Strike!").color(NamedTextColor.GOLD));
    }

    private void triggerChainLightning(Player attacker, LivingEntity victim) {
        String key = "chain_lightning";
        if (onCooldown(attacker, key)) {
            notifyCooldown(attacker, "Chain Lightning", key);
            return;
        }
        setCooldown(attacker, key, 5);
        victim.damage(4.0, attacker);
        int chained = 0;
        for (Entity e : victim.getWorld().getNearbyEntities(victim.getLocation(), 6, 4, 6)) {
            if (chained >= 3) break;
            if (e instanceof LivingEntity le && !le.equals(victim) && !le.equals(attacker)) {
                le.damage(3.0, attacker);
                le.getWorld().strikeLightningEffect(le.getLocation());
                chained++;
            }
        }
        LegendaryUtils.spawnParticle(victim.getLocation(), Particle.END_ROD, 30, 0.4, 0.4, 0.4);
        attacker.sendActionBar(Component.text("Chain Lightning!").color(NamedTextColor.GOLD));
    }

    private void triggerFrostChillHit(Player attacker, LivingEntity victim) {
        String key = "frost_chill_hit";
        if (onCooldown(attacker, key)) {
            notifyCooldown(attacker, "Frost Chill", key);
            return;
        }
        setCooldown(attacker, key, 5);
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, false, true));
        LegendaryUtils.spawnParticle(victim.getLocation(), Particle.SNOWFLAKE, 24, 0.3, 0.5, 0.3);
        LegendaryUtils.playSound(attacker, Sound.BLOCK_GLASS_BREAK, 0.8f, 1.6f);
        attacker.sendActionBar(Component.text("Frost Chill!").color(NamedTextColor.AQUA));
    }

    private void triggerFrostNova(Player player) {
        if (onCooldown(player, "frost_nova")) {
            notifyCooldown(player, "Frost Nova", "frost_nova");
            return;
        }
        setCooldown(player, "frost_nova", 14);
        for (double r = 2; r <= 7; r += 1.5) {
            LegendaryUtils.ringParticle(player.getLocation(), Particle.SNOWFLAKE, (int) (r * 6), r);
        }
        for (Entity e : player.getWorld().getNearbyEntities(player.getLocation(), 7, 4, 7)) {
            if (e instanceof LivingEntity le && !e.equals(player)) {
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 120, 2, false, true));
                le.damage(2.0, player);
            }
        }
        LegendaryUtils.playSound(player, Sound.BLOCK_GLASS_BREAK, 1.0f, 1.2f);
        player.sendActionBar(Component.text("Frost Nova!").color(NamedTextColor.AQUA));
    }

    private void triggerWindBurst(Player player) {
        if (onCooldown(player, "wind_burst")) {
            notifyCooldown(player, "Wind Burst", "wind_burst");
            return;
        }
        setCooldown(player, "wind_burst", 12);
        for (double r = 2; r <= 7; r += 1.5) {
            LegendaryUtils.ringParticle(player.getLocation(), Particle.CLOUD, (int) (r * 8), r);
        }
        for (Entity e : player.getWorld().getNearbyEntities(player.getLocation(), 7, 4, 7)) {
            if (e instanceof LivingEntity le && !e.equals(player)) {
                Vector away = le.getLocation().toVector().subtract(player.getLocation().toVector());
                if (away.length() > 0) {
                    away = away.normalize().setY(1.3);
                } else {
                    away = new Vector(0, 1.3, 0);
                }
                le.setVelocity(away);
            }
        }
        LegendaryUtils.playSound(player, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.3f);
        player.sendActionBar(Component.text("Wind Burst!").color(NamedTextColor.GREEN));
    }

    private void triggerVoidBlink(Player player) {
        if (onCooldown(player, "void_blink")) {
            notifyCooldown(player, "Void Blink", "void_blink");
            return;
        }
        setCooldown(player, "void_blink", 10);
        Location from = player.getLocation();
        Location to = player.getTargetBlock(null, 12).getLocation().add(0.5, 0, 0.5);
        LegendaryUtils.spawnParticle(from, Particle.PORTAL, 40, 0.3, 0.3, 0.3);
        player.teleport(to);
        LegendaryUtils.spawnParticle(player.getLocation(), Particle.PORTAL, 40, 0.3, 0.3, 0.3);
        for (Entity e : player.getWorld().getNearbyEntities(player.getLocation(), 4, 3, 4)) {
            if (e instanceof LivingEntity le && !e.equals(player)) {
                le.damage(8.0, player);
            }
        }
        LegendaryUtils.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.6f);
        player.sendActionBar(Component.text("Void Blink!").color(NamedTextColor.LIGHT_PURPLE));
    }

    private void triggerLifeDrainHit(Player attacker, LivingEntity victim) {
        String key = "life_drain_hit";
        if (onCooldown(attacker, key)) {
            notifyCooldown(attacker, "Life Drain", key);
            return;
        }
        setCooldown(attacker, key, 6);
        double drain = Math.min(4.0, victim.getHealth());
        victim.setHealth(Math.max(0, victim.getHealth() - drain));
        attacker.setHealth(Math.min(attacker.getMaxHealth(), attacker.getHealth() + drain * 0.5));
        LegendaryUtils.spawnParticle(victim.getLocation(), Particle.HEART, 10, 0.3, 0.3, 0.3);
        LegendaryUtils.spawnParticle(victim.getLocation(), Particle.DAMAGE_INDICATOR, 10, 0.3, 0.3, 0.3);
        attacker.sendActionBar(Component.text("Life Drain!").color(NamedTextColor.DARK_RED));
    }
    // ===== SPEAR POWERS =====

    private void triggerSolarBurnHit(Player attacker, LivingEntity victim) {
        String key = "solar_burn_hit";
        if (onCooldown(attacker, key)) {
            notifyCooldown(attacker, "Solar Burn", key);
            return;
        }
        setCooldown(attacker, key, 6);
        victim.setFireTicks(80);
        LegendaryUtils.spawnParticle(victim.getLocation(), Particle.FLAME, 25, 0.4, 0.4, 0.4);
        LegendaryUtils.playSound(attacker, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.8f);
        attacker.sendActionBar(Component.text("Solar Burn!").color(NamedTextColor.GOLD));
    }

    private void triggerSolarFlare(Player player) {
        if (onCooldown(player, "solar_flare")) {
            notifyCooldown(player, "Solar Flare", "solar_flare");
            return;
        }
        setCooldown(player, "solar_flare", 14);
        for (double r = 2; r <= 8; r += 1.5) {
            LegendaryUtils.ringParticle(player.getLocation(), Particle.FLAME, (int) (r * 8), r);
        }
        for (Entity e : player.getWorld().getNearbyEntities(player.getLocation(), 8, 4, 8)) {
            if (e instanceof LivingEntity le && !e.equals(player)) {
                le.setFireTicks(120);
                le.damage(5.0, player);
            }
        }
        LegendaryUtils.playSound(player, Sound.ENTITY_BLAZE_SHOOT, 1.0f, 0.5f);
        player.sendActionBar(Component.text("Solar Flare!").color(NamedTextColor.GOLD));
    }

    private void triggerHarpoonStorm(Player player) {
        if (onCooldown(player, "harpoon_storm")) {
            notifyCooldown(player, "Storm Harpoon", "harpoon_storm");
            return;
        }
        setCooldown(player, "harpoon_storm", 11);
        Location loc;
        Entity target = player.getTargetEntity(40);
        if (target != null) {
            loc = target.getLocation();
        } else {
            loc = player.getTargetBlock(null, 40).getLocation().add(0, 1, 0);
        }
        player.getWorld().strikeLightning(loc);
        for (Entity e : player.getWorld().getNearbyEntities(loc, 5, 4, 5)) {
            if (e instanceof LivingEntity le && !e.equals(player)) {
                le.damage(7.0, player);
            }
        }
        LegendaryUtils.playSound(player, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.2f);
        player.sendActionBar(Component.text("Storm Harpoon!").color(NamedTextColor.DARK_AQUA));
    }

    private void triggerSpearStormchain(Player attacker, LivingEntity victim) {
        String key = "spear_stormchain";
        if (onCooldown(attacker, key)) {
            notifyCooldown(attacker, "Storm Chain", key);
            return;
        }
        setCooldown(attacker, key, 5);
        victim.damage(3.0, attacker);
        LegendaryUtils.spawnParticle(victim.getLocation(), Particle.END_ROD, 25, 0.4, 0.4, 0.4);
        for (Entity e : victim.getWorld().getNearbyEntities(victim.getLocation(), 5, 4, 5)) {
            if (e instanceof LivingEntity le && !le.equals(victim) && !le.equals(attacker)) {
                le.damage(3.0, attacker);
                break;
            }
        }
        attacker.sendActionBar(Component.text("Storm Chain!").color(NamedTextColor.DARK_AQUA));
    }

    private void triggerBloodBleedHit(Player attacker, LivingEntity victim) {
        String key = "blood_bleed_hit";
        if (onCooldown(attacker, key)) {
            notifyCooldown(attacker, "Blood Bleed", key);
            return;
        }
        setCooldown(attacker, key, 7);
        double drain = Math.min(5.0, victim.getHealth());
        victim.setHealth(Math.max(0, victim.getHealth() - drain));
        attacker.setHealth(Math.min(attacker.getMaxHealth(), attacker.getHealth() + drain * 0.6));
        LegendaryUtils.spawnParticle(victim.getLocation(), Particle.DUST, 20, 0.3, 0.3, 0.3);
        LegendaryUtils.playSound(attacker, Sound.ENTITY_WITCH_HURT, 1.0f, 0.5f);
        attacker.sendActionBar(Component.text("Blood Bleed!").color(NamedTextColor.DARK_RED));
    }

    private void triggerBloodPact(Player player) {
        if (onCooldown(player, "blood_pact")) {
            notifyCooldown(player, "Blood Pact", "blood_pact");
            return;
        }
        setCooldown(player, "blood_pact", 20);
        if (player.getHealth() <= 4.0) {
            player.sendActionBar(Component.text("Too weak to strike a Blood Pact.").color(NamedTextColor.RED));
            return;
        }
        player.setHealth(player.getHealth() - 4.0);
        player.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 120, 1, false, true));
        bloodPactTicks.put(player.getUniqueId(), 120);
        LegendaryUtils.spawnParticle(player.getLocation(), Particle.DUST, 40, 0.4, 0.4, 0.4);
        LegendaryUtils.playSound(player, Sound.ENTITY_WITCH_DRINK, 1.0f, 0.6f);
        player.sendActionBar(Component.text("Blood Pact struck! Attack power surges.").color(NamedTextColor.DARK_RED));
    }

    private void triggerPetrifyHit(Player attacker, LivingEntity victim) {
        String key = "petrify_hit";
        if (onCooldown(attacker, key)) {
            notifyCooldown(attacker, "Petrify", key);
            return;
        }
        setCooldown(attacker, key, 8);
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 7, false, true));
        victim.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 80, 200, false, true));
        LegendaryUtils.spawnParticle(victim.getLocation(), Particle.ENCHANTED_HIT, 25, 0.3, 0.3, 0.3);
        LegendaryUtils.playSound(attacker, Sound.BLOCK_STONE_BREAK, 1.0f, 0.7f);
        attacker.sendActionBar(Component.text("Petrify!").color(NamedTextColor.GRAY));
    }

    private void triggerPillarStone(Player player) {
        if (onCooldown(player, "pillar_stone")) {
            notifyCooldown(player, "Pillar of Stone", "pillar_stone");
            return;
        }
        setCooldown(player, "pillar_stone", 13);
        Location loc;
        Entity target = player.getTargetEntity(30);
        if (target != null) {
            loc = target.getLocation();
        } else {
            loc = player.getTargetBlock(null, 30).getLocation().add(0, 1, 0);
        }
        World world = player.getWorld();
        for (int i = 0; i < 4; i++) {
            world.spawnParticle(Particle.BLOCK, loc.clone().add(0, i, 0), 10, 0.3, 0.3, 0.3, Material.STONE.createBlockData());
        }
        for (Entity e : world.getNearbyEntities(loc, 3, 3, 3)) {
            if (e instanceof LivingEntity le && !e.equals(player)) {
                le.damage(6.0, player);
                le.setVelocity(new Vector(0, 1.2, 0));
            }
        }
        LegendaryUtils.playSound(player, Sound.BLOCK_STONE_BREAK, 1.0f, 0.6f);
        player.sendActionBar(Component.text("Pillar of Stone!").color(NamedTextColor.GRAY));
    }

    private void triggerWhirlwindDash(Player player) {
        if (onCooldown(player, "whirlwind_dash")) {
            notifyCooldown(player, "Whirlwind Dash", "whirlwind_dash");
            return;
        }
        setCooldown(player, "whirlwind_dash", 9);
        Vector dir = player.getLocation().getDirection();
        player.setVelocity(dir.multiply(1.8).setY(0.1));
        LegendaryUtils.spawnParticle(player.getLocation(), Particle.CLOUD, 40, 0.4, 0.4, 0.4);
        for (Entity e : player.getWorld().getNearbyEntities(player.getLocation(), 4, 3, 4)) {
            if (e instanceof LivingEntity le && !e.equals(player)) {
                le.damage(8.0, player);
                le.setVelocity(dir.multiply(0.8).setY(0.5));
            }
        }
        LegendaryUtils.playSound(player, Sound.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 0.9f);
        player.sendActionBar(Component.text("Whirlwind Dash!").color(NamedTextColor.GREEN));
    }

    private void triggerWindpushHit(Player attacker, LivingEntity victim) {
        String key = "windpush_hit";
        if (onCooldown(attacker, key)) {
            notifyCooldown(attacker, "Wind Push", key);
            return;
        }
        setCooldown(attacker, key, 4);
        victim.setVelocity(attacker.getLocation().getDirection().multiply(1.8).setY(0.4));
        LegendaryUtils.spawnParticle(victim.getLocation(), Particle.CLOUD, 20, 0.3, 0.3, 0.3);
        LegendaryUtils.playSound(attacker, Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 1.5f);
        attacker.sendActionBar(Component.text("Wind Push!").color(NamedTextColor.GREEN));
    }

    // ===== OTHER WEAPON (VOIDBLADE) POWERS =====

    private void triggerVoidSlash(Player player) {
        if (onCooldown(player, "void_slash")) {
            notifyCooldown(player, "Void Slash", "void_slash");
            return;
        }
        setCooldown(player, "void_slash", 10);
        Vector dir = player.getLocation().getDirection();
        Location base = player.getEyeLocation();
        for (double d = 0; d <= 10; d += 0.8) {
            Location p = base.clone().add(dir.clone().multiply(d));
            LegendaryUtils.spawnParticle(p, Particle.PORTAL, 6, 0.2, 0.2, 0.2);
            for (Entity e : player.getWorld().getNearbyEntities(p, 1.5, 1.5, 1.5)) {
                if (e instanceof LivingEntity le && !e.equals(player)) {
                    le.damage(8.0, player);
                }
            }
        }
        LegendaryUtils.playSound(player, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 0.5f);
        player.sendActionBar(Component.text("Void Slash!").color(NamedTextColor.LIGHT_PURPLE));
    }

    private void triggerShadowTeleportHit(Player attacker, LivingEntity victim) {
        String key = "shadow_teleport_hit";
        if (onCooldown(attacker, key)) {
            notifyCooldown(attacker, "Shadow Teleport", key);
            return;
        }
        setCooldown(attacker, key, 8);
        LegendaryUtils.spawnParticle(attacker.getLocation(), Particle.PORTAL, 30, 0.3, 0.3, 0.3);
        Location behind = victim.getLocation().clone().add(victim.getLocation().getDirection().normalize().multiply(-1.5));
        attacker.teleport(behind);
        LegendaryUtils.spawnParticle(attacker.getLocation(), Particle.PORTAL, 30, 0.3, 0.3, 0.3);
        victim.damage(5.0, attacker);
        LegendaryUtils.playSound(attacker, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        attacker.sendActionBar(Component.text("Shadow Teleport!").color(NamedTextColor.LIGHT_PURPLE));
    }
    //END_FINAL
}

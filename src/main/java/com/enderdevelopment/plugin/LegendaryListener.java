package com.enderdevelopment.plugin;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.block.Action;

/**
 * Unified event listener that routes events to the AbilityLogic engine.
 */
public class LegendaryListener implements Listener {

    private final Main plugin;
    private final LegendaryItems registry;
    private final AbilityLogic abilityLogic;

    public LegendaryListener(Main plugin, LegendaryItems registry, AbilityLogic abilityLogic) {
        this.plugin = plugin;
        this.registry = registry;
        this.abilityLogic = abilityLogic;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        String id = LegendaryUtils.getLegendaryId(item, plugin);
        boolean isWeapon = isWeaponLike(id);

        // Weapons and the voidblade: right-click triggers ability.
        if (isWeapon && id != null) {
            event.setCancelled(true);
            abilityLogic.onRightClick(player);
            return;
        }

        // Armor active powers trigger on sneak + right-click.
        if (isArmorActive(id) && player.isSneaking()) {
            event.setCancelled(true);
            abilityLogic.onRightClick(player);
        }
    }

    private boolean isWeaponLike(String id) {
        if (id == null) {
            return false;
        }
        switch (id) {
            case "mace_crimson":
            case "mace_storm":
            case "mace_frost":
            case "mace_wind":
            case "mace_void":
            case "spear_sun":
            case "spear_storm":
            case "spear_blood":
            case "spear_ancient":
            case "spear_wind":
            case "voidblade":
                return true;
            default:
                return false;
        }
    }

    private boolean isArmorActive(String id) {
        return "shadow_chestplate".equals(id)
                || "shadow_leggings".equals(id)
                || "shadow_boots".equals(id);
    }

    @EventHandler
    public void onPlayerHitEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player attacker && event.getEntity() instanceof LivingEntity victim) {
            // Only physical melee hits (not magic/projectile) trigger weapon on-hit powers.
            if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                    || event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
                abilityLogic.onAttack(attacker, victim);
            }
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        // Crown of the Void: retaliate with a blinding smoke screen when hurt.
        if (hasEquipped(player, EquipmentSlot.HEAD, "shadow_helmet")) {
            abilityLogic.onPlayerHurt(player);
        }
        // Fall damage immunity from shadow boots
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL
                && hasEquipped(player, EquipmentSlot.FEET, "shadow_boots")) {
            player.getWorld().spawnParticle(org.bukkit.Particle.ENCHANT, player.getLocation(), 12, 0.2, 0.2, 0.2);
            event.setCancelled(true);
            return;
        }
        // Wither immunity from full set
        if (event.getCause() == EntityDamageEvent.DamageCause.WITHER && hasFullSet(player)) {
            event.setCancelled(true);
        }
    }

    private boolean hasEquipped(Player player, EquipmentSlot slot, String itemId) {
        ItemStack item = player.getInventory().getItem(slot);
        return itemId.equals(LegendaryUtils.getLegendaryId(item, plugin));
    }

    private boolean hasFullSet(Player player) {
        return hasEquipped(player, EquipmentSlot.HEAD, "shadow_helmet")
                && hasEquipped(player, EquipmentSlot.CHEST, "shadow_chestplate")
                && hasEquipped(player, EquipmentSlot.LEGS, "shadow_leggings")
                && hasEquipped(player, EquipmentSlot.FEET, "shadow_boots");
    }
}

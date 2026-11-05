package com.enderdevelopment.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of all fifteen legendary items.
 * Each item carries a unique id, a display name, a lore backstory and an Ability Lore
 * listing its two powers, their triggers and cooldowns.
 */
public class LegendaryItems {

    private final Main plugin;
    private final Map<String, LegendaryItem> items = new LinkedHashMap<>();

    public LegendaryItems(Main plugin) {
        this.plugin = plugin;
        registerAll();
    }

    private void registerAll() {
        registerShadowArmor();
        registerMaces();
        registerSpears();
        registerOther();
    }

    /**
     * Builds the standard Ability Lore list for an item.
     */
    private List<Component> abilityLore(String abilityOne, String abilityTwo) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());
        lines.add(Component.text("§8§m                                    §r"));
        lines.add(Component.text("§6§l✦ §e§lAbility Lore").decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        lines.add(Component.text("§8» §6Power I: §7" + abilityOne));
        lines.add(Component.text("§8» §6Power II: §7" + abilityTwo));
        lines.add(Component.text("§8§m                                    §r"));
        return lines;
    }

    private List<Component> baseLore(String backstory) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.empty());
        lines.add(Component.text("§7§o\"" + backstory + "\""));
        lines.add(Component.empty());
        lines.add(Component.text("§d§l✧ §5§lLEGENDARY ITEM").decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        return lines;
    }

    private void add(String id, LegendaryItem item) {
        items.put(id, item);
    }

    // ------------------------------------------------------------------
    // 1. The Shadow Imperium armor set (4 pieces + full-set bonus)
    // ------------------------------------------------------------------
    private void registerShadowArmor() {
        TextColor c1 = TextColor.color(0x7b, 0x2f, 0xff);
        TextColor c2 = TextColor.color(0x1d, 0x0a, 0x4a);

        // Helmet - Crown of the Void
        add("shadow_helmet", new LegendaryItem("shadow_helmet", Material.NETHERITE_HELMET,
                LegendaryUtils.gradient("Crown of the Void", c1, c2),
                combine(baseLore("Forged in the silent dark, this crown lets its bearer see what the living cannot."),
                        abilityLore("Passive: grants night vision while worn.", "On hurt: retaliates with a blinding smoke screen (6s cooldown).")),
                "passive_night_vision", "retaliate_smoke",
                ""));

        // Chestplate - Breastplate of the Hollow King
        add("shadow_chestplate", new LegendaryItem("shadow_chestplate", Material.NETHERITE_CHESTPLATE,
                LegendaryUtils.gradient("Breastplate of the Hollow King", c1, c2),
                combine(baseLore("A chestplate that once guarded a throne of whispers; it still answers to its king."),
                        abilityLore("Sneak+Right-click: Phase Shift - become briefly invulnerable and displace away from danger (18s cooldown).", "Passive: grants Resistance while below 40% health.")),
                "phase_shift", "passive_lowhp_resist",
                ""));

        // Leggings - Greaves of the Phantom
        add("shadow_leggings", new LegendaryItem("shadow_leggings", Material.NETHERITE_LEGGINGS,
                LegendaryUtils.gradient("Greaves of the Phantom", c1, c2),
                combine(baseLore("Leggings stitched from the mist of the Grey Expanse; the wearer walks twice as fast."),
                        abilityLore("Passive: increases movement speed.", "Sneak+Right-click: Shadow Step - dash several blocks forward (8s cooldown).")),
                "passive_speed", "shadow_step",
                ""));

        // Boots - Treads of the Abyss
        add("shadow_boots", new LegendaryItem("shadow_boots", Material.NETHERITE_BOOTS,
                LegendaryUtils.gradient("Treads of the Abyss", c1, c2),
                combine(baseLore("Boots bound with chains of twilight; no fall may harm the one who wears them."),
                        abilityLore("Passive: fall damage immunity and no mid-air slowness.", "Sneak+Right-click in air: Abyssal Leap - propel upward (10s cooldown).")),
                "passive_fall_immune", "abyssal_leap",
                ""));

        // Full set bonus is handled in AbilityLogic when all 4 pieces are equipped.
    }

    // ------------------------------------------------------------------
    // 2. Legendary Maces (Buzdygany) - 5 items
    // ------------------------------------------------------------------
    private void registerMaces() {
        // Crimson Gavel of Emberfall
        add("mace_crimson", new LegendaryItem("mace_crimson", Material.MACE,
                LegendaryUtils.gradient("Crimson Gavel of Emberfall", TextColor.color(0xff, 0x3b, 0x3b), TextColor.color(0x7a, 0x00, 0x00)),
                combine(baseLore("A war-hammer carved from the heart of a dying volcano, still warm to the touch."),
                        abilityLore("On-hit: ignites the victim and knocks them back with a fiery blast (5s cooldown).", "Right-click: Ember Ring - surrounding foes burn in a ring of fire (15s cooldown).")),
                "emberflame_hit", "ember_ring",
                ""));

        // Tempest Maul
        add("mace_storm", new LegendaryItem("mace_storm", Material.MACE,
                LegendaryUtils.gradient("Tempest Maul", TextColor.color(0x3b, 0x8f, 0xff), TextColor.color(0x0a, 0x2a, 0x7a)),
                combine(baseLore("A hammer that carries the fury of a captive storm; each swing crackles with static."),
                        abilityLore("Right-click: call a lightning strike on the targeted enemy (12s cooldown).", "On-hit: chain lightning arcs to nearby foes (5s cooldown).")),
                "lightning_strike", "chain_lightning",
                ""));

        // Glacial Shardbreaker
        add("mace_frost", new LegendaryItem("mace_frost", Material.MACE,
                LegendaryUtils.gradient("Glacial Shardbreaker", TextColor.color(0x9b, 0xe8, 0xff), TextColor.color(0x1d, 0x4a, 0x7a)),
                combine(baseLore("Forged in the eternal ice of the northern wastes; it shatters both stone and will."),
                        abilityLore("On-hit: chills the victim, slowing them (5s cooldown).", "Right-click: Frost Nova - a freezing wave slows all nearby enemies (14s cooldown).")),
                "frost_chill_hit", "frost_nova",
                ""));

        // Zephyr Windbreaker
        add("mace_wind", new LegendaryItem("mace_wind", Material.MACE,
                LegendaryUtils.gradient("Zephyr Windbreaker", TextColor.color(0xb8, 0xff, 0x9b), TextColor.color(0x2a, 0x7a, 0x3b)),
                combine(baseLore("A hammer woven from the breath of the eastern gales; it makes the bearer swift as air."),
                        abilityLore("Passive: increases movement speed and jump height.", "Right-click: Wind Burst - launches nearby enemies into the air (12s cooldown).")),
                "passive_wind_speed", "wind_burst",
                ""));

        // Umbral Enderclap
        add("mace_void", new LegendaryItem("mace_void", Material.MACE,
                LegendaryUtils.gradient("Umbral Enderclap", TextColor.color(0x8a, 0x5f, 0xff), TextColor.color(0x1a, 0x0a, 0x3a)),
                combine(baseLore("A weapon stolen from the realm between realms; it blinks through space with the bearer."),
                        abilityLore("Right-click: Void Blink - teleport towards your aim and slam the area (10s cooldown).", "On-hit: drains a portion of the victim's health to heal you (6s cooldown).")),
                "void_blink", "life_drain_hit",
                ""));
    }

    // ------------------------------------------------------------------
    // 3. Legendary Spears (Włócznie) - 5 items
    // ------------------------------------------------------------------
    private void registerSpears() {
        // Solar Lance of Dawn
        add("spear_sun", new LegendaryItem("spear_sun", Material.NETHERITE_SWORD,
                LegendaryUtils.gradient("Solar Lance of Dawn", TextColor.color(0xff, 0xd7, 0x3b), TextColor.color(0xff, 0x8c, 0x1a)),
                combine(baseLore("The first ray of a new sun, given form and a sharp point."),
                        abilityLore("On-hit: sears the victim with solar fire, dealing burn damage over time (6s cooldown).", "Right-click: Solar Flare - a burst of burning light damages everything ahead (14s cooldown).")),
                "solar_burn_hit", "solar_flare",
                ""));

        // Stormcaller Harpoon
        add("spear_storm", new LegendaryItem("spear_storm", Material.NETHERITE_SWORD,
                LegendaryUtils.gradient("Stormcaller Harpoon", TextColor.color(0x7a, 0xd4, 0xff), TextColor.color(0x2a, 0x4a, 0x9b)),
                combine(baseLore("A harpoon used by sky-sailors to hunt storms; it prefers to catch lightning."),
                        abilityLore("Right-click: hurl a lightning harpoon at your target (11s cooldown).", "On-hit: arcs storm energy to nearby enemies (5s cooldown).")),
                "harpoon_storm", "spear_stormchain",
                ""));

        // Sanguine Needle
        add("spear_blood", new LegendaryItem("spear_blood", Material.NETHERITE_SWORD,
                LegendaryUtils.gradient("Sanguine Needle", TextColor.color(0xff, 0x4b, 0x6b), TextColor.color(0x7a, 0x0a, 0x1a)),
                combine(baseLore("A needle fed on the blood of a thousand kings; it hungers still."),
                        abilityLore("On-hit: inflicts a bleeding wound that siphons life to you (7s cooldown).", "Right-click: Blood Pact - sacrifice health to gain a surge of attack power (20s cooldown).")),
                "blood_bleed_hit", "blood_pact",
                ""));

        // Spear of the Elders
        add("spear_ancient", new LegendaryItem("spear_ancient", Material.NETHERITE_SWORD,
                LegendaryUtils.gradient("Spear of the Elders", TextColor.color(0xb0, 0xc4, 0xde), TextColor.color(0x4a, 0x5a, 0x7a)),
                combine(baseLore("A spear passed down from the first age; the stone remembers what it pierces."),
                        abilityLore("On-hit: petrifies the victim, rooting them in stone for a moment (8s cooldown).", "Right-click: Pillar of Stone - a stone pillar erupts beneath your target (13s cooldown).")),
                "petrify_hit", "pillar_stone",
                ""));

        // Whirlwind Trident
        add("spear_wind", new LegendaryItem("spear_wind", Material.NETHERITE_SWORD,
                LegendaryUtils.gradient("Whirlwind Trident", TextColor.color(0x9b, 0xff, 0xd4), TextColor.color(0x1a, 0x7a, 0x5a)),
                combine(baseLore("A spear that spins like a cyclone; those struck by it are scattered to the winds."),
                        abilityLore("Right-click: Whirlwind Dash - dash forward in a spinning strike (9s cooldown).", "On-hit: knocks the victim back with a burst of wind (4s cooldown).")),
                "whirlwind_dash", "windpush_hit",
                ""));
    }

    // ------------------------------------------------------------------
    // 4. Other unique legendary weapon
    // ------------------------------------------------------------------
    private void registerOther() {
        // Eclipse Blade - the Void Reaver
        add("voidblade", new LegendaryItem("voidblade", Material.NETHERITE_SWORD,
                LegendaryUtils.gradient("Eclipse Blade, the Void Reaver", TextColor.color(0x7a, 0x5f, 0xff), TextColor.color(0x0a, 0x0a, 0x2a)),
                combine(baseLore("A blade that swallowed an eclipse; it leaves a trail of night behind every cut."),
                        abilityLore("Right-click: Void Slash - unleash a wave of shadow that damages all in a line (10s cooldown).", "On-hit: shadow teleport behind the victim and strike again with bonus damage (8s cooldown).")),
                "void_slash", "shadow_teleport_hit",
                ""));
    }

    private List<Component> combine(List<Component> a, List<Component> b) {
        List<Component> result = new ArrayList<>(a);
        result.addAll(b);
        if (result.isEmpty()) {
            result.add(Component.empty());
        }
        return result;
    }

    public LegendaryItem get(String id) {
        return items.get(id);
    }

    public Map<String, LegendaryItem> getAll() {
        return items;
    }

    public boolean exists(String id) {
        return items.containsKey(id);
    }

    public List<String> allIds() {
        return new ArrayList<>(items.keySet());
    }
}

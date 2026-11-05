package com.enderdevelopment.plugin;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for the Legendaries plugin.
 * Registers the event listener and gives command on enable.
 */
public final class Main extends JavaPlugin {

    private static Main instance;
    private LegendaryItems registry;
    private AbilityLogic abilityLogic;
    private LegendaryListener listener;

    @Override
    public void onEnable() {
        instance = this;
        registry = new LegendaryItems(this);
        abilityLogic = new AbilityLogic(this);
        listener = new LegendaryListener(this, registry, abilityLogic);

        Bukkit.getPluginManager().registerEvents(listener, this);

        LegendaryCommand command = new LegendaryCommand(this, registry);
        if (getCommand("legendaries") != null) {
            getCommand("legendaries").setExecutor(command);
            getCommand("legendaries").setTabCompleter(command);
        }

        getLogger().info("Legendaries enabled - 15 legendary items registered.");
    }

    @Override
    public void onDisable() {
        if (abilityLogic != null) {
            abilityLogic.shutdown();
        }
        instance = null;
        getLogger().info("Legendaries disabled.");
    }

    public static Main getInstance() {
        return instance;
    }

    public LegendaryItems getRegistry() {
        return registry;
    }

    public AbilityLogic getAbilityLogic() {
        return abilityLogic;
    }
}

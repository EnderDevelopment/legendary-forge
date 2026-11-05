package com.enderdevelopment.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Commands: /legendaries give <player> <item_id>, with full tab completion.
 */
public class LegendaryCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final LegendaryItems registry;

    public LegendaryCommand(Main plugin, LegendaryItems registry) {
        this.plugin = plugin;
        this.registry = registry;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("legendaries.admin")) {
            sender.sendMessage(Component.text("You do not have permission to use this command.").color(NamedTextColor.RED));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(Component.text("Usage: /legendaries give <player> <item_id>").color(NamedTextColor.YELLOW));
            sender.sendMessage(Component.text("Or: /legendaries list").color(NamedTextColor.YELLOW));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give" -> {
                if (args.length < 3) {
                    sender.sendMessage(Component.text("Usage: /legendaries give <player> <item_id>").color(NamedTextColor.YELLOW));
                    return true;
                }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) {
                    sender.sendMessage(Component.text("Player not found: " + args[1]).color(NamedTextColor.RED));
                    return true;
                }
                String itemId = args[2].toLowerCase();
                LegendaryItem def = registry.get(itemId);
                if (def == null) {
                    sender.sendMessage(Component.text("Unknown item id: " + args[2]).color(NamedTextColor.RED));
                    sender.sendMessage(Component.text("Use /legendaries list to see all ids.").color(NamedTextColor.YELLOW));
                    return true;
                }
                ItemStack item = def.buildItem(plugin);
                int slot = target.getInventory().firstEmpty();
                if (slot == -1) {
                    target.getWorld().dropItem(target.getLocation(), item);
                    sender.sendMessage(Component.text("Given " + itemId + " to " + target.getName() + " (inventory full, dropped at their feet).").color(NamedTextColor.GREEN));
                } else {
                    target.getInventory().setItem(slot, item);
                    sender.sendMessage(Component.text("Given " + itemId + " to " + target.getName() + ".").color(NamedTextColor.GREEN));
                }
                target.sendMessage(Component.text("You received a legendary item!").color(NamedTextColor.GOLD));
            }
            case "list" -> {
                sender.sendMessage(Component.text("Legendary item ids:").color(NamedTextColor.GOLD));
                for (LegendaryItem def : registry.getAll().values()) {
                    sender.sendMessage(Component.text("  - " + def.getId()).color(NamedTextColor.LIGHT_PURPLE)
                            .append(Component.text("  ").color(NamedTextColor.DARK_GRAY))
                            .append(def.getDisplayName()));
                }
            }
            default -> {
                sender.sendMessage(Component.text("Unknown subcommand: " + args[0]).color(NamedTextColor.RED));
                sender.sendMessage(Component.text("Usage: /legendaries give <player> <item_id> | /legendaries list").color(NamedTextColor.YELLOW));
            }
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!sender.hasPermission("legendaries.admin")) {
            return completions;
        }
        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            addIfStartsWith(completions, "give", prefix);
            addIfStartsWith(completions, "list", prefix);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            String prefix = args[1].toLowerCase();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(prefix)) {
                    completions.add(player.getName());
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("give")) {
            String prefix = args[2].toLowerCase();
            for (String id : registry.allIds()) {
                if (id.startsWith(prefix)) {
                    completions.add(id);
                }
            }
        }
        return completions;
    }

    private void addIfStartsWith(List<String> list, String value, String prefix) {
        if (value.startsWith(prefix)) {
            list.add(value);
        }
    }
}

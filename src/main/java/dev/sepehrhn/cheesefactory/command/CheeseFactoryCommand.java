package dev.sepehrhn.cheesefactory.command;

import dev.sepehrhn.cheesefactory.CheeseFactoryPlugin;
import dev.sepehrhn.cheesefactory.locale.LocaleManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CheeseFactoryCommand implements CommandExecutor, TabCompleter {

    private final CheeseFactoryPlugin plugin;
    private final LocaleManager locale;

    public CheeseFactoryCommand(CheeseFactoryPlugin plugin) {
        this.plugin = plugin;
        this.locale = plugin.getLocaleManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 1) {
            if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("cheesefactory.admin")) {
                    sender.sendMessage(locale.component(sender, "command.no-permission"));
                    return true;
                }
                plugin.reloadCheeseConfig();
                sender.sendMessage(locale.component(sender, "command.reload.success"));
                return true;
            }
            if (args[0].equalsIgnoreCase("debugbarrel")) {
                if (!sender.hasPermission("cheesefactory.admin")) {
                    sender.sendMessage(locale.component(sender, "command.no-permission"));
                    return true;
                }
                boolean enabled = plugin.toggleCheeseBarrelDebug();
                String key = enabled ? "debug.cheese_barrel.enabled" : "debug.cheese_barrel.disabled";
                sender.sendMessage(locale.component(sender, key));
                return true;
            }
            if (args[0].equalsIgnoreCase("testbarrel")) {
                if (!sender.hasPermission("cheesefactory.admin")) {
                    sender.sendMessage(locale.component(sender, "command.no-permission"));
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage(locale.component(sender, "debug.cheese_barrel.player_only"));
                    return true;
                }
                var target = player.getTargetBlockExact(5);
                if (target == null) {
                    sender.sendMessage(locale.component(sender, "debug.cheese_barrel.no_target"));
                    return true;
                }
                var loc = target.getLocation();
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("x", String.valueOf(loc.getBlockX()));
                placeholders.put("y", String.valueOf(loc.getBlockY()));
                placeholders.put("z", String.valueOf(loc.getBlockZ()));
                placeholders.put("type", target.getType().name());
                if (target.getType() != org.bukkit.Material.BARREL) {
                    sender.sendMessage(locale.component(sender, "debug.cheese_barrel.not_barrel", placeholders));
                    return true;
                }
                sender.sendMessage(locale.component(sender, "debug.cheese_barrel.test_converting", placeholders));
                boolean success = plugin.getBarrelManager().registerBarrel(target);
                if (plugin.isCheeseBarrelDebugEnabled()) {
                    plugin.getLogger().info("[CheeseDebug] Test command invoked conversion for barrel at ("
                            + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ() + "). Success=" + success);
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("give")) {
                return handleGive(sender, args);
            }
        }

        sender.sendMessage(locale.component(sender, "command.usage", Collections.singletonMap("command", label)));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1 && sender.hasPermission("cheesefactory.admin")) {
            String arg = args[0].toLowerCase(Locale.ROOT);
            return List.of("reload", "debugbarrel", "testbarrel", "give").stream()
                    .filter(opt -> opt.startsWith(arg))
                    .toList();
        }
        if (args.length == 2 && sender.hasPermission("cheesefactory.admin") && args[0].equalsIgnoreCase("give")) {
            return List.of("cheese_barrel", "bacteria", "curd", "inoculated_milk").stream()
                    .filter(opt -> opt.startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .toList();
        }
        return Collections.emptyList();
    }

    private boolean handleGive(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(locale.component(sender, "command.usage", Collections.singletonMap("command", "cheesefactory")));
            return true;
        }
        String target = args[1].toLowerCase(Locale.ROOT);
        switch (target) {
            case "cheese_barrel", "barrel" -> giveBarrel(sender, args);
            case "bacteria" -> giveBacteria(sender, args);
            case "curd" -> giveCurd(sender, args);
            case "inoculated_milk", "inoculatedmilk", "milk" -> giveInoculatedMilk(sender, args);
            default -> sender.sendMessage(locale.component(sender, "command.usage", Collections.singletonMap("command", "cheesefactory")));
        }
        return true;
    }

    private void giveBarrel(CommandSender sender, String[] args) {
        if (!sender.hasPermission("cheesefactory.give.barrel")) {
            sender.sendMessage(locale.component(sender, "command.no-permission"));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(locale.component(sender, "commands.give.barrel.player_only"));
            return;
        }
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(locale.component(sender, "commands.give.barrel.invalid_amount"));
                return;
            }
        }
        amount = Math.max(1, Math.min(64, amount));
        var stack = plugin.getBarrelItemService().createBarrelItem();
        stack.setAmount(amount);
        var leftover = player.getInventory().addItem(stack);
        leftover.values().forEach(it -> player.getWorld().dropItemNaturally(player.getLocation().add(0, 0.5, 0), it));
        sender.sendMessage(locale.component(sender, "commands.give.barrel.success_self", Collections.singletonMap("amount", String.valueOf(amount))));
    }

    private void giveBacteria(CommandSender sender, String[] args) {
        if (!sender.hasPermission("cheesefactory.give.bacteria")) {
            sender.sendMessage(locale.component(sender, "command.no-permission"));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(locale.component(sender, "commands.give.bacteria.player_only"));
            return;
        }
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(locale.component(sender, "commands.give.bacteria.invalid_amount"));
                return;
            }
        }
        amount = Math.max(1, Math.min(64, amount));
        var stack = plugin.getItemManager().createBacteriaItem();
        stack.setAmount(amount);
        var leftover = player.getInventory().addItem(stack);
        leftover.values().forEach(it -> player.getWorld().dropItemNaturally(player.getLocation().add(0, 0.5, 0), it));

        sender.sendMessage(locale.component(sender, "commands.give.bacteria.success_self", Collections.singletonMap("amount", String.valueOf(amount))));
    }

    private void giveCurd(CommandSender sender, String[] args) {
        if (!sender.hasPermission("cheesefactory.give.curd")) {
            sender.sendMessage(locale.component(sender, "command.no-permission"));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(locale.component(sender, "commands.give.curd.player_only"));
            return;
        }
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(locale.component(sender, "commands.give.curd.invalid_amount"));
                return;
            }
        }
        amount = Math.max(1, Math.min(64, amount));
        for (int i = 0; i < amount; i++) {
            var stack = plugin.getItemManager().createCurdItem();
            var leftover = player.getInventory().addItem(stack);
            leftover.values().forEach(it -> player.getWorld().dropItemNaturally(player.getLocation().add(0, 0.5, 0), it));
        }

        sender.sendMessage(locale.component(sender, "commands.give.curd.success_self", Collections.singletonMap("amount", String.valueOf(amount))));
    }

    private void giveInoculatedMilk(CommandSender sender, String[] args) {
        if (!sender.hasPermission("cheesefactory.give.inoculated_milk")) {
            sender.sendMessage(locale.component(sender, "command.no-permission"));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(locale.component(sender, "commands.give.inoculated_milk.player_only"));
            return;
        }
        int amount = 1;
        if (args.length >= 3) {
            try {
                amount = Integer.parseInt(args[2]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(locale.component(sender, "commands.give.inoculated_milk.invalid_amount"));
                return;
            }
        }
        amount = Math.max(1, Math.min(64, amount));
        var stack = plugin.getItemManager().createInoculatedMilk();
        stack.setAmount(amount);
        var leftover = player.getInventory().addItem(stack);
        leftover.values().forEach(it -> player.getWorld().dropItemNaturally(player.getLocation().add(0, 0.5, 0), it));

        sender.sendMessage(locale.component(sender, "commands.give.inoculated_milk.success_self", Collections.singletonMap("amount", String.valueOf(amount))));
    }
}

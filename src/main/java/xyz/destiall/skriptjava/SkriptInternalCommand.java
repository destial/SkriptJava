package xyz.destiall.skriptjava;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SkriptInternalCommand implements CommandExecutor {
    private final SkriptManager manager;
    public SkriptInternalCommand(SkriptManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length > 0 ? args[0] : "";
        if (sub.equalsIgnoreCase("reload")) {
            if (sender.hasPermission("skriptjava.reload")) {
                String script = args.length > 1 ? args[1] : "";
                try {
                    if (manager.reloadScript(script)) {
                        sender.sendMessage(ChatColor.GREEN + "Successfully reloaded script " + script);
                    } else {
                        sender.sendMessage(ChatColor.RED + "Unable to reload script " + script + ". Maybe the script is missing?");
                    }
                } catch (ScriptException e) {
                    sender.sendMessage(ChatColor.RED + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
            }
        } else if (sub.equalsIgnoreCase("reloadall")) {
            if (sender.hasPermission("skriptjava.reloadall")) {
                Set<String> names = new HashSet<>(manager.getEngine().getNames());
                for (String n : names) {
                    try {
                        manager.reload();
                        sender.sendMessage(ChatColor.GREEN + "Successfully reloaded script " + n);
                    } catch (Exception e) {
                        e.printStackTrace();
                        sender.sendMessage(ChatColor.RED + "Unable to reload script " + n + ". Maybe the script is missing?");
                    }
                }
                sender.sendMessage(ChatColor.GREEN + "Successfully reloaded all scripts");
            } else {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
            }
        } else if (sub.equalsIgnoreCase("load")) {
            if (sender.hasPermission("skriptjava.load")) {
                String script = args.length > 1 ? args[1] : "";
                try {
                    if (manager.loadScript(script)) {
                        sender.sendMessage(ChatColor.GREEN + "Successfully loaded script " + script);
                    } else {
                        sender.sendMessage(ChatColor.RED + "Unable to load script " + script + ". Maybe the script is missing?");
                    }
                } catch (ScriptException e) {
                    sender.sendMessage(ChatColor.RED + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
            }
        } else if (sub.equalsIgnoreCase("unload")) {
            if (sender.hasPermission("skriptjava.unload")) {
                String script = args.length > 1 ? args[1] : "";
                if (manager.unloadScript(script)) {
                    sender.sendMessage(ChatColor.GREEN + "Successfully unloaded script " + script);
                } else {
                    sender.sendMessage(ChatColor.RED + "Unable to unload script " + script + ". Maybe it's already unloaded?");
                }
            } else {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
            }
        } else if (sub.equalsIgnoreCase("list")) {
            if (sender.hasPermission("skriptjava.list")) {
                sender.sendMessage(ChatColor.GREEN + "Active Scripts:");
                Set<String> names = new HashSet<>(manager.getEngine().getNames());
                for (String name : names) {
                    sender.sendMessage(ChatColor.YELLOW + " - " + name);
                }
            } else {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
            }
        } else if (sub.equalsIgnoreCase("help")) {
            List<String> available = new ArrayList<>();
            String[] all = { "reload", "reloadall", "unload", "load", "list" };
            for (String a : all) {
                if (sender.hasPermission("skriptjava." + a)) {
                    available.add(a);
                }
            }
            if (available.size() == 0) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to use that command!");
            } else {
                sender.sendMessage(ChatColor.GREEN + "Available Commands:");
                for (String a : available) {
                    sender.sendMessage(ChatColor.YELLOW + " - /skriptjava " + a);
                }
            }
        }
        else {
            sender.sendMessage(ChatColor.RED + "Usage: /skriptjava help");
        }
        return true;
    }
}

package xyz.destiall.skriptjava;

import ch.obermuhlner.scriptengine.java.JavaCompiledScript;
import ch.obermuhlner.scriptengine.java.JavaScriptEngine;
import ch.obermuhlner.scriptengine.java.JavaScriptEngineFactory;
import ch.obermuhlner.scriptengine.java.constructor.DefaultConstructorStrategy;
import ch.obermuhlner.scriptengine.java.execution.MethodExecutionStrategy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

class SimpleSkript extends Skript implements CommandExecutor {
    private final SkriptJava skriptJava;
    private final JavaScriptEngine compiler;
    private final HashMap<String, JavaCompiledScript> scripts;
    private final HashMap<JavaCompiledScript, Listener> scriptListeners;
    private final HashMap<JavaCompiledScript, SkriptCommand> scriptCommands;
    private SimpleCommandMap reflectedCommandMap;

    public SimpleSkript(SkriptJava skriptJava) {
        this.skriptJava = skriptJava;
        scripts = new HashMap<>();
        scriptListeners = new HashMap<>();
        scriptCommands = new HashMap<>();
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = new JavaScriptEngineFactory().getScriptEngine();
        engine.setBindings(manager.getBindings(), ScriptContext.GLOBAL_SCOPE);
        compiler = (JavaScriptEngine) engine;
        compiler.setConstructorStrategy(DefaultConstructorStrategy.byMatchingArguments(skriptJava));
        compiler.setExecutionStrategyFactory((clazz) -> MethodExecutionStrategy.byMatchingArguments(clazz, "load"));
        compiler.setExecutionClassLoader(Bukkit.class.getClassLoader());
    }

    public boolean loadScript(File script, boolean execute) {
        String scriptName = script.getName().substring(0, script.getName().length() - ".java".length());
        String contents = readScript(script);
        try {
            skriptJava.getLogger().info("Compiling " + scriptName + "...");
            JavaCompiledScript compiled = compiler.compile(contents);
            scripts.put(scriptName, compiled);
            if (execute) {
                evalScript(compiled);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean loadScript(String name) throws ScriptException {
        JavaCompiledScript script = scripts.get(name);
        if (script != null) return false;
        File newScript = new File(skriptJava.getDataFolder(), name + ".java");
        if (!newScript.exists()) return false;
        return loadScript(newScript, true);
    }

    public boolean unloadScript(String name, boolean remove) {
        JavaCompiledScript script = scripts.get(name);
        if (script == null) return false;
        unloadScript(script, remove);
        if (remove) scripts.remove(name);
        return true;
    }

    public boolean reloadScript(String name) throws ScriptException{
        unloadScript(name, true);
        return loadScript(name);
    }

    public String readScript(File file) {
        try {
            Scanner scanner = new Scanner(file);
            StringBuilder contents = new StringBuilder();
            while (scanner.hasNextLine()) {
                contents.append(scanner.nextLine()).append("\n");
            }
            return contents.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void evalScript(JavaCompiledScript script) throws ScriptException {
        if (script.getCompiledInstance() instanceof Listener) {
            Listener listener = (Listener) script.getCompiledInstance();
            scriptListeners.put(script, listener);
            Bukkit.getPluginManager().registerEvents(listener, skriptJava);
        }
        if (script.getCompiledInstance() instanceof CommandExecutor) {
            CommandExecutor executor = (CommandExecutor) script.getCompiledInstance();
            try {
                Method getCommand = executor.getClass().getDeclaredMethod("getCommand");
                getCommand.setAccessible(true);
                if (getCommand.getReturnType() != String.class) throw new ScriptException("getCommand does not return a String");

                String cmd = (String) getCommand.invoke(executor);
                SkriptCommand skriptCommand = new SkriptCommand(cmd, executor);
                reflectedCommandMap.register("skriptjava", skriptCommand);
                scriptCommands.put(script, skriptCommand);

            } catch (Exception e) {
                skriptJava.getLogger().warning("Script " + script.getCompiledClass() + " does not have a getCommand() method when it implements CommandExecutor! Skipping...");
            }
        }
        script.eval();
    }

    @Override
    public void execute() {
        File[] fileScripts = skriptJava.getDataFolder().listFiles((f, n) -> n.endsWith(".java"));
        if (fileScripts == null) return;

        try {
            Field cmdMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            cmdMapField.setAccessible(true);
            reflectedCommandMap = (SimpleCommandMap) cmdMapField.get(Bukkit.getServer());
            cmdMapField.setAccessible(false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        for (File script : fileScripts) {
            try {
                loadScript(script, false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (JavaCompiledScript compiled : scripts.values()) {
            try {
                evalScript(compiled);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        Bukkit.getServer().getPluginCommand("skriptjava").setExecutor(this);
    }

    public void unloadScript(JavaCompiledScript script, boolean remove) {
        Object instance = script.getCompiledInstance();
        try {
            Listener listener = scriptListeners.get(script);
            if (listener != null) {
                HandlerList.unregisterAll(listener);
                if (remove) scriptListeners.remove(script);
            }

            if (reflectedCommandMap != null && scriptCommands.containsKey(script)) {
                SkriptCommand cmd = scriptCommands.get(script);
                cmd.setExecutor(null);
                unregisterCommand(cmd);
                if (remove) scriptCommands.remove(script);
            }
            Method unload = script.getCompiledClass().getMethod("unload");
            unload.invoke(instance);
        } catch (Exception e) {
            System.err.println("Script " + script.getCompiledClass() + " does not have an unload method!");
        }
    }

    @Override
    public void unload() {
        for (JavaCompiledScript script : scripts.values()) {
            unloadScript(script, false);
        }
        scripts.clear();
        scriptListeners.clear();
        scriptCommands.clear();
        HandlerList.unregisterAll(skriptJava);
        Bukkit.getServer().getPluginCommand("skriptjava").setExecutor(null);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length > 0 ? args[0] : "";
        if (sub.equalsIgnoreCase("reload")) {
            if (sender.hasPermission("skriptjava.reload")) {
                String script = args.length > 1 ? args[1] : "";
                try {
                    if (reloadScript(script)) {
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
                Set<String> names = new HashSet<>(scripts.keySet());
                for (String n : names) {
                    try {
                        reloadScript(n);
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
                    if (loadScript(script)) {
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
                if (unloadScript(script, true)) {
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
                for (String name : scripts.keySet()) {
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

    private void unregisterCommand(SkriptCommand cmd) {
        if (reflectedCommandMap == null) return;
        try {
            Field knownCmdsField = reflectedCommandMap.getClass().getDeclaredField("knownCommands");
            knownCmdsField.setAccessible(true);
            Map<String, Command> knownCommands = (Map<String, Command>) knownCmdsField.get(reflectedCommandMap);
            knownCommands.remove(cmd.getName());
            knownCmdsField.setAccessible(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

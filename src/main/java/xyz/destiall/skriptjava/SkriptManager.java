package xyz.destiall.skriptjava;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import javax.script.ScriptException;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class SkriptManager {
    private final SkriptJava skriptJava;
    private final HashMap<Class<?>, Listener> scriptListeners;
    private final HashMap<Class<?>, SkriptCommand> scriptCommands;
    private final Engine engine;
    private SimpleCommandMap reflectedCommandMap;

    public SkriptManager(SkriptJava skriptJava) {
        this.skriptJava = skriptJava;
        this.engine = new Engine();
        scriptListeners = new HashMap<>();
        scriptCommands = new HashMap<>();
    }

    public boolean loadScript(File script) {
        String scriptName = script.getName().substring(0, script.getName().length() - ".java".length());
        String contents = FileIO.readData(script);
        try {
            skriptJava.getLogger().info("Compiling " + scriptName + "...");
            Skript skript = engine.compile(script, contents);

            Object instance = skript.getInstance();
            if (instance instanceof Listener) {
                Bukkit.getPluginManager().registerEvents((Listener) instance, skriptJava);
                scriptListeners.put(skript.getCompiledClass(), ((Listener) instance));
            }

            if (instance instanceof CommandExecutor) {
                registerCommand(skript);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean loadScript(String name) throws ScriptException {
        Skript script = engine.get(name);
        if (script != null) return false;
        File newScript = new File(skriptJava.getDataFolder(), name + ".java");
        if (!newScript.exists()) return false;
        return loadScript(newScript);
    }

    public boolean unloadScript(String name) {
        Skript script = engine.get(name);
        if (script == null) return false;
        unloadScript(script);
        engine.removeScript(name);
        return true;
    }

    public boolean reloadScript(String name) throws ScriptException{
        unloadScript(name);
        return loadScript(name);
    }

    public void unloadScript(Skript script) {
        Object instance = script.getInstance();
        try {
            Listener listener = scriptListeners.get(script.getCompiledClass());
            if (listener != null) {
                HandlerList.unregisterAll(listener);
                scriptListeners.remove(script.getCompiledClass());
            }

            if (reflectedCommandMap != null && scriptCommands.containsKey(script.getCompiledClass())) {
                SkriptCommand cmd = scriptCommands.get(script.getCompiledClass());
                cmd.setExecutor(null);
                unregisterCommand(cmd);
                scriptCommands.remove(script.getCompiledClass());
            }
            Method unload = script.getCompiledClass().getDeclaredMethod("unload");
            unload.invoke(instance);

            engine.removeScript(script);
        } catch (Exception e) {
            SkriptJava.getPlugin().getLogger().warning("Script " + script.getCompiledClass() + " does not have an unload method!");
        }
    }

    public void load() {
        try {
            Field cmdMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            cmdMapField.setAccessible(true);
            reflectedCommandMap = (SimpleCommandMap) cmdMapField.get(Bukkit.getServer());
            cmdMapField.setAccessible(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Bukkit.getServer().getPluginCommand("skriptjava").setExecutor(new SkriptInternalCommand(this));

        File[] fileScripts = skriptJava.getDataFolder().listFiles((f, n) -> n.endsWith(".java"));
        if (fileScripts == null) return;
        try {
            List<Skript> skripts = engine.compileAll(Arrays.asList(fileScripts));
            for (Skript skript : skripts) {
                Object instance = skript.getInstance();
                if (instance instanceof Listener) {
                    Bukkit.getPluginManager().registerEvents((Listener) instance, skriptJava);
                    scriptListeners.put(skript.getCompiledClass(), (Listener) instance);
                }

                if (instance instanceof CommandExecutor) {
                    registerCommand(skript);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Engine getEngine() {
        return engine;
    }

    public void unload() {
        scriptListeners.clear();
        for (SkriptCommand cmd : scriptCommands.values()) {
            unregisterCommand(cmd);
        }
        scriptCommands.clear();
        HandlerList.unregisterAll(skriptJava);
        engine.removeAll();
        Bukkit.getServer().getPluginCommand("skriptjava").setExecutor(null);
    }

    private void unregisterCommand(SkriptCommand cmd) {
        if (reflectedCommandMap == null) return;
        try {
            try {
                cmd.unregister(reflectedCommandMap);
            } catch (Exception ignored) {} // Probably has no unregister method

            try {
                Field knownCmdsField = reflectedCommandMap.getClass().getField("knownCommands");
                knownCmdsField.setAccessible(true);
                Map<String, Command> knownCommands = (Map<String, Command>) knownCmdsField.get(reflectedCommandMap);
                knownCommands.remove(cmd.getName());
                knownCommands.remove("skriptjava:" + cmd.getName());

            } catch (Exception ignored1) { // No idea why it can't find field, so using methods.
                Method knownCmdsMethod = reflectedCommandMap.getClass().getDeclaredMethod("getKnownCommands");
                knownCmdsMethod.setAccessible(true);
                Map<String, Command> knownCommands = (Map<String, Command>) knownCmdsMethod.invoke(reflectedCommandMap);
                knownCommands.remove(cmd.getName());
                knownCommands.remove("skriptjava:" + cmd.getName());
            } // At this point, the command should be fully removed from the command map.
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void registerCommand(Skript instance) {
        if (reflectedCommandMap == null) return;
        try {
            Method getCommand = instance.getCompiledClass().getDeclaredMethod("getCommand");
            getCommand.setAccessible(true);
            if (getCommand.getReturnType() != String.class) throw new ScriptException("getCommand does not return a String");
            String name = (String) getCommand.invoke(instance.getInstance());
            SkriptCommand cmd = new SkriptCommand(name, (CommandExecutor) instance.getInstance());
            reflectedCommandMap.register("skriptjava", cmd);
            scriptCommands.put(instance.getCompiledClass(), cmd);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void reload() {
        engine.removeAll();
        File[] fileScripts = skriptJava.getDataFolder().listFiles((f, n) -> n.endsWith(".java"));
        if (fileScripts == null) return;
        try {
            List<Skript> skripts = engine.compileAll(Arrays.asList(fileScripts));
            for (Skript skript : skripts) {
                Object instance = skript.getInstance();
                if (instance instanceof Listener) {
                    Bukkit.getPluginManager().registerEvents((Listener) instance, skriptJava);
                }

                if (instance instanceof CommandExecutor) {
                    registerCommand(skript);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

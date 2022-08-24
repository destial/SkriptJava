package xyz.destiall.skriptjava;

import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class SkriptJava extends JavaPlugin implements Listener {
    private SkriptManager skriptManager;
    private static SkriptJava plugin;
    @Override
    public void onEnable() {
        plugin = this;
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
            saveResource("HelloWorld.java", false);
        }
        getServer().getScheduler().runTaskLater(this, () -> {
            skriptManager = new SkriptManager(this);
            skriptManager.load();
            getServer().getPluginManager().registerEvents(this, this);
        }, 1L);
    }

    public SkriptManager getSkriptManager() {
        return skriptManager;
    }

    public static SkriptJava getPlugin() {
        return plugin;
    }

    @Override
    public void onDisable() {
        if (skriptManager != null) skriptManager.unload();
        plugin = null;
        HandlerList.unregisterAll((JavaPlugin) this);
    }

    @EventHandler
    public void onPluginUnload(PluginDisableEvent e) {
        skriptManager.getEngine().reloadLibraries();
    }

    @EventHandler
    public void onPluginLoad(PluginEnableEvent e) {
        skriptManager.getEngine().reloadLibraries();
    }
}

package xyz.destiall.skriptjava;

import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

public class SkriptJava extends JavaPlugin implements CommandExecutor {
    private SkriptManager skriptManager;
    private static SkriptJava plugin;
    @Override
    public void onEnable() {
        plugin = this;
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
            saveResource("HelloWorld.java", false);
        }
        skriptManager = new SkriptManager(this);
        skriptManager.load();
    }

    public static SkriptJava getPlugin() {
        return plugin;
    }

    @Override
    public void onDisable() {
        skriptManager.unload();
        plugin = null;
    }
}

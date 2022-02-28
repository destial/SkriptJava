package xyz.destiall.skriptjava;

import org.bukkit.command.CommandExecutor;
import org.bukkit.plugin.java.JavaPlugin;

public class SkriptJava extends JavaPlugin implements CommandExecutor {
    private SimpleSkript skript;

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
            saveResource("HelloWorld.java", false);
        }

        skript = new SimpleSkript(this);
        skript.execute();
    }

    @Override
    public void onDisable() {
        skript.unload();
    }
}

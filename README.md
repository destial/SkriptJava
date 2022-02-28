Run Java plugins without compiling them!

# Usage
Under `plugins/SkriptJava`, you can put your `.java` files to run when the server is up and loaded. This plugin will compile them at runtime and execute them.

# Execution
Each script has to have a `load()` and `unload()` function, as well as a constructor that takes in the `Plugin` parameter. There is a demo script loaded for you to see when the plugin loads for the first time.

# Listeners
You can call Bukkit functions and classes like normal. You can also implement `Listener` if you want to handle events. It's the same as you would do for a plugin.

# Commands
You can also make command scripts by implementing `CommandExecutor` and having a `getCommand()` function that returns the command name.

# Example
HelloWorld.java
```java
package skriptjava;

/**
 * You can use Bukkit imports here. It would still work.
 */
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.UUID;

/**
 * A basic Java class, with a Bukkit Listener implemented to handle events.
 */
public class HelloWorld implements Listener, CommandExecutor {

    private HashSet<UUID> uuids;
    private final Plugin skriptJava;
    public HelloWorld(Plugin plugin) {
        this.skriptJava = plugin;
        uuids = new HashSet<>();
    }

    /**
     * Every script must start with a load method. This will be called when a script is loaded.
     */
    public void load() {
        System.out.println("Hello World! This is a Demo Java Skript");
        skriptJava.getServer().getScheduler().runTaskTimer(skriptJava, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage("You can even run scheduled scripts!");
            }
        }, 0L, 20L);
    }

    /**
     * Every script can also have an unload method. This will be called when unloading the script,
     * and its better memory management. It is not necessary but it is recommended.
     */
    public void unload() {
        uuids.clear();
    }

    /**
     * You can handle events here by using normal @EventHandler annotations.
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        uuids.add(e.getPlayer().getUniqueId());
        broadcast(e.getPlayer().getUniqueId(), e.getPlayer().getName());
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent e) {
        uuids.remove(e.getPlayer().getUniqueId());
        broadcast(e.getPlayer().getUniqueId(), e.getPlayer().getName());
    }

    private void broadcast(UUID uuid, String name) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(name + " (" + uuid + ") has joined the server!");
        }
    }

    /**
     * Handle commands using Bukkit CommandExecutor interface.
     */
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        commandSender.sendMessage("This is a Demo Java Skript command!");
        return false;
    }

    /**
     * Running /demo will execute the onCommand method.
     */
    public String getCommand() {
        return "demo";
    }
}
```

# In-game Commands
- /skriptjava reload [script] - Hot-reloads a script. Case-sensitive. *(Permission: skriptjava.reload)*
- /skriptjava reloadall - Hot-reloads all active scripts. *(Permission: skriptjava.reloadall)*
- /skriptjava load [script] - Loads a new script. Case-sensitive. *(Permission: skriptjava.load)*
- /skriptjava unload [script] - Unloads an active script. Case-sensitive. *(Permission: skriptjava.unload)*
- /skriptjava list - Lists all active scripts. *(Permission: skriptjava.list)*

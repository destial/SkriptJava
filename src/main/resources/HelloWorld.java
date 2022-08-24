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
     * Every script should start with a load method. This will be called when a script is loaded.
     */
    public void load() {
        System.out.println("Hello World! This is a Demo Java Skript");
    }

    /**
     * Every script should also have an unload method. This will be called when unloading the script,
     * and its better memory management.
     */
    public void unload() {
        uuids.clear();
    }

    /**
     * You can handle events here by using normal @EventHandler annotations.
     * @param e
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
        Bukkit.getConsoleSender().sendMessage(name + " (" + uuid + ") has joined the server!");
    }

    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        commandSender.sendMessage("This is a Demo Java Skript command!");
        return false;
    }

    public String getCommand() {
        return "demo";
    }
}
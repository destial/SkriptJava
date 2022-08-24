package xyz.destiall.skriptjava;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.Collections;
import java.util.List;

public class SkriptCommandWrapper extends Command {
    private CommandExecutor executor;
    protected SkriptCommandWrapper(String name, CommandExecutor executor) {
        super(name);
        this.executor = executor;
    }

    public void setExecutor(CommandExecutor executor) {
        this.executor = executor;
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        try {
            if (executor == null) return false;
            return executor.onCommand(sender, this, label, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String label, String[] args) throws IllegalArgumentException {
        if (executor != null && executor instanceof TabExecutor) {
            List<String> tabs = null;
            try {
                tabs = ((TabExecutor) executor).onTabComplete(sender, this, label, args);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return tabs == null ? Collections.emptyList() : tabs;
        }
        return super.tabComplete(sender, label, args);
    }
}

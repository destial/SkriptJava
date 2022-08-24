package xyz.destiall.skriptjava;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class SkriptCommand extends Command {
    private CommandExecutor executor;
    protected SkriptCommand(String name, CommandExecutor executor) {
        super(name);
        this.executor = executor;
    }

    public void setExecutor(CommandExecutor executor) {
        this.executor = executor;
    }

    @Override
    public boolean execute(CommandSender commandSender, String s, String[] strings) {
        try {
            if (executor == null) return false;
            return executor.onCommand(commandSender, this, s, strings);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}

package io.papermc.testplugin;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class TestPlugin extends JavaPlugin implements Listener {

    public static final String TEST_COMMAND_ID = "test-command";
    public static final List<String> sources = List.of(TEST_COMMAND_ID, "test-command-alias");
    public static final CommandExecutor exampleCommandExecutor = new ExampleCommandExecutor();

    private volatile boolean registered = false;
    private final AtomicInteger calls = new AtomicInteger(0);

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);

        // io.papermc.testplugin.brigtests.Registration.registerViaOnEnable(this);

        this.getCommand("run-register-unregister-by-one").setExecutor((sender, command, label, args) -> {
            final BukkitTask[] task_link = new BukkitTask[1];
            task_link[0] = Bukkit.getScheduler().runTaskTimer(this, () -> {
                if (calls.get() == 99) {
                    task_link[0].cancel();
                    calls.set(0);
                }
                calls.incrementAndGet();
                registrationStep();
            }, 0, 1);
            return true;
        });
        this.getCommand("run-register-unregister-at-once").setExecutor((sender, command, label, args) -> {
            for (int i = 0; i != 100; i++) registrationStep();
            return true;
        });
        String reregisterThis = "reregister-this";
        CommandExecutor[] reregisterThisExecutorLink = new CommandExecutor[1];
        reregisterThisExecutorLink[0] = (sender, command, label, args) -> {
            unregisterCommand(reregisterThis);
            registerCommand(reregisterThis, TestPlugin.this, List.of(reregisterThis, reregisterThis+"-alias"), reregisterThisExecutorLink[0]);
            return true;
        };
        this.getCommand(reregisterThis).setExecutor(reregisterThisExecutorLink[0]);
    }

    public void registrationStep() {
        if (!registered) {
            registerCommand(TEST_COMMAND_ID, this, sources, exampleCommandExecutor);
            System.out.println("registering");
            registered = true;
        } else {
            System.out.println("unregistering");
            unregisterCommand(TEST_COMMAND_ID);
            System.out.println("unregistered successfully");
            registered = false;
        }
    }

    public static void registerCommand(
        @NotNull String id,
        @NotNull JavaPlugin owner,
        @NotNull List<String> sources,
        @NonNull CommandExecutor executor
    ) {
        var command = registerCommand(id, owner, sources);
        command.setExecutor(executor);
    }

    public static PluginCommand registerCommand(
        @NonNull String id,
        @NonNull JavaPlugin owner,
        @NonNull List<String> sources
    ) {
        try {
            CommandMap map = Bukkit.getServer().getCommandMap();
            Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, org.bukkit.plugin.Plugin.class);
            constructor.setAccessible(true);
            PluginCommand command = constructor.newInstance(id, owner);
            command.setAliases(sources);

            map.register(owner.getName(), command);
            syncCommands();
            return command;
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    public static void unregisterCommand(
        @NonNull String commandName
    ) {
        Map<String, Command> knownCommands = Bukkit.getCommandMap().getKnownCommands();
        //noinspection DataFlowIssue
        for (String alias : Bukkit.getPluginCommand(commandName).getAliases()) knownCommands.remove(alias);
        knownCommands.remove(commandName);
        syncCommands();
    }

    public static void syncCommands() {
        ((CraftServer) Bukkit.getServer()).syncCommands();
    }
}

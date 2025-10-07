package github.nighter.smartspawner.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import github.nighter.smartspawner.SmartSpawner;
import github.nighter.smartspawner.commands.give.GiveSubCommand;
import github.nighter.smartspawner.commands.hologram.HologramSubCommand;
import github.nighter.smartspawner.commands.list.ListSubCommand;
import github.nighter.smartspawner.commands.prices.PricesSubCommand;
import github.nighter.smartspawner.commands.reload.ReloadSubCommand;
import github.nighter.smartspawner.language.MessageService;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NullMarked;

import java.util.List;

@NullMarked
@RequiredArgsConstructor
public class MainCommand {
    private final SmartSpawner plugin;
    private final MessageService messageService;
    private final List<BaseSubCommand> subCommands;

    public MainCommand(SmartSpawner plugin) {
        this.plugin = plugin;
        this.messageService = plugin.getMessageService();
        this.subCommands = List.of(
                new ReloadSubCommand(plugin),
                new GiveSubCommand(plugin),
                new ListSubCommand(plugin),
                new HologramSubCommand(plugin),
                new PricesSubCommand(plugin)
        );
    }

    // Build the main command with all subcommands
    public LiteralCommandNode<CommandSourceStack> buildCommand() {
        return buildCommandWithName("smartspawner");
    }

    // Build the alias command
    public LiteralCommandNode<CommandSourceStack> buildAliasCommand() {
        return buildCommandWithName("spawner");
    }

    public LiteralCommandNode<CommandSourceStack> buildAliasCommand2() {
        return buildCommandWithName("ss");
    }

    // Helper method to build command with any name
    private LiteralCommandNode<CommandSourceStack> buildCommandWithName(String name) {
        LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal(name);

        // Add permission requirement that works for console/RCON
        builder.requires(source -> {
            CommandSender sender = source.getSender();

            // Always allow console and RCON
            if (sender instanceof ConsoleCommandSender || sender instanceof RemoteConsoleCommandSender) {
                return true;
            }

            // For players, check the base permission
            if (sender instanceof Player player) {
                return player.hasPermission("smartspawner.admin") || player.isOp();
            }

            // Allow other command senders (like command blocks) if they have permission
            return sender.hasPermission("smartspawner.admin");
        });

        // Add all subcommands to the builder
        for (BaseSubCommand subCommand : subCommands) {
            builder.then(subCommand.build());
        }

        return builder.build();
    }
}
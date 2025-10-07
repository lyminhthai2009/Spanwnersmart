package github.nighter.smartspawner.commands.reload;

import com.mojang.brigadier.context.CommandContext;
import github.nighter.smartspawner.SmartSpawner;
import github.nighter.smartspawner.commands.BaseSubCommand;
import github.nighter.smartspawner.spawner.gui.synchronization.ItemUpdater;
import github.nighter.smartspawner.spawner.properties.SpawnerData;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Map;

@NullMarked
public class ReloadSubCommand extends BaseSubCommand {

    public ReloadSubCommand(SmartSpawner plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getPermission() {
        return "smartspawner.reload";
    }

    @Override
    public String getDescription() {
        return "Reload the plugin configuration and data";
    }

    @Override
    public int execute(CommandContext<CommandSourceStack> context) {
        CommandSender sender = context.getSource().getSender();

        reloadAll(sender);
        return 1; // Success
    }

    private void reloadAll(CommandSender sender) {
        try {
            plugin.getMessageService().sendMessage(sender, "reload_command_start");

            // Log current cache stats for debugging
            if (plugin.getConfig().getBoolean("debug", false)) {
                logCacheStats();
            }

            // Clear all caches first to avoid using stale data during reload
            ItemUpdater.clearCache();
            plugin.getSpawnerItemFactory().clearAllCaches();
            plugin.getMessageService().clearKeyExistsCache();

            // Reload all configurations
            plugin.reloadConfig();

            // Reload components in dependency order
            plugin.setUpHopperHandler();
            plugin.getItemPriceManager().reload();
            plugin.getEntityLootRegistry().reload();
            plugin.getSpawnerManager().reloadSpawnerDropsAndConfigs();
            plugin.getLanguageManager().reloadLanguages();
            plugin.getSpawnerMenuUI().loadConfig();
            
            // Recheck timer placeholders after language reload to detect GUI configuration changes
            plugin.getSpawnerGuiViewManager().recheckTimerPlaceholders();

            // Reload factory AFTER its dependencies (loot registry, language manager)
            plugin.getSpawnerItemFactory().reload();
            plugin.getSpawnerManager().reloadAllHolograms();
            plugin.getRangeChecker().reload();
            plugin.reload();
            plugin.getChunkSpawnerLimiter().reloadConfig();

            // Log new cache stats after reload if in debug mode
            if (plugin.getConfig().getBoolean("debug", false)) {
                logCacheStats();
            }

            plugin.getMessageService().sendMessage(sender, "reload_command_success");
        } catch (Exception e) {
            plugin.getLogger().severe("Error during reload: " + e.getMessage());
            e.printStackTrace();
            plugin.getMessageService().sendMessage(sender, "reload_command_error");
        }
    }

    private void logCacheStats() {
        Map<String, Object> stats = plugin.getLanguageManager().getCacheStats();
        plugin.getLogger().info("Language cache statistics:");
        for (Map.Entry<String, Object> entry : stats.entrySet()) {
            plugin.getLogger().info("  " + entry.getKey() + ": " + entry.getValue());
        }
    }
}

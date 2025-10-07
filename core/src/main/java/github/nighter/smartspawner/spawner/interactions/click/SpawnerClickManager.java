package github.nighter.smartspawner.spawner.interactions.click;

import github.nighter.smartspawner.SmartSpawner;
import github.nighter.smartspawner.hooks.protections.CheckOpenMenu;
import github.nighter.smartspawner.language.MessageService;
import github.nighter.smartspawner.nms.ParticleWrapper;
import github.nighter.smartspawner.spawner.gui.main.SpawnerMenuUI;
import github.nighter.smartspawner.spawner.gui.main.SpawnerMenuFormUI;
import github.nighter.smartspawner.spawner.interactions.stack.SpawnerStackHandler;
import github.nighter.smartspawner.spawner.interactions.type.SpawnEggHandler;
import github.nighter.smartspawner.spawner.properties.SpawnerData;
import github.nighter.smartspawner.spawner.properties.SpawnerManager;
import github.nighter.smartspawner.Scheduler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SpawnerClickManager implements Listener {
    private static final long COOLDOWN_MS = 250;
    private static final long CLEANUP_INTERVAL_TICKS = 6000L; // 5 minutes

    private final SmartSpawner plugin;
    private final MessageService messageService;
    private final SpawnerManager spawnerManager;
    private final SpawnEggHandler spawnEggHandler;
    private final SpawnerStackHandler spawnerStackHandler;
    private final SpawnerMenuUI spawnerMenuUI;
    private final SpawnerMenuFormUI spawnerMenuFormUI;

    // Use ConcurrentHashMap for thread safety without explicit synchronization
    private final Map<UUID, Long> playerCooldowns = new ConcurrentHashMap<>();

    public SpawnerClickManager(SmartSpawner plugin) {
        this.plugin = plugin;
        this.messageService = plugin.getMessageService();
        this.spawnerManager = plugin.getSpawnerManager();
        this.spawnEggHandler = plugin.getSpawnEggHandler();
        this.spawnerStackHandler = plugin.getSpawnerStackHandler();
        this.spawnerMenuUI = plugin.getSpawnerMenuUI();
        this.spawnerMenuFormUI = plugin.getSpawnerMenuFormUI();
        initCleanupTask();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onSpawnerClick(PlayerInteractEvent event) {
        // Quick validation checks
        if (!isValidSpawnerInteraction(event)) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        SpawnerData spawner = spawnerManager.getSpawnerByLocation(block.getLocation());
        if (spawner == null) {
            return;
        }

        // Apply interaction cooldown
        if (!isInteractionAllowed(player)) {
            event.setCancelled(true);
            return;
        }

        ItemStack heldItem = player.getInventory().getItemInMainHand();
        ItemStack offhandItem = player.getInventory().getItemInOffHand();
        Material itemType = heldItem.getType();
        Material offhandType = offhandItem.getType();

        // Allow normal block placement when sneaking with placeable block
        if (shouldAllowNormalBlockPlacement(player, itemType)) {
            return;
        }

        // Special handling for Bedrock players using tools
        if (isBedrockPlayerUsingTool(player, itemType)) {
            return;
        }

        // Check if player is holding armor in either hand - allow equipping but don't open menu
        if (isArmor(itemType) || isArmor(offhandType)) {
            return;
        }

        // Prevent default interaction
        event.setCancelled(true);

        if (!spawner.getSpawnerActive()) {
            handleInactiveSpawnerInteraction(player, block, spawner, heldItem, itemType);
        }

        // Process spawner interaction
        handleSpawnerInteraction(player, block, heldItem, itemType, spawner);
    }

    private boolean isValidSpawnerInteraction(PlayerInteractEvent event) {
        return event.getAction() == Action.RIGHT_CLICK_BLOCK &&
                event.getClickedBlock() != null &&
                event.getClickedBlock().getType() == Material.SPAWNER;
    }

    private boolean isInteractionAllowed(Player player) {
        long currentTime = System.currentTimeMillis();
        Long lastInteraction = playerCooldowns.get(player.getUniqueId());

        if (lastInteraction != null && currentTime - lastInteraction < COOLDOWN_MS) {
            return false;
        }

        playerCooldowns.put(player.getUniqueId(), currentTime);
        return true;
    }

    private boolean shouldAllowNormalBlockPlacement(Player player, Material itemType) {
        return player.isSneaking() && itemType.isBlock() && itemType != Material.SPAWNER;
    }

    private boolean isBedrockPlayerUsingTool(Player player, Material itemType) {
        if (!isBedrockPlayer(player)) {
            return false;
        }

        String itemName = itemType.name();
        boolean isTool = itemName.endsWith("_PICKAXE") ||
                itemName.endsWith("_SHOVEL") ||
                itemName.endsWith("_HOE") ||
                itemName.endsWith("_AXE");

        return isTool;
    }

    private boolean isArmor(Material material) {
        String name = material.name();
        return name.endsWith("_HELMET") ||
                name.endsWith("_CHESTPLATE") ||
                name.endsWith("_LEGGINGS") ||
                name.endsWith("_BOOTS") ||
                material == Material.ELYTRA ||
                material == Material.CARVED_PUMPKIN ||
                material == Material.SKELETON_SKULL ||
                material == Material.WITHER_SKELETON_SKULL ||
                material == Material.ZOMBIE_HEAD ||
                material == Material.PLAYER_HEAD ||
                material == Material.DRAGON_HEAD ||
                material == Material.PIGLIN_HEAD ||
                material == Material.CREEPER_HEAD ||
                material == Material.TURTLE_HELMET;
    }

    private void handleSpawnerInteraction(Player player, Block block, ItemStack heldItem, Material itemType, SpawnerData spawner) {

        // Check permission on claimed land
        if (!CheckOpenMenu.CanPlayerOpenMenu(player, block.getLocation())) {
            messageService.sendMessage(player, "spawner_protected");
            return;
        }

        // Handle spawn egg usage
        if (isSpawnEgg(itemType)) {
            spawnEggHandler.handleSpawnEggUse(player, (CreatureSpawner) block.getState(), spawner, heldItem);
            return;
        }

        // Handle spawner stacking
        if (itemType == Material.SPAWNER) {
            spawnerStackHandler.handleSpawnerStacking(player, block, spawner, heldItem);
            return;
        }

        // Open spawner menu if not using special items
        openSpawnerMenu(player, spawner);
    }

    private void handleInactiveSpawnerInteraction(Player player, Block block, SpawnerData spawner, ItemStack heldItem, Material itemType) {

        // Check permission on claimed land
        if (!CheckOpenMenu.CanPlayerOpenMenu(player, block.getLocation())) {
            messageService.sendMessage(player, "spawner_protected");
            return;
        }

        // Handle spawn egg usage
        if (isSpawnEgg(itemType)) {
            spawnEggHandler.handleSpawnEggUse(player, (CreatureSpawner) block.getState(), spawner, heldItem);
            return;
        }

        // Activate the spawner
        spawner.setSpawnerActive(true);
        spawnerManager.queueSpawnerForSaving(spawner.getSpawnerId());
        messageService.sendMessage(player, "spawner_activated");

        // Handle spawner stacking
        if (itemType == Material.SPAWNER) {
            spawnerStackHandler.handleSpawnerStacking(player, block, spawner, heldItem);
            return;
        }

        // Open spawner menu if not using special items
        openSpawnerMenu(player, spawner);
    }

    private void openSpawnerMenu(Player player, SpawnerData spawner) {
        // Check if the player is a Bedrock player and use FormUI
        if (isBedrockPlayer(player)) {
            if (spawnerMenuFormUI != null) {
                spawnerMenuFormUI.openSpawnerForm(player, spawner);
            } else {
                // Fallback to standard GUI if FormUI not available
                spawnerMenuUI.openSpawnerMenu(player, spawner, false);
            }
        } else {
            // Open the regular GUI menu for Java players
            spawnerMenuUI.openSpawnerMenu(player, spawner, false);
        }
    }

    private boolean isSpawnEgg(Material material) {
        return material.name().endsWith("_SPAWN_EGG");
    }

    private boolean isBedrockPlayer(Player player) {
        if (plugin.getIntegrationManager() == null || 
            plugin.getIntegrationManager().getFloodgateHook() == null) {
            return false;
        }
        return plugin.getIntegrationManager().getFloodgateHook().isBedrockPlayer(player);
    }

    private void initCleanupTask() {
        Scheduler.runTaskTimer(this::cleanupCooldowns, CLEANUP_INTERVAL_TICKS, CLEANUP_INTERVAL_TICKS);
    }

    public void cleanupCooldowns() {
        long expirationThreshold = System.currentTimeMillis() - (COOLDOWN_MS * 10);
        playerCooldowns.entrySet().removeIf(entry -> entry.getValue() < expirationThreshold);
    }

    public void cleanup() {
        playerCooldowns.clear();
    }
}
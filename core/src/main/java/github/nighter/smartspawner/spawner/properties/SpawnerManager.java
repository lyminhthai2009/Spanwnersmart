package github.nighter.smartspawner.spawner.properties;

import github.nighter.smartspawner.SmartSpawner;
import github.nighter.smartspawner.spawner.utils.SpawnerFileHandler;
import github.nighter.smartspawner.Scheduler;
import org.bukkit.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class SpawnerManager {
    private final SmartSpawner plugin;
    private final Map<String, SpawnerData> spawners = new HashMap<>();
    private final Map<LocationKey, SpawnerData> locationIndex = new HashMap<>();
    private final Map<String, Set<SpawnerData>> worldIndex = new HashMap<>();
    private final SpawnerFileHandler spawnerFileHandler;
    // Set to keep track of confirmed ghost spawners to avoid repeated checks
    private final Set<String> confirmedGhostSpawners = ConcurrentHashMap.newKeySet();

    public SpawnerManager(SmartSpawner plugin) {
        this.plugin = plugin;
        this.spawnerFileHandler = plugin.getSpawnerFileHandler();
        // Initialize without loading spawners - let WorldEventHandler manage loading
        initializeWithoutLoading();
    }

    private static class LocationKey {
        private final String world;
        private final int x, y, z;

        public LocationKey(Location location) {
            this.world = location.getWorld().getName();
            this.x = location.getBlockX();
            this.y = location.getBlockY();
            this.z = location.getBlockZ();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof LocationKey)) return false;
            LocationKey that = (LocationKey) o;
            return x == that.x &&
                    y == that.y &&
                    z == that.z &&
                    world.equals(that.world);
        }

        @Override
        public int hashCode() {
            return Objects.hash(world, x, y, z);
        }
    }

    public void reloadSpawnerDrops() {
        List<SpawnerData> allSpawners = getAllSpawners();
        for (SpawnerData spawner : allSpawners) {
            try {
                spawner.setLootConfig();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to reload drops config for spawner " +
                        spawner.getSpawnerId() + ": " + e.getMessage());
            }
        }
    }

    public void reloadSpawnerDropsAndConfigs() {
        List<SpawnerData> allSpawners = getAllSpawners();
        for (SpawnerData spawner : allSpawners) {
            try {
                spawner.loadConfigurationValues();
                spawner.recalculateAfterConfigReload();
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to reload config for spawner " +
                        spawner.getSpawnerId() + ": " + e.getMessage());
            }
        }
    }

    public void addSpawner(String id, SpawnerData spawner) {
        spawners.put(id, spawner);
        locationIndex.put(new LocationKey(spawner.getSpawnerLocation()), spawner);

        // Add to world index
        String worldName = spawner.getSpawnerLocation().getWorld().getName();
        worldIndex.computeIfAbsent(worldName, k -> new HashSet<>()).add(spawner);

        // Queue for saving
        spawnerFileHandler.queueSpawnerForSaving(id);
    }

    public void removeSpawner(String id) {
        SpawnerData spawner = spawners.get(id);
        if (spawner != null) {
            Location loc = spawner.getSpawnerLocation();
            // Run hologram removal on location thread
            Scheduler.runLocationTask(loc, spawner::removeHologram);

            locationIndex.remove(new LocationKey(spawner.getSpawnerLocation()));

            // Remove from world index
            String worldName = spawner.getSpawnerLocation().getWorld().getName();
            Set<SpawnerData> worldSpawners = worldIndex.get(worldName);
            if (worldSpawners != null) {
                worldSpawners.remove(spawner);
                if (worldSpawners.isEmpty()) {
                    worldIndex.remove(worldName);
                }
            }

            spawners.remove(id);
        }
    }

    public int countSpawnersInWorld(String worldName) {
        Set<SpawnerData> worldSpawners = worldIndex.get(worldName);
        return worldSpawners != null ? worldSpawners.size() : 0;
    }

    public int countTotalSpawnersWithStacks(String worldName) {
        Set<SpawnerData> worldSpawners = worldIndex.get(worldName);
        if (worldSpawners == null) return 0;

        return worldSpawners.stream()
                .mapToInt(SpawnerData::getStackSize)
                .sum();
    }

    public SpawnerData getSpawnerByLocation(Location location) {
        return locationIndex.get(new LocationKey(location));
    }

    public SpawnerData getSpawnerById(String id) {
        return spawners.get(id);
    }

    public List<SpawnerData> getAllSpawners() {
        return new ArrayList<>(spawners.values());
    }

    public void addSpawnerToIndexes(String spawnerId, SpawnerData spawner) {
        spawners.put(spawnerId, spawner);
        locationIndex.put(new LocationKey(spawner.getSpawnerLocation()), spawner);

        // Add to world index
        String worldName = spawner.getSpawnerLocation().getWorld().getName();
        worldIndex.computeIfAbsent(worldName, k -> new HashSet<>()).add(spawner);
    }

    public Set<SpawnerData> getSpawnersInWorld(String worldName) {
        return worldIndex.get(worldName);
    }

    public void initializeWithoutLoading() {
        // Clear existing data
        spawners.clear();
        locationIndex.clear();
        worldIndex.clear();
        confirmedGhostSpawners.clear();

        // Don't load spawners - let WorldEventHandler handle it
    }

    public boolean isGhostSpawner(SpawnerData spawner) {
        if (spawner == null) return false;

        // If already confirmed as ghost, return true immediately
        if (confirmedGhostSpawners.contains(spawner.getSpawnerId())) {
            return true;
        }

        Location loc = spawner.getSpawnerLocation();
        if (loc == null || loc.getWorld() == null) return true;

        // Only check loaded chunks
        if (!loc.getChunk().isLoaded()) {
            return false; // Can't confirm, assume valid
        }

        return loc.getBlock().getType() != Material.SPAWNER;
    }

    public void removeGhostSpawner(String spawnerId) {
        SpawnerData spawner = spawners.get(spawnerId);
        if (spawner != null) {
            Location loc = spawner.getSpawnerLocation();

            // Add to confirmed list
            confirmedGhostSpawners.add(spawnerId);

            // Remove hologram and spawner data
            Scheduler.runLocationTask(loc, () -> {
                spawner.removeHologram();

                Scheduler.runTask(() -> {
                    removeSpawner(spawnerId);
                    spawnerFileHandler.markSpawnerDeleted(spawnerId);
                    plugin.debug("Removed ghost spawner " + spawnerId);
                });
            });
        }
    }

    /**
     * Marks a spawner as modified for batch saving
     *
     * @param spawnerId The ID of the modified spawner
     */
    public void markSpawnerModified(String spawnerId) {
        spawnerFileHandler.markSpawnerModified(spawnerId);
    }

    public void markSpawnerDeleted(String spawnerId) {
        spawnerFileHandler.markSpawnerDeleted(spawnerId);
    }

    /**
     * Immediately queues a spawner for saving
     *
     * @param spawnerId The ID of the spawner to save
     */
    public void queueSpawnerForSaving(String spawnerId) {
        spawnerFileHandler.queueSpawnerForSaving(spawnerId);
    }

    // ===============================================================
    //                    Spawner Hologram
    // ===============================================================

    public void refreshAllHolograms() {
        for (SpawnerData spawner : spawners.values()) {
            Location loc = spawner.getSpawnerLocation();
            Scheduler.runLocationTask(loc, spawner::refreshHologram);
        }
    }

    public void reloadAllHolograms() {
        if (plugin.getConfig().getBoolean("hologram.enabled", false)) {
            for (SpawnerData spawner : spawners.values()) {
                Location loc = spawner.getSpawnerLocation();
                Scheduler.runLocationTask(loc, spawner::reloadHologramData);
            }
        }
    }

    public void cleanupAllSpawners() {
        spawners.clear();
        locationIndex.clear();
        worldIndex.clear();
        confirmedGhostSpawners.clear();
    }

    public int getTotalSpawners() {
        return this.spawners.size();
    }
}
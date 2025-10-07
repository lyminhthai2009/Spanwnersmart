package github.nighter.smartspawner.api;

import github.nighter.smartspawner.SmartSpawner;
import github.nighter.smartspawner.spawner.item.SpawnerItemFactory;
import org.bukkit.block.BlockState;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Implementation of the SmartSpawnerAPI interface
 */
public class SmartSpawnerAPIImpl implements SmartSpawnerAPI {

    private final SmartSpawner plugin;
    private final SpawnerItemFactory itemFactory;

    public SmartSpawnerAPIImpl(SmartSpawner plugin) {
        this.plugin = plugin;
        this.itemFactory = new SpawnerItemFactory(plugin);
    }

    @Override
    public ItemStack createSpawnerItem(EntityType entityType) {
        return itemFactory.createSpawnerItem(entityType);
    }

    @Override
    public ItemStack createSpawnerItem(EntityType entityType, int amount) {
        return itemFactory.createSpawnerItem(entityType, amount);
    }

    @Override
    public EntityType getSpawnerEntityType(ItemStack item) {
        if (!isValidSpawner(item)) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta instanceof BlockStateMeta) {
            BlockStateMeta blockMeta = (BlockStateMeta) meta;
            BlockState blockState = blockMeta.getBlockState();

            if (blockState instanceof CreatureSpawner) {
                CreatureSpawner cs = (CreatureSpawner) blockState;
                return cs.getSpawnedType();
            }
        }

        return null;
    }

    @Override
    public boolean isValidSpawner(ItemStack item) {
        if (item == null || item.getType() != org.bukkit.Material.SPAWNER) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        return meta instanceof BlockStateMeta;
    }
}
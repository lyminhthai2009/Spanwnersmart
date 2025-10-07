package github.nighter.smartspawner.commands.prices;

import github.nighter.smartspawner.SmartSpawner;
import github.nighter.smartspawner.commands.prices.holders.PricesHolder;
import github.nighter.smartspawner.hooks.economy.ItemPriceManager;
import github.nighter.smartspawner.language.LanguageManager;
import github.nighter.smartspawner.language.MessageService;
import github.nighter.smartspawner.spawner.loot.EntityLootConfig;
import github.nighter.smartspawner.spawner.loot.EntityLootRegistry;
import github.nighter.smartspawner.spawner.loot.LootItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.stream.Collectors;

public class PricesGUI implements Listener {
    private final SmartSpawner plugin;
    private final LanguageManager languageManager;
    private final MessageService messageService;
    private final EntityLootRegistry lootRegistry;
    private final ItemPriceManager priceManager;
    
    private static final int ITEMS_PER_PAGE = 45;
    private static final int GUI_SIZE = 54;

    public PricesGUI(SmartSpawner plugin) {
        this.plugin = plugin;
        this.languageManager = plugin.getLanguageManager();
        this.messageService = plugin.getMessageService();
        this.lootRegistry = plugin.getEntityLootRegistry();
        this.priceManager = plugin.getItemPriceManager();
    }

    public void openPricesGUI(Player player, int page) {
        if (!player.hasPermission("smartspawner.prices")) {
            messageService.sendMessage(player, "no_permission");
            return;
        }

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);

        // Collect all unique items with prices from all entity loot configs
        Map<Material, PriceInfo> allItems = collectAllPriceableItems();
        
        if (allItems.isEmpty()) {
            messageService.sendMessage(player, "no_priceable_items");
            return;
        }

        // Sort items by material name for consistent ordering
        List<Map.Entry<Material, PriceInfo>> sortedItems = allItems.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(Material::name)))
                .toList();

        int totalPages = (int) Math.ceil((double) sortedItems.size() / ITEMS_PER_PAGE);
        if (page > totalPages) page = totalPages;
        if (page < 1) page = 1;

        Inventory inventory = Bukkit.createInventory(
                new PricesHolder(page, totalPages),
                GUI_SIZE,
                languageManager.getGuiTitle("prices_gui_title", Map.of(
                        "current_page", String.valueOf(page),
                        "total_pages", String.valueOf(totalPages)
                ))
        );

        // Add price items
        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, sortedItems.size());

        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<Material, PriceInfo> entry = sortedItems.get(i);
            ItemStack itemStack = createPriceDisplayItem(entry.getKey(), entry.getValue());
            inventory.setItem(i - startIndex, itemStack);
        }

        // Add navigation buttons
        addNavigationButtons(inventory, page, totalPages);

        player.openInventory(inventory);
    }

    private Map<Material, PriceInfo> collectAllPriceableItems() {
        Map<Material, PriceInfo> allItems = new HashMap<>();

        for (EntityType entityType : EntityType.values()) {
            EntityLootConfig lootConfig = lootRegistry.getLootConfig(entityType);
            if (lootConfig == null) continue;

            for (LootItem lootItem : lootConfig.getAllItems()) {
                if (!lootItem.isAvailable()) continue;

                Material material = lootItem.getMaterial();
                double finalPrice = priceManager.getPrice(material);
                
                if (finalPrice > 0) {
                    // Get individual price components
                    double customPrice = getCustomPrice(material);
                    double shopPrice = getShopPrice(material);
                    
                    // Determine which source is being used
                    String priceSource = determinePriceSource(material, finalPrice, customPrice, shopPrice);
                    
                    allItems.put(material, new PriceInfo(finalPrice, priceSource, customPrice, shopPrice));
                }
            }
        }

        return allItems;
    }

    private double getCustomPrice(Material material) {
        Map<String, Double> allPrices = priceManager.getAllPrices();
        return allPrices.getOrDefault(material.name(), 0.0);
    }

    private double getShopPrice(Material material) {
        if (priceManager.getShopIntegrationManager() == null) return 0.0;
        return priceManager.getShopIntegrationManager().getPrice(material);
    }

    private String determinePriceSource(Material material, double finalPrice, double customPrice, double shopPrice) {
        // Determine source by comparing final price with individual sources
        if (Math.abs(finalPrice - customPrice) < 0.001 && customPrice > 0) {
            return "Custom";
        } else if (Math.abs(finalPrice - shopPrice) < 0.001 && shopPrice > 0) {
            return "Shop";
        } else if (finalPrice > 0) {
            return "Default";
        }
        return "Unknown";
    }

    private ItemStack createPriceDisplayItem(Material material, PriceInfo priceInfo) {
        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Set display name
        String materialName = languageManager.getVanillaItemName(material);
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("item_name", materialName);
        placeholders.put("ɪᴛᴇᴍ_ɴᴀᴍᴇ", languageManager.getSmallCaps(materialName));
        placeholders.put("price", languageManager.formatNumber(priceInfo.finalPrice));
        placeholders.put("price_source", priceInfo.source);

        meta.setDisplayName(languageManager.getGuiItemName("price_item.name", placeholders));

        // Set lore with price information
        List<String> lore = new ArrayList<>();
        placeholders.put("custom_price", languageManager.formatNumber(priceInfo.customPrice));
        placeholders.put("shop_price", languageManager.formatNumber(priceInfo.shopPrice));
        
        lore.addAll(languageManager.getGuiItemLoreAsList("price_item.lore", placeholders));

        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_UNBREAKABLE);
        item.setItemMeta(meta);

        return item;
    }

    private void addNavigationButtons(Inventory inventory, int currentPage, int totalPages) {
        // Previous page button (slot 45)
        if (currentPage > 1) {
            ItemStack prevButton = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prevButton.getItemMeta();
            if (prevMeta != null) {
                prevMeta.setDisplayName(languageManager.getGuiItemName("navigation.previous_page"));
                prevButton.setItemMeta(prevMeta);
            }
            inventory.setItem(45, prevButton);
        }

        // Close button (slot 49)
        ItemStack closeButton = new ItemStack(Material.BARRIER);
        ItemMeta closeMeta = closeButton.getItemMeta();
        if (closeMeta != null) {
            closeMeta.setDisplayName(languageManager.getGuiItemName("navigation.close"));
            closeButton.setItemMeta(closeMeta);
        }
        inventory.setItem(49, closeButton);

        // Next page button (slot 53)
        if (currentPage < totalPages) {
            ItemStack nextButton = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = nextButton.getItemMeta();
            if (nextMeta != null) {
                nextMeta.setDisplayName(languageManager.getGuiItemName("navigation.next_page"));
                nextButton.setItemMeta(nextMeta);
            }
            inventory.setItem(53, nextButton);
        }
    }

    @EventHandler
    public void onPricesGUIClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder(false) instanceof PricesHolder)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        event.setCancelled(true);

        PricesHolder holder = (PricesHolder) event.getInventory().getHolder(false);
        if (holder == null) return;

        int slot = event.getSlot();
        int currentPage = holder.getCurrentPage();
        int totalPages = holder.getTotalPages();

        // Handle navigation clicks
        if (slot == 45 && currentPage > 1) {
            // Previous page
            openPricesGUI(player, currentPage - 1);
        } else if (slot == 53 && currentPage < totalPages) {
            // Next page
            openPricesGUI(player, currentPage + 1);
        } else if (slot == 49) {
            // Close
            player.closeInventory();
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
        }
    }

    private static class PriceInfo {
        final double finalPrice;
        final String source;
        final double customPrice;
        final double shopPrice;

        PriceInfo(double finalPrice, String source, double customPrice, double shopPrice) {
            this.finalPrice = finalPrice;
            this.source = source;
            this.customPrice = customPrice;
            this.shopPrice = shopPrice;
        }
    }
}
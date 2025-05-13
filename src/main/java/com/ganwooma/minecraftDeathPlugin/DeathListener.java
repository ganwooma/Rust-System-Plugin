package com.ganwooma.minecraftDeathPlugin;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DeathListener implements Listener {
    private final Plugin plugin;
    private final Map<String, Map.Entry<Zombie, Slime>> deathEntities = new HashMap<>();

    public DeathListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getEntity();

        Location location = player.getLocation();

        // Create sleeping zombie with player skin
        Zombie zombie = (Zombie) location.getWorld().spawnEntity(location, EntityType.ZOMBIE);
        zombie.setBaby(false);
        zombie.setAI(false);

        // Set player's armor and equipment
        zombie.getEquipment().setHelmet(player.getInventory().getHelmet());
        zombie.getEquipment().setChestplate(player.getInventory().getChestplate());
        zombie.getEquipment().setLeggings(player.getInventory().getLeggings());
        zombie.getEquipment().setBoots(player.getInventory().getBoots());

        zombie.setMetadata("player_death_entity", new FixedMetadataValue(plugin, player.getUniqueId().toString()));

        // Create invisible, invincible slime
        Slime slime = (Slime) location.getWorld().spawnEntity(location, EntityType.SLIME);
        slime.setSize(1);
        slime.setInvisible(true);
        slime.setMaxHealth(20.0);
        slime.setHealth(20.0);

        slime.setMetadata("death_slime", new FixedMetadataValue(plugin, player.getUniqueId().toString()));
        slime.setMetadata("player_inventory", new FixedMetadataValue(plugin, serializeInventory(player.getInventory())));

        // Store references
        deathEntities.put(player.getUniqueId().toString(), Map.entry(zombie, slime));
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        // Prevent damage to death zombie and slime
        if ((event.getEntity() instanceof Zombie zombie && zombie.hasMetadata("player_death_entity")) ||
                (event.getEntity() instanceof Slime slime && slime.hasMetadata("death_slime"))) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Map.Entry<Zombie, Slime> deathEntity = deathEntities.remove(player.getUniqueId().toString());

        if (deathEntity != null) {
            deathEntity.getKey().remove();
            deathEntity.getValue().remove();
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        var clicked = event.getRightClicked();
        var player = event.getPlayer();

        // Check if clicked entity is the death slime
        if (clicked instanceof Slime slime && slime.hasMetadata("death_slime")) {
            String playerUuid = slime.getMetadata("death_slime").get(0).asString();
            String inventoryData = slime.getMetadata("player_inventory").get(0).asString();

            // Deserialize and show inventory
            Inventory inventory = deserializeInventory(inventoryData);
            player.openInventory(inventory);

            // Prevent further interaction
            event.setCancelled(true);
        }
    }

    private String serializeInventory(org.bukkit.inventory.PlayerInventory inventory) {
        List<String> items = new ArrayList<>();

        // Serialize each inventory slot
        for (ItemStack item : inventory.getContents()) {
            items.add(item != null ? itemToString(item) : "null");
        }

        return String.join(";", items);
    }

    private Inventory deserializeInventory(String data) {
        Inventory inventory = plugin.getServer().createInventory(null, 45, "Player Death Inventory");
        String[] items = data.split(";");

        for (int i = 0; i < items.length; i++) {
            if (!"null".equals(items[i])) {
                ItemStack item = stringToItem(items[i]);
                inventory.setItem(i, item);
            }
        }

        return inventory;
    }

    private String itemToString(ItemStack item) {
        // Serialize item with additional metadata
        String base = item.getType() + ":" + item.getAmount();

        // Add enchantments
        if (!item.getEnchantments().isEmpty()) {
            String enchants = item.getEnchantments().entrySet().stream()
                    .map(e -> e.getKey().getName() + "," + e.getValue())
                    .collect(Collectors.joining(","));
            base += ":" + enchants;
        }

        return base;
    }

    private ItemStack stringToItem(String str) {
        String[] parts = str.split(":");
        Material material = Material.valueOf(parts[0]);
        int amount = Integer.parseInt(parts[1]);

        ItemStack item = new ItemStack(material, amount);

        // Restore enchantments if exists
        if (parts.length > 2) {
            for (int i = 2; i < parts.length; i += 2) {
                Enchantment enchantment = Enchantment.getByName(parts[i]);
                int level = Integer.parseInt(parts[i + 1]);
                if (enchantment != null) {
                    item.addEnchantment(enchantment, level);
                }
            }
        }

        return item;
    }

    // Helper method to check for specific item curses
    private boolean hasBindingCurse(ItemStack item) {
        return item != null && item.hasItemMeta() &&
                item.getItemMeta().hasEnchant(Enchantment.BINDING_CURSE);
    }

    private boolean hasVanishingCurse(ItemStack item) {
        return item != null && item.hasItemMeta() &&
                item.getItemMeta().hasEnchant(Enchantment.VANISHING_CURSE);
    }
}
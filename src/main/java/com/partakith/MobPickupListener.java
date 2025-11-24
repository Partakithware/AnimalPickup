package com.partakith;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.NamespacedKey;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;

public class MobPickupListener implements Listener {
	
private final APCore plugin;
public MobPickupListener(APCore plugin) {
    this.plugin = plugin;
}

    private final NamespacedKey entityDataKey = new NamespacedKey(APCore.getPlugin(APCore.class), "picked_up_entity_data");

    @EventHandler
    public void onMobPickup(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        
        if (!player.isSneaking() || !isPickuppable(player, entity.getType())) {
            return;
        }
        
        // 1. Cancel the event immediately to prevent default behavior.
        event.setCancelled(true);
        
        String entityData = EntitySerializer.serializeEntity(entity);
        if (entityData == null) {
            player.sendMessage("§cCould not retrieve entity data.");
            return;
        }
        
        ItemStack customEgg = createCustomEgg(entity.getType(), entityData);
        
     // 2. Remove the entity immediately. This should be safe now that the event is cancelled.
        entity.remove(); 

        // 3. Schedule the item addition for the next tick to ensure no item is duplicated
        // or dropped by the entity.
        Bukkit.getScheduler().runTask(plugin, () -> {
            player.getInventory().addItem(customEgg);
            player.sendMessage("§aYou picked up a " + entity.getType().name() + "!");
        });
    }

    @EventHandler
    public void onMobPlacement(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }
        
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        String entityData = meta.getPersistentDataContainer().get(entityDataKey, PersistentDataType.STRING);
        
        if (entityData == null) {
            return;
        }

        event.setCancelled(true);
        
        Location placementLocation;
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            Block clickedBlock = event.getClickedBlock();
            placementLocation = clickedBlock.getLocation().add(0.5, 1.0, 0.5); 
        } else { // RIGHT_CLICK_AIR
            placementLocation = player.getLocation().add(player.getLocation().getDirection().multiply(1.5));
        }
        
        Entity placedEntity = EntitySerializer.deserializeAndSpawn(entityData, placementLocation);

        if (placedEntity != null) {
            player.sendMessage("§aEntity successfully placed!");
            item.setAmount(item.getAmount() - 1);
        } else {
            player.sendMessage("§cFailed to place the entity!");
        }
    }

   /* private boolean isPickuppable(EntityType type) {
        // Only allow non-hostile/passive/utility mobs
        if (type == EntityType.COW ||
            type == EntityType.PIG ||
            type == EntityType.SHEEP ||
            type == EntityType.CHICKEN ||
            type == EntityType.WOLF ||
            type == EntityType.CAT ||
            type == EntityType.HORSE ||
            type == EntityType.VILLAGER ||
            type == EntityType.TRADER_LLAMA ||
            type == EntityType.LLAMA ||
            type == EntityType.CAMEL ||
            type == EntityType.ALLAY ||
            type == EntityType.AXOLOTL ||
            type == EntityType.BEE ||
            type == EntityType.ARMADILLO ||
            type == EntityType.DONKEY ||
            type == EntityType.FOX ||
            type == EntityType.MOOSHROOM) {
            return true;
        }
        return false;
    } */
    private boolean isPickuppable(Player player, EntityType type) {
        // OP override
        if (player.isOp()) return true;

        FileConfiguration cfg = plugin.getPickupConfig();
        if (!cfg.isConfigurationSection("pickuppables")) return false;

        for (String permission : cfg.getConfigurationSection("pickuppables").getKeys(false)) {
            if (player.hasPermission(permission)) {
                List<String> allowed = cfg.getStringList("pickuppables." + permission);
                for (String ent : allowed) {
                    if (ent.equalsIgnoreCase(type.name())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    private ItemStack createCustomEgg(EntityType type, String entityData) {
        Material eggMaterial = Material.getMaterial(type.name() + "_SPAWN_EGG");
        if (eggMaterial == null) {
            eggMaterial = Material.STRUCTURE_VOID; 
        }

        ItemStack item = new ItemStack(eggMaterial);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName("§b[Picked-Up] §f" + type.name().replace("_", " "));
        meta.getPersistentDataContainer().set(entityDataKey, PersistentDataType.STRING, entityData);

        item.setItemMeta(meta);
        return item;
    }
}
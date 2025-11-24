package com.partakith;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.World;
import org.bukkit.util.Vector;

import de.tr7zw.changeme.nbtapi.NBTContainer;
import de.tr7zw.changeme.nbtapi.NBTEntity;

import java.lang.reflect.Method;
import java.util.Base64;
import java.util.logging.Level;
import java.util.UUID; // Added UUID import

// REVERTED IMPORTS to use the original (non-relocated) package path:
import org.bukkit.Bukkit;

/*The MIT License (MIT)

Copyright (c) 2015 - 2024, tr7zw

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE. */

public class EntitySerializer {

    // --- Serialization (Pickup) ---

    /**
     * Serializes an Entity's NBT data into a Base64 encoded string.
     * Uses Entity.getAsString() to get the raw Mojangson string.
     */
	public static String serializeEntity(Entity entity) {
	    try {
	        // Wrap Bukkit entity in NBT API wrapper
	        NBTEntity nbt = new NBTEntity(entity);

	        // Convert full NBT to SNBT (stringified Mojangson)
	        String nbtString = nbt.toString();

	        // Save type + SNBT
	        String full = entity.getType().name() + "|" + nbtString;

	        return Base64.getEncoder().encodeToString(full.getBytes());
	    } catch (Exception e) {
	        Bukkit.getLogger().severe("Failed to serialize entity: " + e.getMessage());
	        return null;
	    }
	}

    // --- Deserialization (Placement) ---
    
    /**
     * Deserializes Base64 data, spawns a new entity, and loads the data into it 
     * using the stable Bukkit API if available.
     */
	public static Entity deserializeAndSpawn(String base64, Location loc) {
	    try {
	        String full = new String(Base64.getDecoder().decode(base64));
	        String[] parts = full.split("\\|", 2);

	        EntityType type = EntityType.valueOf(parts[0]);
	        String nbtString = parts[1];

	        // 1. Spawn a "blank" entity (This gives the entity a clean, new UUID)
	        Entity entity = loc.getWorld().spawnEntity(loc, type);

	        NBTEntity nbtEntity = new NBTEntity(entity);
	        NBTContainer container = new NBTContainer(nbtString);

	        // 2. >>> ESSENTIAL FIX <<<
	        // Remove the old UUID tags from the serialized NBT before merging.
	        // This forces the new entity to keep its clean UUID from step 1.
	        container.removeKey("UUID"); 
	        container.removeKey("UUIDMost"); 
	        container.removeKey("UUIDLeast"); 
	        // 3. Merge saved NBT into the new entity
	        nbtEntity.mergeCompound(container);

	        // Ensure proper placement — some NBT might override pos
	        entity.teleport(loc);
	        entity.setVelocity(new Vector(0, 0, 0));

	        return entity;

	    } catch (Exception e) {
	        Bukkit.getLogger().severe("Failed to deserialize and spawn entity: " + e.getMessage());
	        return null;
	    }
	}


}
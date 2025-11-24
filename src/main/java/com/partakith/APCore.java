package com.partakith;

import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.File;
import java.io.IOException;
import java.util.List;

public class APCore extends JavaPlugin {

    private File configFile;
    private FileConfiguration config;
    // Called when the plugin is enabled
    @Override
    public void onEnable() {
    	loadCustomConfig();
        // Register the event listener class
    	getServer().getPluginManager().registerEvents(new MobPickupListener(this), this);
        getLogger().info("AnimalPickup has been enabled!");
    }
    
    public void loadCustomConfig() {
        configFile = new File(getDataFolder(), "pickup-config.yml");
        if (!configFile.exists()) {
            saveResource("pickup-config.yml", false); // optional default included in jar
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public FileConfiguration getPickupConfig() {
        return config;
    }

    public void savePickupConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            getLogger().severe("Could not save pickup-config.yml: " + e.getMessage());
        }
    }

    // Called when the plugin is disabled
    @Override
    public void onDisable() {
        getLogger().info("AnimalPickup has been disabled.");
    }
}
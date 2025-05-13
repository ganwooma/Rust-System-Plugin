package com.ganwooma.minecraftDeathPlugin;

import org.bukkit.plugin.java.JavaPlugin;

public class MinecraftDeathPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getLogger().info("DeathMechanicsPlugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("DeathMechanicsPlugin has been disabled!");
    }
}
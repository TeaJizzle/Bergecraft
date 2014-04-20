package com.valadian.bergecraft.bergeypvp;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class WeaponTimer extends BukkitRunnable {
	 
    private final Player player;
    private final ItemStack item;
 
    private short initialDurability;
    private long end;
    private long timerInMillis;
 
    public WeaponTimer(Player player, ItemStack item, long start, long timerInMillis) {
        this.player = player;
        this.item = item;
        if (timerInMillis < 1) {
            throw new IllegalArgumentException("counter must be greater than 1");
        } else {
            this.timerInMillis = timerInMillis;
            this.end = start + timerInMillis;
            this.initialDurability = item.getDurability();
        }
    }
    public boolean cancelled = false;
    @Override
    public void run() {
        // What you want to schedule goes here
    	long now = System.currentTimeMillis();
        if (now < end) { 
        	short max = item.getType().getMaxDurability();
        	item.setDurability((short)((end - now) * max / timerInMillis));
        } else {
        	if(!cancelled)
        	{
                cancelled = true;
            	player.sendMessage("Weapon Cooldown finished!");
            	item.setDurability(initialDurability);
                this.cancel();
        	}
        	else
        	{
            	player.sendMessage("Waiting to cancel!");
        	}
        }
    }
    public void resetTimer()
    {
    	long now = System.currentTimeMillis();
    	end = now + timerInMillis;
    }
}
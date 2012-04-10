package com.nisovin.magicspells.util;

import java.util.Set;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

public interface CraftBukkitHandle {

	public void playPotionEffect(Player player, Entity entity, int color, int duration);
	
	public void addPotionGraphicalEffect(LivingEntity entity, int color, int duration);
	
	public void entityPathTo(LivingEntity entity, LivingEntity target);
	
	public void makeMinion(LivingEntity entity, Player master);
	
	public void queueChunksForUpdate(Player player, Set<Chunk> chunks);
	
	public void sendFakeSlotUpdate(Player player, int slot, ItemStack item);
	
	public void stackByData(int itemId, String var);
	
	public void toggleLeverOrButton(Block block);
	
	public void pressPressurePlate(Block block);
	
	public void removeMobEffect(LivingEntity entity, PotionEffectType type);
	
	public void collectItem(Player player, Item item);
	
	public boolean simulateTnt(Location target, float explosionSize);
	
	public boolean createExplosionByPlayer(Player player, Location location, float size);
	
	public void setExperienceBar(Player player, int level, float percent);
	
}

package com.nisovin.magicspells.volatilecode;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public interface VolatileCodeHandle {
	
	public void addPotionGraphicalEffect(LivingEntity entity, int color, int duration);
	
	public void entityPathTo(LivingEntity entity, LivingEntity target);
	
	public void sendFakeSlotUpdate(Player player, int slot, ItemStack item);
	
	public void toggleLeverOrButton(Block block);
	
	public void pressPressurePlate(Block block);
	
	public boolean simulateTnt(Location target, LivingEntity source, float explosionSize, boolean fire);
	
	public boolean createExplosionByPlayer(Player player, Location location, float size, boolean fire, boolean breakBlocks);
	
	public void playExplosionEffect(Location location, float size);
	
	public void setExperienceBar(Player player, int level, float percent);
	
	public Fireball shootSmallFireball(Player player);
	
	public void setTarget(LivingEntity entity, LivingEntity target);
	
	public void playSound(Location location, String sound, float volume, float pitch);
	
	public void playSound(Player player, String sound, float volume, float pitch);
	
	public ItemStack addFakeEnchantment(ItemStack item);
	
	public void setFallingBlockHurtEntities(FallingBlock block, float damage, int max);
	
	//public void addPotionEffect(LivingEntity entity, PotionEffect effect, boolean ambient);
	
	public void playEntityAnimation(Location location, EntityType entityType, int animationId, boolean instant);
	
	public void createFireworksExplosion(Location location, boolean flicker, boolean trail, int type, int[] colors, int[] fadeColors, int flightDuration);
	
	//public void setHeldItemSlot(Player player, int slot);
	
	public void playParticleEffect(Location location, String name, float spreadHoriz, float spreadVert, float speed, int count, int radius, float yOffset);
	
}

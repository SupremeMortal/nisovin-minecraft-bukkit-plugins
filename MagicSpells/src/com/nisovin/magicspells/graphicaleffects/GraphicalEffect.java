package com.nisovin.magicspells.graphicaleffects;

import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

/**
 * 
 * Represents a graphical effect that can be used with the 'effects' option of a spell.
 *
 */
public abstract class GraphicalEffect {
	
	/**
	 * Plays an effect on the specified entity.
	 * @param entity the entity to play the effect on
	 * @param param the parameter specified in the spell config (can be ignored)
	 */
	public void playEffect(Entity entity, String param) {
		playEffect(entity.getLocation(), param);
	}
	
	/**
	 * Plays an effect at the specified location.
	 * @param location location to play the effect at
	 * @param param the parameter specified in the spell config (can be ignored)
	 */
	public void playEffect(Location location, String param) {
		
	}
	
	/**
	 * Plays an effect between two locations (such as a smoke trail type effect).
	 * @param location1 the starting location
	 * @param location2 the ending location
	 * @param param the parameter specified in the spell config (can be ignored)
	 */
	public void playEffect(Location location1, Location location2, String param) {
		
	}
	
	private static HashMap<String, GraphicalEffect> effects = new HashMap<String, GraphicalEffect>();
	
	/**
	 * Gets the GraphicalEffect by the provided name.
	 * @param name the name of the effect
	 * @return
	 */
	public static GraphicalEffect getEffectByName(String name) {
		return effects.get(name);
	}
	
	/**
	 * Adds an effect with the provided name to the list of available effects.
	 * This will replace an existing effect if the same name is used.
	 * @param name the name of the effect
	 * @param effect the effect to add
	 */
	public static void addEffect(String name, GraphicalEffect effect) {
		effects.put(name, effect);
	}
	
	static {
		effects.put("bigsmoke", new BigSmokeEffect());
		effects.put("blockbreak", new BlockBreakEffect());
		effects.put("cloud", new CloudEffect());
		effects.put("ender", new EnderSignalEffect());
		effects.put("explosion", new ExplosionEffect());
		effects.put("hearts", new HeartsEffect());
		effects.put("itemspray", new ItemSprayEffect());
		effects.put("lightning", new LightningEffect());
		effects.put("potion", new PotionEffect());
		effects.put("smoke", new SmokeEffect());
		effects.put("smokeswirl", new SmokeSwirlEffect());
		effects.put("smoketrail", new SmokeTrailEffect());
		effects.put("spawn", new MobSpawnerEffect());
		effects.put("splash", new SplashPotionEffect());
	}
	
}

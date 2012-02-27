package com.nisovin.magicspells.spells.instant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Egg;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class ProjectileSpell extends InstantSpell {

	private String projectileType;
	private double velocity;
	private boolean requireHitEntity;
	private boolean cancelDamage;
	private boolean removeProjectile;
	private int maxDistanceSquared;
	private List<String> spellNames;
	private List<TargetedSpell> spells;
	private String strHitCaster;
	private String strHitTarget;
	
	private HashMap<Projectile, ProjectileInfo> projectiles;
	
	public ProjectileSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		projectileType = getConfigString("projectile", "arrow");
		velocity = getConfigFloat("velocity", 0);
		requireHitEntity = getConfigBoolean("require-hit-entity", false);
		cancelDamage = getConfigBoolean("cancel-damage", true);
		removeProjectile = getConfigBoolean("remove-projectile", true);
		maxDistanceSquared = getConfigInt("max-distance", 0);
		maxDistanceSquared = maxDistanceSquared * maxDistanceSquared;
		spellNames = getConfigStringList("spells", null);
		strHitCaster = getConfigString("str-hit-caster", "");
		strHitTarget = getConfigString("str-hit-target", "");
		
		projectiles = new HashMap<Projectile, ProjectileInfo>();
	}
	
	@Override
	public void initialize() {
		spells = new ArrayList<TargetedSpell>();
		if (spellNames != null) {
			for (String spellName : spellNames) {
				Spell spell = MagicSpells.getSpellByInternalName(spellName);
				if (spell != null && (spell instanceof TargetedEntitySpell || spell instanceof TargetedLocationSpell)) {
					spells.add((TargetedSpell)spell);
				} else {
					MagicSpells.error("Projectile spell '" + internalName + "' attempted to add invalid spell '" + spellName + "'.");
				}
			}
		}
		if (spells.size() == 0) {
			MagicSpells.error("Projectile spell '" + internalName + "' has no spells!");
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Class<? extends Projectile> projectileClass = null;
			if (projectileType.equalsIgnoreCase("arrow")) {
				projectileClass = Arrow.class;
			} else if (projectileType.equalsIgnoreCase("snowball")) {
				projectileClass = Snowball.class;
			} else if (projectileType.equalsIgnoreCase("egg")) {
				projectileClass = Egg.class;
			} else if (projectileType.equalsIgnoreCase("enderpearl")) {
				projectileClass = EnderPearl.class;
			}
			if (projectileClass != null) {
				Projectile projectile = player.launchProjectile(projectileClass);
				if (velocity > 0) {
					projectile.setVelocity(player.getLocation().getDirection().multiply(velocity));
				}
				projectiles.put(projectile, new ProjectileInfo(player, power));
				playGraphicalEffects(1, projectile);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onEntityDamage(EntityDamageByEntityEvent event) {
		if (!(event.getDamager() instanceof Projectile)) return;
		
		Projectile projectile = (Projectile)event.getDamager();
		ProjectileInfo info = projectiles.get(projectile);
		if (info == null || event.isCancelled()) return;
				
		if (!info.done && (maxDistanceSquared == 0 || projectile.getLocation().distanceSquared(info.start) <= maxDistanceSquared) && event.getEntity() instanceof LivingEntity) { 
			LivingEntity target = (LivingEntity)event.getEntity();
			
			// call target event
			SpellTargetEvent evt = new SpellTargetEvent(this, info.player, target);
			Bukkit.getPluginManager().callEvent(evt);
			if (evt.isCancelled()) {
				return;
			} else {
				target = evt.getTarget();
			}
			
			// run spells
			for (TargetedSpell spell : spells) {
				if (spell instanceof TargetedEntitySpell) {
					((TargetedEntitySpell)spell).castAtEntity(info.player, target, info.power);
					playGraphicalEffects(2, target);
				} else if (spell instanceof TargetedLocationSpell) {
					((TargetedLocationSpell)spell).castAtLocation(info.player, target.getLocation(), info.power);
					playGraphicalEffects(2, target.getLocation());
				}
			}
			info.done = true;
			
			// send messages
			String entityName;
			if (target instanceof Player) {
				entityName = ((Player)target).getDisplayName();
			} else {
				EntityType entityType = target.getType();
				entityName = MagicSpells.entityNames.get(entityType);
				if (entityName == null) {
					entityName = entityType.getName();
				}
			}
			sendMessage(info.player, strHitCaster, "%t", entityName);
			if (target instanceof Player) {
				sendMessage((Player)target, strHitTarget, "%a", info.player.getDisplayName());
			}
		}
		
		if (cancelDamage) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onProjectileHit(ProjectileHitEvent event) {
		final Projectile projectile = (Projectile)event.getEntity();
		ProjectileInfo info = projectiles.get(projectile);
		if (info != null) {
			if (!requireHitEntity && !info.done && (maxDistanceSquared == 0 || projectile.getLocation().distanceSquared(info.start) <= maxDistanceSquared)) {
				for (TargetedSpell spell : spells) {
					if (spell instanceof TargetedLocationSpell) {
						((TargetedLocationSpell)spell).castAtLocation(info.player, projectile.getLocation(), info.power);
						playGraphicalEffects(2, projectile.getLocation());
					}
				}
				info.done = true;
				sendMessage(info.player, strHitCaster);
			}
			// remove it from world
			if (removeProjectile) {
				projectile.remove();
			}
			// remove it later just in case it didn't hit anything
			Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
				public void run() {
					projectiles.remove(projectile);
				}
			}, 0);
		}
	}
	
	private class ProjectileInfo {
		Player player;
		Location start;
		float power;
		boolean done;
		
		public ProjectileInfo(Player player, float power) {
			this.player = player;
			this.start = player.getLocation().clone();
			this.power = power;
			this.done = false;
		}
	}

}

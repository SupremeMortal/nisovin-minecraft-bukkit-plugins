package com.nisovin.magicspells.spells.instant;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.BlockUtils;
import com.nisovin.magicspells.util.BoundingBox;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.Util;

public class ParticleProjectileSpell extends InstantSpell {

	float projectileVelocity;
	float projectileGravity;
	float projectileSpread;
	boolean powerAffectsVelocity;
	
	int tickInterval;
	float ticksPerSecond;
	int specialEffectInterval;
	int spellInterval;
	
	String particleName;
	float particleSpeed;
	int particleCount;
	float particleHorizontalSpread;
	float particleVerticalSpread;
	
	int maxDistanceSquared;
	int maxDuration;
	float hitRadius;
	float verticalHitRadius;
	int renderDistance;
	
	boolean hugSurface;
	float heightFromSurface;
	
	boolean hitPlayers;
	boolean hitNonPlayers;
	boolean hitSelf;
	boolean hitGround;
	boolean hitAirAtEnd;
	boolean hitAirDuring;
	boolean stopOnHitEntity;
	boolean stopOnHitGround;
	
	String landSpellName;
	TargetedSpell spell;
	
	ParticleProjectileSpell thisSpell;
	Random rand = new Random();

	public ParticleProjectileSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		thisSpell = this;
		
		projectileVelocity = getConfigFloat("projectile-velocity", 10F);
		projectileGravity = getConfigFloat("projectile-gravity", 0.25F);
		projectileSpread = getConfigFloat("projectile-spread", 0F);
		powerAffectsVelocity = getConfigBoolean("power-affects-velocity", true);
		
		tickInterval = getConfigInt("tick-interval", 2);
		ticksPerSecond = 20F / (float)tickInterval;
		specialEffectInterval = getConfigInt("special-effect-interval", 0);
		spellInterval = getConfigInt("spell-interval", 20);
		
		particleName = getConfigString("particle-name", "reddust");
		particleSpeed = getConfigFloat("particle-speed", 0.3F);
		particleCount = getConfigInt("particle-count", 15);
		particleHorizontalSpread = getConfigFloat("particle-horizontal-spread", 0.3F);
		particleVerticalSpread = getConfigFloat("particle-vertical-spread", 0.3F);
		
		maxDistanceSquared = getConfigInt("max-distance", 15);
		maxDistanceSquared *= maxDistanceSquared;
		maxDuration = getConfigInt("max-duration", 0) * 1000;
		hitRadius = getConfigFloat("hit-radius", 1.5F);
		verticalHitRadius = getConfigFloat("vertical-hit-radius", hitRadius);
		renderDistance = getConfigInt("render-distance", 32);
		
		hugSurface = getConfigBoolean("hug-surface", false);
		if (hugSurface) {
			heightFromSurface = getConfigFloat("height-from-surface", .6F);
		} else {
			heightFromSurface = 0;
		}
		
		hitPlayers = getConfigBoolean("hit-players", false);
		hitNonPlayers = getConfigBoolean("hit-non-players", true);
		hitSelf = getConfigBoolean("hit-self", false);
		hitGround = getConfigBoolean("hit-ground", true);
		hitAirAtEnd = getConfigBoolean("hit-air-at-end", false);
		hitAirDuring = getConfigBoolean("hit-air-during", false);
		stopOnHitEntity = getConfigBoolean("stop-on-hit-entity", true);
		stopOnHitGround = getConfigBoolean("stop-on-hit-ground", true);
		
		landSpellName = getConfigString("spell", "explode");
	}
	
	@Override
	public void initialize() {
		super.initialize();
		
		Spell s = MagicSpells.getSpellByInternalName(landSpellName);
		if (s != null && s instanceof TargetedSpell) {
			spell = (TargetedSpell)s;
		} else {
			MagicSpells.error("ParticleProjectileSpell " + internalName + " has an invalid spell defined!");
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			new ProjectileTracker(player, power);
			playSpellEffects(EffectPosition.CASTER, player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	class ProjectileTracker implements Runnable {
		
		Player caster;
		float power;
		long startTime;
		Location startLocation;
		Location previousLocation;
		Location currentLocation;
		Vector currentVelocity;
		int currentX;
		int currentZ;
		int taskId;
		List<LivingEntity> inRange;
		Map<LivingEntity, Long> immune;
		
		int counter = 0;
		
		public ProjectileTracker(Player caster, float power) {
			this.caster = caster;
			this.power = power;
			this.startTime = System.currentTimeMillis();
			this.startLocation = caster.getLocation();
			this.startLocation.setY(this.startLocation.getY() + 1);
			this.startLocation.add(this.startLocation.getDirection());
			this.previousLocation = startLocation.clone();
			this.currentLocation = startLocation.clone();
			this.currentVelocity = caster.getLocation().getDirection();
			if (projectileSpread > 0) {
				this.currentVelocity.add(new Vector(rand.nextFloat() * projectileSpread, rand.nextFloat() * projectileSpread, rand.nextFloat() * projectileSpread));
			}
			if (powerAffectsVelocity) {
				this.currentVelocity.multiply(power);
			}
			if (hugSurface) {
				this.currentLocation.setY((int)this.currentLocation.getY() + heightFromSurface);
				this.currentVelocity.setY(0);
			}
			this.currentVelocity.multiply(projectileVelocity / ticksPerSecond);
			this.taskId = MagicSpells.scheduleRepeatingTask(this, 0, tickInterval);
			if (hitPlayers || hitNonPlayers) {
				this.inRange = currentLocation.getWorld().getLivingEntities();
				Iterator<LivingEntity> iter = inRange.iterator();
				while (iter.hasNext()) {
					LivingEntity e = iter.next();
					if (!hitSelf && e.equals(caster)) {
						iter.remove();
						continue;
					}
					if (!hitPlayers && e instanceof Player) {
						iter.remove();
						continue;
					}
					if (!hitNonPlayers && !(e instanceof Player)) {
						iter.remove();
						continue;
					}
				}
			}
			this.immune = new HashMap<LivingEntity, Long>();
		}
		
		@Override
		public void run() {
			if (!caster.isValid()) {
				stop();
				return;
			}
			
			// check if duration is up
			if (maxDuration > 0 && startTime + maxDuration < System.currentTimeMillis()) {
				stop();
				return;
			}
			
			// move projectile and apply gravity
			previousLocation = currentLocation.clone();
			currentLocation.add(currentVelocity);
			if (hugSurface) {
				if (currentLocation.getBlockX() != currentX || currentLocation.getBlockZ() != currentZ) {
					Block b = currentLocation.subtract(0, heightFromSurface, 0).getBlock();
					if (BlockUtils.isPathable(b)) {
						int attempts = 0;
						boolean ok = false;
						while (attempts++ < 10) {
							b = b.getRelative(BlockFace.DOWN);
							if (BlockUtils.isPathable(b)) {
								currentLocation.add(0, -1, 0);
							} else {
								ok = true;
								break;
							}
						}
						if (!ok) {
							stop();
							return;
						}
					} else {
						int attempts = 0;
						boolean ok = false;
						while (attempts++ < 10) {
							b = b.getRelative(BlockFace.UP);
							currentLocation.add(0, 1, 0);
							if (BlockUtils.isPathable(b)) {
								ok = true;
								break;
							}
						}
						if (!ok) {
							stop();
							return;
						}
					}
					currentLocation.setY((int)currentLocation.getY() + heightFromSurface);
					currentX = currentLocation.getBlockX();
					currentZ = currentLocation.getBlockZ();
				}
			} else if (projectileGravity != 0) {
				currentVelocity.setY(currentVelocity.getY() - (projectileGravity / ticksPerSecond));
			}
			
			// show particle
			MagicSpells.getVolatileCodeHandler().playParticleEffect(currentLocation, particleName, particleHorizontalSpread, particleVerticalSpread, particleSpeed, particleCount, renderDistance, 0F);
			
			// play effects
			if (specialEffectInterval > 0 && counter % specialEffectInterval == 0) {
				playSpellEffects(EffectPosition.SPECIAL, currentLocation);
			}
			
			counter++;
			
			// cast spell mid air
			if (hitAirDuring && counter % spellInterval == 0 && spell instanceof TargetedLocationSpell) {
				((TargetedLocationSpell)spell).castAtLocation(caster, currentLocation, power);
			}
			
			if (stopOnHitGround && !BlockUtils.isPathable(currentLocation.getBlock())) {
				if (hitGround && spell != null && spell instanceof TargetedLocationSpell) {
					Util.setLocationFacingFromVector(previousLocation, currentVelocity);
					((TargetedLocationSpell)spell).castAtLocation(caster, previousLocation, power);
					playSpellEffects(EffectPosition.TARGET, currentLocation);
				}
				stop();
			} else if (currentLocation.distanceSquared(startLocation) >= maxDistanceSquared) {
				if (hitAirAtEnd && spell != null && spell instanceof TargetedLocationSpell) {
					((TargetedLocationSpell)spell).castAtLocation(caster, currentLocation, power);
					playSpellEffects(EffectPosition.TARGET, currentLocation);
				}
				stop();
			} else if (inRange != null) {
				BoundingBox hitBox = new BoundingBox(currentLocation, hitRadius, verticalHitRadius);
				for (int i = 0; i < inRange.size(); i++) {
					LivingEntity e = inRange.get(i);
					if (!e.isDead() && hitBox.contains(e.getLocation().add(0, 0.6, 0))) {
						if (spell != null) {
							if (spell instanceof TargetedEntitySpell) {
								ValidTargetChecker checker = spell.getValidTargetChecker();
								if (checker != null && !checker.isValidTarget(e)) {
									inRange.remove(i);
									break;
								}
								SpellTargetEvent event = new SpellTargetEvent(thisSpell, caster, e);
								Bukkit.getPluginManager().callEvent(event);
								if (event.isCancelled()) {
									inRange.remove(i);
									break;
								}
								((TargetedEntitySpell)spell).castAtEntity(caster, e, power);
								playSpellEffects(EffectPosition.TARGET, e);
							} else if (spell instanceof TargetedLocationSpell) {
								((TargetedLocationSpell)spell).castAtLocation(caster, currentLocation, power);
								playSpellEffects(EffectPosition.TARGET, currentLocation);
							}
						}
						if (stopOnHitEntity) {
							stop();
						} else {
							inRange.remove(i);
							immune.put(e, System.currentTimeMillis());
						}
						break;
					}
				}
				Iterator<Map.Entry<LivingEntity, Long>> iter = immune.entrySet().iterator();
				while (iter.hasNext()) {
					Map.Entry<LivingEntity, Long> entry = iter.next();
					if (entry.getValue().longValue() < System.currentTimeMillis() - 2000) {
						iter.remove();
						inRange.add(entry.getKey());
					}
				}
			}
		}
		
		public void stop() {
			playSpellEffects(EffectPosition.DELAYED, currentLocation);
			MagicSpells.cancelTask(taskId);
			caster = null;
			startLocation = null;
			previousLocation = null;
			currentLocation = null;
			currentVelocity = null;
			if (inRange != null) {
				inRange.clear();
				inRange = null;
			}
		}
		
	}
	
}

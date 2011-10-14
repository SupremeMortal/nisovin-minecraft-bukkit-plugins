package com.nisovin.magicspells.spells.buff;

import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.BuffSpell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;

public class FlamewalkSpell extends BuffSpell {
	
	private int range;
	private int fireTicks;
	private int tickInterval;
	private boolean targetPlayers;
	private boolean checkPlugins;
	
	private HashMap<String,Float> flamewalkers;
	private Burner burner;
	
	public FlamewalkSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		range = config.getInt("spells." + spellName + ".range", 8);
		fireTicks = config.getInt("spells." + spellName + ".fire-ticks", 80);
		tickInterval = config.getInt("spells." + spellName + ".tick-interval", 100);
		targetPlayers = config.getBoolean("spells." + spellName + ".target-players", false);
		checkPlugins = config.getBoolean("spells." + spellName + ".check-plugins", true);
		
		flamewalkers = new HashMap<String,Float>();
	}

	@Override
	protected PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (flamewalkers.containsKey(player.getName())) {
			turnOff(player);
			return PostCastAction.ALREADY_HANDLED;
		} else if (state == SpellCastState.NORMAL) {
			flamewalkers.put(player.getName(),power);
			if (burner == null) {
				burner = new Burner();
			}
			startSpellDuration(player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}	
	
	@Override
	protected void turnOff(Player player) {
		super.turnOff(player);
		sendMessage(player, strFade);
		flamewalkers.remove(player.getName());
		if (flamewalkers.size() == 0 && burner != null) {
			burner.stop();
			burner = null;
		}
	}
	
	@Override
	protected void turnOff() {
		flamewalkers.clear();
		if (burner != null) {
			burner.stop();
			burner = null;
		}
	}

	private class Burner implements Runnable {
		int taskId;
		String[] strArr = new String[0];
		
		public Burner() {
			taskId = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(MagicSpells.plugin, this, tickInterval, tickInterval);
		}
		
		public void stop() {
			Bukkit.getServer().getScheduler().cancelTask(taskId);
		}
		
		public void run() {
			for (String s : flamewalkers.keySet().toArray(strArr)) {
				Player player = Bukkit.getServer().getPlayer(s);
				float power = flamewalkers.get(s);
				if (player != null) {
					if (isExpired(player)) {
						turnOff(player);
						continue;
					}
					List<Entity> entities = player.getNearbyEntities(range, range, range);
					for (Entity entity : entities) {
						if (entity instanceof Player) {
							if (entity != player && targetPlayers) {
								if (checkPlugins) {
									EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(player, entity, DamageCause.ENTITY_ATTACK, 1);
									Bukkit.getServer().getPluginManager().callEvent(event);
									if (event.isCancelled()) {
										continue;
									}
								}
								entity.setFireTicks(Math.round(fireTicks*power));
								addUse(player);
								chargeUseCost(player);
							}
						} else if (entity instanceof LivingEntity) {
							entity.setFireTicks(Math.round(fireTicks*power));
							addUse(player);
							chargeUseCost(player);
						}
					}
				}
			}
		}
	}



}
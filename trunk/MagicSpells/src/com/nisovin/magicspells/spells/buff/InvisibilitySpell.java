package com.nisovin.magicspells.spells.buff;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class InvisibilitySpell extends BuffSpell {

	private boolean toggle;
	private boolean preventPickups;
	private boolean cancelOnSpellCast;
	
	private HashMap<String,CostCharger> invisibles = new HashMap<String, InvisibilitySpell.CostCharger>();
	
	public InvisibilitySpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		toggle = getConfigBoolean("toggle", true);
		preventPickups = getConfigBoolean("prevent-pickups", true);
		cancelOnSpellCast = getConfigBoolean("cancel-on-spell-cast", false);
	}
	
	@Override
	public void initialize() {
		super.initialize();
		if (cancelOnSpellCast) {
			registerEvents(new SpellCastListener());
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (toggle && invisibles.containsKey(player)) {
			turnOff(player);
			return PostCastAction.ALREADY_HANDLED;
		} else if (state == SpellCastState.NORMAL) {
			// make player invisible
			for (Player p : Bukkit.getOnlinePlayers()) {
				p.hidePlayer(player);
			}
			// detarget monsters
			Creature creature;
			for (Entity e : player.getNearbyEntities(30, 30, 30)) {
				if (e instanceof Creature) {
					creature = (Creature)e;
					if (creature.getTarget() != null && creature.getTarget().equals(player)) {
						creature.setTarget(null);
					}
				}
			}
			// start buff stuff
			startSpellDuration(player);
			invisibles.put(player.getName(), new CostCharger(player));
			// spell effect
			playSpellEffects(1, player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@EventHandler
	public void onPlayerItemPickup(PlayerPickupItemEvent event) {
		if (preventPickups && invisibles.containsKey(event.getPlayer())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onEntityTarget(EntityTargetEvent event) {
		if (!event.isCancelled() && event.getTarget() instanceof Player) {
			if (invisibles.containsKey((Player)event.getTarget())) {
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		for (String name : invisibles.keySet()) {
			Player p = Bukkit.getPlayerExact(name);
			if (p != null && !name.equals(player.getName())) {
				player.hidePlayer(p);
			}
		}
		if (invisibles.containsKey(player.getName())) {
			for (Player p : Bukkit.getOnlinePlayers()) {
				p.hidePlayer(player);
			}
		}
	}
	
	@Override
	public void turnOff(Player player) {
		if (invisibles.containsKey(player.getName())) {
			super.turnOff(player);
			// stop charge ticker
			CostCharger c = invisibles.remove(player.getName());
			c.stop();
			// force visible
			for (Player p : Bukkit.getOnlinePlayers()) {
				p.showPlayer(player);
			}
			// spell effect
			playSpellEffects(4, player);
			sendMessage(player, strFade);
		}
	}

	@Override
	protected void turnOff() {
		for (CostCharger c : invisibles.values()) {
			c.stop();
		}
		invisibles.clear();
	}
	
	public class SpellCastListener implements Listener {
		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onSpellCast(SpellCastEvent event) {
			if (isActive(event.getCaster())) {
				turnOff(event.getCaster());
			}
		}
	}
	
	private class CostCharger implements Runnable {
		int taskId = -1;
		Player player;
		
		public CostCharger(Player player) {
			this.player = player;
			if (useCostInterval > 0) {
				taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(MagicSpells.plugin, this, 20, 20);
			}
		}
		
		public void run() {
			addUseAndChargeCost(player);
		}
		
		public void stop() {
			if (taskId != -1) {
				Bukkit.getScheduler().cancelTask(taskId);
			}
		}
	}

	@Override
	public boolean isActive(Player player) {
		return invisibles.containsKey(player.getName());
	}

}

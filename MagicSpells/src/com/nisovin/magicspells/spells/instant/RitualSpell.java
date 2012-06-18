package com.nisovin.magicspells.spells.instant;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class RitualSpell extends InstantSpell {
	
	private int ritualDuration;
	private int reqParticipants;
	private boolean needSpellToParticipate;
	private boolean showProgressOnExpBar;
	private boolean chargeReagentsImmediately;
	private boolean setCooldownImmediately;
	private boolean setCooldownForAll;
	private Spell spell;
	private String theSpellName;
	private int tickInterval;
	private int effectInterval;
	private String strRitualJoined;
	private String strRitualSuccess;
	private String strRitualInterrupted;
	private String strRitualFailed;
	
	private HashMap<Player, ActiveRitual> activeRituals;
	
	public RitualSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		ritualDuration = getConfigInt("ritual-time", 200);
		reqParticipants = getConfigInt("req-participants", 3);
		needSpellToParticipate = getConfigBoolean("need-spell-to-participate", false);
		showProgressOnExpBar = getConfigBoolean("show-progress-on-exp-bar", true);
		chargeReagentsImmediately = getConfigBoolean("charge-reagents-immediately", true);
		setCooldownImmediately = getConfigBoolean("set-cooldown-immediately", true);
		setCooldownForAll = getConfigBoolean("set-cooldown-for-all", true);
		theSpellName = getConfigString("spell", "");
		tickInterval = getConfigInt("tick-interval", 5);
		effectInterval = getConfigInt("effect-interval", 20);
		strRitualJoined = getConfigString("str-ritual-joined", null);
		strRitualSuccess = getConfigString("str-ritual-success", null);
		strRitualInterrupted = getConfigString("str-ritual-interrupted", null);
		strRitualFailed = getConfigString("str-ritual-failed", null);
		
		activeRituals = new HashMap<Player, ActiveRitual>();
	}
	
	@Override
	public void initialize() {
		super.initialize();
		spell = MagicSpells.getSpellByInternalName(theSpellName);
		if (spell == null) {
			MagicSpells.error("RitualSpell '" + internalName + "' does not have a spell defined (" + theSpellName + ")!");
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (activeRituals.containsKey(player)) {
			ActiveRitual channel = activeRituals.remove(player);
			channel.stop(strRitualInterrupted);
		}
		if (state == SpellCastState.NORMAL) {
			activeRituals.put(player, new ActiveRitual(player, power, args));
			if (!chargeReagentsImmediately && !setCooldownImmediately) {
				return PostCastAction.MESSAGES_ONLY;
			} else if (!chargeReagentsImmediately) {
				return PostCastAction.NO_REAGENTS;
			} else if (!setCooldownImmediately) {
				return PostCastAction.NO_COOLDOWN;
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void onPlayerInteract(PlayerInteractEntityEvent event) {
		if (event.getRightClicked() instanceof Player) {
			ActiveRitual channel = activeRituals.get((Player)event.getRightClicked());
			if (channel != null) {
				if (!needSpellToParticipate || hasThisSpell(event.getPlayer())) {
					channel.addChanneler(event.getPlayer());
					sendMessage(event.getPlayer(), strRitualJoined);
				}
			}
		}
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		for (ActiveRitual ritual : activeRituals.values()) {
			if (ritual.isChanneler(event.getPlayer())) {
				ritual.stop(strInterrupted);
			}
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
	public void onPlayerDeath(PlayerDeathEvent event) {
		for (ActiveRitual ritual : activeRituals.values()) {
			if (ritual.isChanneler(event.getEntity())) {
				ritual.stop(strInterrupted);
			}
		}
	}
	
	private boolean hasThisSpell(Player player) {
		return MagicSpells.getSpellbook(player).hasSpell(this);
	}
	
	public class ActiveRitual implements Runnable {
		
		private Player caster;
		private float power;
		private String[] args;
		private int duration = 0;
		private int taskId;
		private HashMap<Player, Location> channelers = new HashMap<Player, Location>();
		
		public ActiveRitual(Player caster, float power, String[] args) {
			this.power = power;
			this.args = args;
			this.caster = caster;
			this.channelers.put(caster, caster.getLocation().clone());
			this.taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(MagicSpells.plugin, this, tickInterval, tickInterval);
			playSpellEffects(EffectPosition.CASTER, caster);
		}
		
		public void addChanneler(Player player) {
			if (!channelers.containsKey(player)) {
				channelers.put(player, player.getLocation().clone());
				playSpellEffects(EffectPosition.CASTER, player);
			}
		}
		
		public void removeChanneler(Player player) {
			channelers.remove(player);
		}
		
		public boolean isChanneler(Player player) {
			return channelers.containsKey(player);
		}
		
		@Override
		public void run() {
			duration += tickInterval;

			int count = channelers.size();
			boolean interrupted = false;
			for (Player player : channelers.keySet()) {
				// check for movement
				Location oldloc = channelers.get(player);
				Location newloc = player.getLocation();
				if (Math.abs(oldloc.getX() - newloc.getX()) > .01 || Math.abs(oldloc.getY() - newloc.getY()) > .01 || Math.abs(oldloc.getZ() - newloc.getZ()) > .01) {
					interrupted = true;
					break;
				}
				// send exp bar update
				if (showProgressOnExpBar) {
					MagicSpells.getVolatileCodeHandler().setExperienceBar(player, count, (float)duration / (float)ritualDuration);
				}
				// spell effect
				if (duration % effectInterval == 0) {
					playSpellEffects(EffectPosition.CASTER, player);
				}
			}
			if (interrupted) {
				stop(strRitualInterrupted);
			}
			
			if (duration >= ritualDuration) {
				// channel is done
				if (count >= reqParticipants && !caster.isDead() && caster.isOnline()) {
					if (chargeReagentsImmediately || hasReagents(caster)) {
						stop(strRitualSuccess);
						PostCastAction action = spell.castSpell(caster, SpellCastState.NORMAL, power, args);
						if (!chargeReagentsImmediately && action.chargeReagents()) {
							removeReagents(caster);
						}
						if (!setCooldownImmediately && action.setCooldown()) {
							setCooldown(caster, cooldown);
						}
						if (setCooldownForAll && action.setCooldown()) {
							for (Player p : channelers.keySet()) {
								setCooldown(p, cooldown);
							}
						}
					} else {
						stop(strRitualFailed);
					}
				} else {
					stop(strRitualFailed);
				}
			}
		}
		
		public void stop(String message) {
			for (Player player : channelers.keySet()) {
				sendMessage(player, message);
				MagicSpells.getVolatileCodeHandler().setExperienceBar(player, player.getLevel(), player.getExp());
			}
			channelers.clear();
			Bukkit.getScheduler().cancelTask(taskId);
			activeRituals.remove(caster);
		}
		
	}

}

package com.nisovin.magicspells;

import java.util.HashMap;
import java.util.HashSet;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.spells.BuffSpell;

public class BuffManager {
	
	private HashMap<String,HashSet<BuffSpell>> activeBuffs;
	private int taskId = -1;
	
	public BuffManager(int interval) {
		activeBuffs = new HashMap<String, HashSet<BuffSpell>>();
		taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(MagicSpells.plugin, new Monitor(), interval, interval);
	}
	
	public void addBuff(Player player, BuffSpell spell) {
		HashSet<BuffSpell> buffs = activeBuffs.get(player.getName());
		if (buffs == null) {
			buffs = new HashSet<BuffSpell>();
			activeBuffs.put(player.getName(), buffs);
		}
		buffs.add(spell);
	}
	
	public void removeBuff(Player player, BuffSpell spell) {
		HashSet<BuffSpell> buffs = activeBuffs.get(player.getName());
		if (buffs != null) {
			buffs.remove(spell);
			if (buffs.size() == 0) {
				activeBuffs.remove(player.getName());
			}
		}
	}
	
	public HashSet<BuffSpell> getActiveBuffs(Player player) {
		return activeBuffs.get(player.getName());
	}
	
	public void turnOff() {
		if (taskId > 0) {
			Bukkit.getScheduler().cancelTask(taskId);
			activeBuffs.clear();
			activeBuffs = null;
		}
	}
	
	class Monitor implements Runnable {
		@Override
		public void run() {
			NoMagicZoneHandler noMagicZones = MagicSpells.getNoMagicZoneHandler();
			for (String playerName : activeBuffs.keySet()) {
				Player p = Bukkit.getPlayerExact(playerName);
				if (p != null) {
					HashSet<BuffSpell> buffs = new HashSet<BuffSpell>(activeBuffs.get(playerName));
					for (BuffSpell spell : buffs) {
						if (noMagicZones.willFizzle(p, spell)) {
							spell.turnOff(p);
						}
					}
				}
			}
		}
	}
	
}

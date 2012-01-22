package com.nisovin.magicspells.spells.buff;

import java.lang.reflect.Field;
import java.util.HashMap;

import net.minecraft.server.EntityLiving;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.MobEffect;
import net.minecraft.server.Packet42RemoveMobEffect;

import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerToggleSprintEvent;

import com.nisovin.magicspells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class HasteSpell extends BuffSpell {

	private int strength;
	private int boostDuration;
	
	private HashMap<Player,Integer> hasted;
	
	public HasteSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		strength = getConfigInt("effect-strength", 3);
		boostDuration = getConfigInt("boost-duration", 300);
		
		hasted = new HashMap<Player,Integer>();
	}

	@Override
	protected PostCastAction castSpell(final Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			hasted.put(player, Math.round(strength*power));
			startSpellDuration(player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerToggleSprint(PlayerToggleSprintEvent event) {
		if (event.isCancelled()) return;
		Player player = event.getPlayer();
		if (hasted.containsKey(player)) {
			if (isExpired(player)) {
				turnOff(player);
			} else if (event.isSprinting()) {
				event.setCancelled(true);
				setMobEffect(event.getPlayer(), 1, boostDuration, hasted.get(player));
				addUseAndChargeCost(player);
			} else {
				removeMobEffect(event.getPlayer(), 1);
			}
		}
	}

	@Override
	protected void turnOff(Player player) {
		if (hasted.containsKey(player)) {
			super.turnOff(player);
			hasted.remove(player);
			removeMobEffect(player, 1);
			sendMessage(player, strFade);
		}
	}
	
	@Override
	protected void turnOff() {
		for (Player p : hasted.keySet()) {
			removeMobEffect(p, 1);
		}
		hasted.clear();
	}

}

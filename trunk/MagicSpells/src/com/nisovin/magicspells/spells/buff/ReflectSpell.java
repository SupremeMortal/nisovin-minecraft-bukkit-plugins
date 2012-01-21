package com.nisovin.magicspells.spells.buff;

import java.util.HashSet;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;

import com.nisovin.magicspells.BuffSpell;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.util.MagicConfig;

public class ReflectSpell extends BuffSpell {

	private HashSet<Player> reflectors;
	
	public ReflectSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		reflectors = new HashSet<Player>();
	}
	
	@Override
	protected PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (reflectors.contains(player)) {
			turnOff(player);
			return PostCastAction.ALREADY_HANDLED;
		} else if (state == SpellCastState.NORMAL) {
			reflectors.add(player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@EventHandler
	public void onSpellTarget(SpellTargetEvent event) {
		if (event.isCancelled()) return;
		if (event.getTarget() instanceof Player) {
			Player target = (Player)event.getTarget();
			if (reflectors.contains(target)) {
				boolean ok = chargeUseCost(target);
				if (ok) {
					event.setTarget(event.getCaster());
					addUse(target);
				}
			}
		}
	}

	@Override
	protected void turnOff(Player player) {
		if (reflectors.contains(player)) {
			super.turnOff(player);
			reflectors.remove(player);
			sendMessage(player, strFade);
		}
	}

	@Override
	protected void turnOff() {
		reflectors.clear();
	}

}

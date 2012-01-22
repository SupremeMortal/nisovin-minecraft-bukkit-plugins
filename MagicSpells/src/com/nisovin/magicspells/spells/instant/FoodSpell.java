package com.nisovin.magicspells.spells.instant;

import org.bukkit.entity.Player;

import com.nisovin.magicspells.InstantSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class FoodSpell extends InstantSpell {

	private int food;
	private float saturation;
	
	public FoodSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		food = getConfigInt("food", 4);
		saturation = getConfigFloat("saturation", 2.5F);
	}

	@Override
	protected PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			player.setFoodLevel(player.getFoodLevel() + food);
			player.setSaturation(player.getSaturation() + saturation);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

}

package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.entity.Player;

import com.nisovin.magicspells.castmodifiers.Condition;

public class InWorldCondition extends Condition {

	String world = "";

	@Override
	public boolean setVar(String var) {
		world = var;
		return true;
	}
	
	@Override
	public boolean check(Player player) {
		return player.getWorld().getName().equalsIgnoreCase(world);
	}

}

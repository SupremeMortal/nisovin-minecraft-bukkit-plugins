package com.nisovin.magicspells.castmodifiers.conditions;

import org.bukkit.entity.Player;

import com.nisovin.magicspells.castmodifiers.Condition;

public class BlockingCondition extends Condition {
	
	@Override
	public boolean setVar(String var) {
		return true;
	}
	
	@Override
	public boolean check(Player player) {
		return player.isBlocking();
	}
	
}

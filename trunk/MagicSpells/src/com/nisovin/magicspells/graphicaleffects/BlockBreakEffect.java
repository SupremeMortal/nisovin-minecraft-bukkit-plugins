package com.nisovin.magicspells.graphicaleffects;

import org.bukkit.Effect;
import org.bukkit.Location;

public class BlockBreakEffect extends GraphicalEffect {
	
	@Override
	public void showEffect(Location location, String param) {
		int id = 0;
		if (param != null && !param.isEmpty()) {
			try {
				id = Integer.parseInt(param);
			} catch (NumberFormatException e) {			
			}
		}
		location.getWorld().playEffect(location, Effect.STEP_SOUND, id);
	}
}

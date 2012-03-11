package com.nisovin.magicspells.graphicaleffects;

import java.util.Random;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

import com.nisovin.magicspells.util.BlockUtils;

public class CloudEffect extends GraphicalEffect {

	Random rand = new Random();
	
	@Override
	public void playEffect(Location location, String param) {
		int radius = 3;
		try {
			radius = Integer.parseInt(param);
		} catch (NumberFormatException e) {
			radius = 3;
		}
		
		World w = location.getWorld();
		int cx = location.getBlockX();
		int cy = location.getBlockY();
		int cz = location.getBlockZ();
		
		Block b;
		for (int x = cx - radius; x <= cx + radius; x++) {
			for (int z = cz - radius; z <= cz + radius; z++) {
				if (inRange(x, z, cx, cz, radius)) {
					b = w.getBlockAt(x, cy, z);
					if (BlockUtils.isPathable(b)) {
						smoke(w, b, radius);
					} else {
						b = b.getRelative(0, -1, 0);
						if (BlockUtils.isPathable(b)) {
							smoke(w, b, radius);
						} else {
							b = b.getRelative(0, 2, 0);
							if (BlockUtils.isPathable(b)) {
								smoke(w, b, radius);
							}
						}
					}
				}
			}
		}
	}
	
	private void smoke(World w, Block b, int r) {
		Location loc = b.getLocation();
		if (r <= 5) {
			for (int i = 0; i <= 8; i+=2) {
				w.playEffect(loc, Effect.SMOKE, i);
			}
		} else if (r <= 8) {
			w.playEffect(loc, Effect.SMOKE, rand.nextInt(9));
			w.playEffect(loc, Effect.SMOKE, rand.nextInt(9));
		} else {
			w.playEffect(loc, Effect.SMOKE, rand.nextInt(9));
		}
	}
	
	private boolean inRange(int x1, int z1, int x2, int z2, int r) {
		return sq(x1-x2) + sq(z1-z2) < sq(r);
	}
	
	private int sq(int v) {
		return v*v;
	}

}

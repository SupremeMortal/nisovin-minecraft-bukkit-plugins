package com.nisovin.MagicSpells.Util;

import java.util.ArrayList;
import java.util.HashSet;

import org.bukkit.Material;
import org.bukkit.block.Block;

import com.nisovin.MagicSpells.MagicSpells;

public class TemporaryBlockSet implements Runnable {
	
	private static HashSet<TemporaryBlockSet> blockSets = new HashSet<TemporaryBlockSet>();
	
	private Material original;
	private Material replaceWith;
	
	private ArrayList<Block> blocks;
	
	public TemporaryBlockSet(Material original, Material replaceWith) {
		this.original = original;
		this.replaceWith = replaceWith;
		
		this.blocks = new ArrayList<Block>();
		
		blockSets.add(this);
	}
	
	public void add(Block block) {
		if (block.getType() == original) {
			block.setType(replaceWith);
			blocks.add(block);
		}
	}
	
	public void removeAfter(int seconds) {
		if (blocks.size() > 0) {
			MagicSpells.plugin.getServer().getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, this, seconds*20);
		}
	}
	
	public void run() {
		for (Block block : blocks) {
			if (block.getType() == replaceWith) {
				block.setType(original);
			}
		}
		blockSets.remove(this);
	}
	
	public static boolean isTemporary(Block block) {
		if (blockSets == null || blockSets.size() == 0) {
			return false;
		} else {
			for (TemporaryBlockSet set : blockSets) {
				for (Block b : set.blocks) {	
					if (b.equals(block)) {
						return true;
					}
				}
			}
			return false;
		}
	}

}
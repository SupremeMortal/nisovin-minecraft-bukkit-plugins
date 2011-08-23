package com.nisovin.MagicSpells.Spells;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.bukkit.util.config.Configuration;

import com.nisovin.MagicSpells.InstantSpell;
import com.nisovin.MagicSpells.Util.TemporaryBlockSet;

public class WallSpell extends InstantSpell {

	private int wallWidth;
	private int wallHeight;
	private Material wallType;
	private int wallDuration;
	private String strNoTarget;
	
	public WallSpell(Configuration config, String spellName) {
		super(config, spellName);
		
		wallWidth = config.getInt("spells." + spellName + ".wall-width", 5);
		wallHeight = config.getInt("spells." + spellName + ".wall-height", 3);
		wallType = Material.getMaterial(config.getInt("spells." + spellName + ".wall-type", Material.BRICK.getId()));
		wallDuration = config.getInt("spells." + spellName + ".wall-duration", 15);
		strNoTarget = config.getString("spells." + spellName + ".str-no-target", "Unable to create a wall.");
	}
	
	@Override
	protected PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Block target = player.getTargetBlock(null, range>0&&range<15?range:3);
			if (target == null || target.getType() != Material.AIR) {
				// fail
				sendMessage(player, strNoTarget);
				return PostCastAction.ALREADY_HANDLED;
			} else {
				TemporaryBlockSet blockSet = new TemporaryBlockSet(Material.AIR, wallType);
				Location loc = target.getLocation();
				Vector dir = player.getLocation().getDirection();
				int wallWidth = Math.round(this.wallWidth*power);
				int wallHeight = Math.round(this.wallHeight*power);
				if (Math.abs(dir.getX()) > Math.abs(dir.getZ())) {
					for (int z = loc.getBlockZ() - (wallWidth/2); z <= loc.getBlockZ() + (wallWidth/2); z++) {
						for (int y = loc.getBlockY() - 1; y < loc.getBlockY() + wallHeight - 1; y++) {
							blockSet.add(player.getWorld().getBlockAt(target.getX(), y, z));
						}
					}
				} else {
					for (int x = loc.getBlockX() - (wallWidth/2); x <= loc.getBlockX() + (wallWidth/2); x++) {
						for (int y = loc.getBlockY() - 1; y < loc.getBlockY() + wallHeight - 1; y++) {
							blockSet.add(player.getWorld().getBlockAt(x, y, target.getZ()));
						}
					}
				}
				blockSet.removeAfter(Math.round(wallDuration*power));
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
}
package com.nisovin.magicspells.spells.instant;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.InstantSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class ZapSpell extends InstantSpell {
	
	private String strCantZap;
	private HashSet<Byte> transparentBlockTypes;
	private List<Integer> allowedBlockTypes;
	private List<Integer> disallowedBlockTypes;
	private boolean dropBlock;
	private boolean dropNormal;
	private boolean checkPlugins;
	
	public ZapSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		strCantZap = getConfigString("str-cant-zap", "");
		String[] transparent = getConfigString("transparent-block-types","0,8,9").split(",");
		String[] allowed = getConfigString("allowed-block-types","").split(",");
		String[] disallowed = getConfigString("disallowed-block-types","0,7,10,11").split(",");
		transparentBlockTypes = new HashSet<Byte>();
		for (String s : transparent) {
			transparentBlockTypes.add(Byte.parseByte(s));
		}
		allowedBlockTypes = new ArrayList<Integer>();
		for (String s : allowed) {
			allowedBlockTypes.add(Integer.parseInt(s));
		}
		disallowedBlockTypes = new ArrayList<Integer>();
		for (String s : disallowed) {
			disallowedBlockTypes.add(Integer.parseInt(s));
		}
		dropBlock = config.getBoolean("spells." + spellName + ".drop-block", false);
		dropNormal = getConfigBoolean("drop-normal", true);
		checkPlugins = getConfigBoolean("check-plugins", true);
	}

	@Override
	protected PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			// get targeted block
			Block target = player.getTargetBlock(transparentBlockTypes, range>0?range:100);
			if (target != null) {
				// check for disallowed block
				if (disallowedBlockTypes.contains(target.getTypeId()) || (allowedBlockTypes.size() > 0 && !allowedBlockTypes.contains(target.getTypeId()))) {
					sendMessage(player, strCantZap);
					return PostCastAction.ALREADY_HANDLED;
				}
				
				// check for protection
				if (checkPlugins) {
					BlockBreakEvent event = new BlockBreakEvent(target, player);
					MagicSpells.plugin.getServer().getPluginManager().callEvent(event);
					if (event.isCancelled()) {
						// a plugin cancelled the event
						sendMessage(player, strCantZap);
						return PostCastAction.ALREADY_HANDLED;
					}
				}
				
				// drop block
				if (dropBlock) {
					if (dropNormal) {
						net.minecraft.server.Block.byId[target.getTypeId()].dropNaturally(((CraftWorld)target.getWorld()).getHandle(), target.getX(), target.getY(), target.getZ(), target.getData(), 1.0F);
					} else {
						target.getWorld().dropItemNaturally(target.getLocation(), new ItemStack(target.getType(), 1, target.getData()));
					}
				}
				
				// show animation
				player.getWorld().playEffect(target.getLocation(), Effect.STEP_SOUND, target.getTypeId());
				
				// remove block
				target.setType(Material.AIR);
				
			} else {
				sendMessage(player, strCantZap);
				return PostCastAction.ALREADY_HANDLED;
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
}

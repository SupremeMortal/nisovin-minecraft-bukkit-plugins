package com.nisovin.magicspells.spells.instant;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import com.nisovin.magicspells.InstantSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class BuildSpell extends InstantSpell {
	
	private int slot;
	private boolean consumeBlock;
	private boolean showEffect;
	private int[] allowedTypes;
	private boolean checkPlugins;
	private String strInvalidBlock;
	private String strCantBuild;
	
	public BuildSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		slot = config.getInt("spells." + spellName + ".slot", 0);
		consumeBlock = config.getBoolean("spells." + spellName + ".consume-block", true);
		showEffect = config.getBoolean("spells." + spellName + ".show-effect", true);
		String[] allowed = config.getString("spells." + spellName + ".allowed-types", "1,2,3,4,5,12,13,17,20,22,24,35,41,42,43,44,45,47,48,49,50,53,57,65,67,80,85,87,88,89,91,92").split(",");
		allowedTypes = new int[allowed.length];
		for (int i = 0; i < allowed.length; i++) {
			allowedTypes[i] = Integer.parseInt(allowed[i]);
		}
		checkPlugins = config.getBoolean("spells." + spellName + ".check-plugins", true);
		strInvalidBlock = config.getString("spells." + spellName + "str-invalid-block", "You can't build that block.");
		strCantBuild = config.getString("spells." + spellName + "str-cant-build", "You can't build there.");
	}
	
	protected PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			// get mat
			ItemStack item = player.getInventory().getItem(slot);
			if (item == null || !isAllowed(item.getType())) {
				// fail
				sendMessage(player, strInvalidBlock);
				return PostCastAction.ALREADY_HANDLED;
			}
			
			// get target
			List<Block> lastBlocks = player.getLastTwoTargetBlocks(null, range);
			if (lastBlocks.size() < 2 || lastBlocks.get(1).getType() == Material.AIR) {
				// fail
				sendMessage(player, strCantBuild);
				return PostCastAction.ALREADY_HANDLED;
			} else {
				// check plugins
				Block b = lastBlocks.get(0);
				b.setTypeIdAndData(item.getTypeId(), (byte)item.getDurability(), true);
				if (checkPlugins) {
					BlockPlaceEvent event = new BlockPlaceEvent(b, b.getState(), lastBlocks.get(1), player.getItemInHand(), player, true);
					Bukkit.getServer().getPluginManager().callEvent(event);
					if (event.isCancelled() && b.getType() == item.getType()) {
						b.setType(Material.AIR);
						sendMessage(player, strCantBuild);
						return PostCastAction.ALREADY_HANDLED;
					}
				}
				if (showEffect) {
					b.getWorld().playEffect(b.getLocation(), Effect.STEP_SOUND, item.getTypeId());
				}
				if (consumeBlock) {
					int amt = item.getAmount()-1;
					if (amt > 0) {
						item.setAmount(amt);
						player.getInventory().setItem(slot, item);
					} else {
						player.getInventory().setItem(slot, null);
					}
				}
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private boolean isAllowed(Material mat) {
		for (int i = 0; i < allowedTypes.length; i++) {
			if (allowedTypes[i] == mat.getId()) {
				return true;
			}
		}
		return false;
	}
}
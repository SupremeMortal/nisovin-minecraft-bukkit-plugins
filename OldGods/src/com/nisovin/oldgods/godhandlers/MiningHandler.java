package com.nisovin.oldgods.godhandlers;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;

import com.nisovin.oldgods.God;
import com.nisovin.oldgods.OldGods;

public class MiningHandler {
	
	public static void onEntityDamage(EntityDamageEvent event) {
		if (event.getCause() == DamageCause.LAVA) {
			event.setDamage(1);
		}
	}
	
	public static void onBlockBreak(BlockBreakEvent event) {
		Material inHand = event.getPlayer().getItemInHand().getType();
		if (inHand == Material.IRON_PICKAXE || inHand == Material.GOLD_PICKAXE || inHand == Material.DIAMOND_PICKAXE) {
			Block b = event.getBlock();
			boolean devoutBlessing = (event.getPlayer().hasPermission("oldgods.disciple.mining") && OldGods.random() < 5); 
			if (b.getType() == Material.DIAMOND_ORE) {
				event.getBlock().getWorld().dropItemNaturally(b.getLocation(), new ItemStack(Material.DIAMOND, devoutBlessing ? 5 : 1));
			} else if (b.getType() == Material.IRON_ORE) {
				event.getBlock().getWorld().dropItemNaturally(b.getLocation(), new ItemStack(Material.IRON_INGOT, devoutBlessing ? 8 : 1));
			} else if (b.getType() == Material.GOLD_ORE) {
				event.getBlock().getWorld().dropItemNaturally(b.getLocation(), new ItemStack(Material.GOLD_INGOT, devoutBlessing? 6 : 1));
			} else if (b.getType() == Material.LAPIS_ORE) {
				event.getBlock().getWorld().dropItemNaturally(b.getLocation(), new ItemStack(Material.INK_SACK, devoutBlessing? 8 : 2, (short)4));
			} else {
				devoutBlessing = false;
			}
			if (devoutBlessing) {
				event.getPlayer().sendMessage(OldGods.getDevoutMessage(God.MINING));
			}
		}
	}
	
	public static void pray(Player player, Location location, int amount) {
		int chance = player.hasPermission("oldgods.disciple.mining") ? 20 : 2;
		if (OldGods.random() > chance) return;
		
		int quantity = 0;
		Material type = null;
		int r = OldGods.random(4);
		if (r==0) {
			type = Material.IRON_INGOT;
			quantity = 4;
		} else if (r==1) {
			type = Material.GOLD_INGOT;
			quantity = 2;
		} else if (r==2) {
			type = Material.DIAMOND;
			quantity = 1;
		} else if (r==3) {
			type = Material.DIAMOND_PICKAXE;
			quantity = 1;
		}
		
		if (quantity > 0 && type != null) {
			for (int i = 0; i < quantity; i++) {
				location.getWorld().dropItemNaturally(location, new ItemStack(type,1));
			}
		}		
	}
}

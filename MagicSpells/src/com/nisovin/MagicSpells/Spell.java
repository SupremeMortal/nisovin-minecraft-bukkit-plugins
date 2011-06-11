package com.nisovin.MagicSpells;

import java.util.HashMap;
import java.util.List;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.config.Configuration;

public abstract class Spell implements Comparable<Spell> {

	protected String internalName;
	protected String name;
	protected String description;
	protected int castItem;
	protected ItemStack[] cost;
	protected int healthCost = 0;
	protected int cooldown;
	protected int broadcastRange;
	protected String strCost;
	protected String strCastSelf;
	protected String strCastOthers;
	
	private HashMap<String, Long> lastCast;
	
	public Spell(Configuration config, String spellName) {
		this.internalName = spellName;
		this.name = config.getString("spells." + spellName + ".name", spellName);
		this.description = config.getString("spells." + spellName + ".description", "");
		this.castItem = config.getInt("spells." + spellName + ".cast-item", 280);
		List<String> costList = config.getStringList("spells." + spellName + ".cost", null);
		if (costList != null && costList.size() > 0) {
			cost = new ItemStack [costList.size()];
			String[] data, subdata;
			for (int i = 0; i < costList.size(); i++) {
				if (costList.get(i).contains(" ")) {
					data = costList.get(i).split(" ");
					if (data[0].equalsIgnoreCase("health")) {
						healthCost = Integer.parseInt(data[1]);
					} else if (data[0].contains(":")) {
						subdata = data[0].split(":");
						cost[i] = new ItemStack(Integer.parseInt(subdata[0]), Integer.parseInt(data[1]), Short.parseShort(subdata[1]));
					} else {
						cost[i] = new ItemStack(Integer.parseInt(data[0]), Integer.parseInt(data[1]));
					}
				} else {
					cost[i] = new ItemStack(Integer.parseInt(costList.get(i)));
				}
			}
		} else {
			cost = null;
		}
		this.cooldown = config.getInt("spells." + spellName + ".cooldown", 0);
		this.broadcastRange = config.getInt("spells." + spellName + ".broadcast-range", MagicSpells.broadcastRange);
		this.strCost = config.getString("spells." + spellName + ".str-cost", null);
		this.strCastSelf = config.getString("spells." + spellName + ".str-cast-self", null);
		this.strCastOthers = config.getString("spells." + spellName + ".str-cast-others", null);
		
		if (cooldown > 0) {
			lastCast = new HashMap<String, Long>();
		}
	}

	public final void cast(Player player) {
		cast(player, null);
	}
	
	public final void cast(Player player, String[] args) {
		SpellCastState state;
		if (!MagicSpells.getSpellbook(player).canCast(this)) {
			state = SpellCastState.CANT_CAST;
		} else if (onCooldown(player)) {
			state = SpellCastState.ON_COOLDOWN;
		} else if (!hasReagents(player)) {
			state = SpellCastState.MISSING_REAGENTS;
		} else {
			state = SpellCastState.NORMAL;
		}
		
		boolean handled = castSpell(player, state, args);
		if (!handled) {
			if (state == SpellCastState.NORMAL) {
				setCooldown(player);
				removeReagents(player);
				sendMessage(player, strCastSelf);
				sendMessageNear(player, formatMessage(strCastOthers, "%a", player.getName()));
			} else if (state == SpellCastState.ON_COOLDOWN) {
				sendMessage(player, formatMessage(MagicSpells.strOnCooldown, "%c", getCooldown(player)+""));
			} else if (state == SpellCastState.MISSING_REAGENTS) {
				sendMessage(player, MagicSpells.strMissingReagents);
			} else if (state == SpellCastState.CANT_CAST) {
				sendMessage(player, MagicSpells.strCantCast);
			}
		}
	}
	
	public abstract boolean canCastWithItem();
	
	public abstract boolean canCastByCommand();
	
	public String getCostStr() {
		if (strCost == null || strCost.equals("")) {
			return null;
		} else {
			return strCost;
		}
	}
	
	protected boolean onCooldown(Player player) {
		if (cooldown == 0 || (MagicSpells.freecastNoCooldown && MagicSpells.castForFree != null && MagicSpells.castForFree.contains(player.getName().toLowerCase()))) {
			return false;
		}
		
		Long casted = lastCast.get(player.getName());
		if (casted != null) {
			if (casted + (cooldown*1000) > System.currentTimeMillis()) {
				return true;
			}
		}
		return false;
	}
	
	protected int getCooldown(Player player) {
		if (cooldown <= 0) {
			return 0;
		}
		
		Long casted = lastCast.get(player.getName());
		if (casted != null) {
			return (int)(cooldown - ((System.currentTimeMillis()-casted)/1000));
		} else {
			return 0;
		}
	}
	
	protected void setCooldown(Player player) {
		if (cooldown > 0) {
			lastCast.put(player.getName(), System.currentTimeMillis());
		}
	}
	
	protected boolean hasReagents(Player player) {
		return hasReagents(player, cost, healthCost);
	}
	
	protected boolean hasReagents(Player player, ItemStack[] cost) {
		return hasReagents(player, cost, 0);
	}
	
	protected boolean hasReagents(Player player, ItemStack[] reagents, int healthCost) {
		if (MagicSpells.castForFree != null && MagicSpells.castForFree.contains(player.getName().toLowerCase())) {
			return true;
		}
		if (reagents == null && healthCost <= 0) {
			return true;
		}
		if (player.getHealth() <= healthCost) { // TODO: add option to allow death from health cost
			return false;
		}
		for (ItemStack item : reagents) {
			if (!inventoryContains(player.getInventory(), item)) {
				return false;
			}
		}
		return true;		
	}
	
	protected void removeReagents(Player player) {
		removeReagents(player, cost, healthCost);
	}
	
	protected void removeReagents(Player player, ItemStack[] reagents) {
		removeReagents(player, reagents, 0);
	}
	
	protected void removeReagents(Player player, ItemStack[] reagents, int healthCost) {
		if (MagicSpells.castForFree != null && MagicSpells.castForFree.contains(player.getName().toLowerCase())) {
			return;
		}
		if (reagents != null) {
			for (ItemStack item : reagents) {
				removeFromInventory(player.getInventory(), item);
			}
		}
		if (healthCost > 0) {
			player.setHealth(player.getHealth() - healthCost);
		}
	}
	
	private boolean inventoryContains(Inventory inventory, ItemStack item) {
		int count = 0;
		ItemStack[] items = inventory.getContents();
		for (int i = 0; i < items.length; i++) {
			if (items[i] != null && items[i].getType() == item.getType() && items[i].getDurability() == item.getDurability()) {
				count += items[i].getAmount();
			}
			if (count >= item.getAmount()) {
				return true;
			}
		}
		return false;
	}
	
	public void removeFromInventory(Inventory inventory, ItemStack item) {
		int amt = item.getAmount();
		ItemStack[] items = inventory.getContents();
		for (int i = 0; i < items.length; i++) {
			if (items[i] != null && items[i].getType() == item.getType() && items[i].getDurability() == item.getDurability()) {
				if (items[i].getAmount() > amt) {
					items[i].setAmount(items[i].getAmount() - amt);
					break;
				} else if (items[i].getAmount() == amt) {
					items[i] = null;
					break;
				} else {
					amt -= items[i].getAmount();
					items[i] = null;
				}
			}
		}
		inventory.setContents(items);
	}
	
	protected String formatMessage(String message, String... replacements) {
		if (message == null) return null;
		
		String msg = message;
		for (int i = 0; i < replacements.length; i+=2) {
			msg = msg.replace(replacements[i], replacements[i+1]);
		}
		return msg;
	}
	
	static protected void sendMessage(Player player, String message) {
		if (message != null && !message.equals("")) {
			String [] msgs = message.replaceAll("&([0-9a-f])", "\u00A7$1").split("\n");
			for (String msg : msgs) {
				if (!msg.equals("")) {
					player.sendMessage(MagicSpells.textColor + msg);
				}
			}
		}
	}
	
	protected void sendMessageNear(Player player, String message) {
		sendMessageNear(player, message, broadcastRange);
	}
	
	protected void sendMessageNear(Player player, String message, int range) {
		if (message != null && !message.equals("")) {
			String [] msgs = message.replaceAll("&([0-9a-f])", "\u00A7$1").split("\n");
			List<Entity> entities = player.getNearbyEntities(range*2, range*2, range*2);
			for (Entity entity : entities) {
				if (entity instanceof Player && entity != player) {
					for (String msg : msgs) {
						if (!msg.equals("")) {
							((Player)entity).sendMessage(MagicSpells.textColor + msg);
						}
					}
				}
			}
		}
	}
	
	protected abstract boolean castSpell(Player player, SpellCastState state, String[] args);

	public String getInternalName() {
		return this.internalName;
	}
	
	public String getName() {
		return this.name;
	}
	
	public int getCastItem() {
		return this.castItem;
	}
	
	public String getDescription() {
		return this.description;
	}
	
	protected void addListener(Event.Type eventType) {
		MagicSpells.addSpellListener(eventType, this);
	}
	
	protected void removeListener(Event.Type eventType) {
		MagicSpells.removeSpellListener(eventType, this);
	}
	
	public void onPlayerJoin(PlayerJoinEvent event) {}
	public void onPlayerQuit(PlayerQuitEvent event) {}
	public void onPlayerInteract(PlayerInteractEvent event) {}
	public void onPlayerMove(PlayerMoveEvent event) {}
	public void onEntityDamage(EntityDamageEvent event) {}	
	public void onEntityTarget(EntityTargetEvent event) {}	
	public void onExplosionPrime(ExplosionPrimeEvent event) {}	
	public void onBlockBreak(BlockBreakEvent event) {}
	public void onBlockPlace(BlockPlaceEvent event) {}
	
	protected enum SpellCastState {
		NORMAL,
		ON_COOLDOWN,
		MISSING_REAGENTS,
		CANT_CAST
	}
	
	@Override
	public int compareTo(Spell spell) {
		return this.name.compareTo(spell.name);
	}

}

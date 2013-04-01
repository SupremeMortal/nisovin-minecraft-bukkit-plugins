package com.nisovin.magicspells.spells.buff;

import java.util.HashMap;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spells.BuffSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class SeeHealthSpell extends BuffSpell {

	private String mode;
	private int interval;
	private int range;
	private boolean targetPlayers;
	private boolean targetNonPlayers;
	private boolean obeyLos;
	
	private String symbol = "=";
	private int barSize = 20;
	private boolean colorBlind = false;
	
	private HashMap<Player, Integer> bars;
	private Updater updater;
	
	public SeeHealthSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		mode = getConfigString("mode", "always");
		interval = getConfigInt("update-interval", 5);
		range = getConfigInt("range", 15);
		targetPlayers = getConfigBoolean("target-players", true);
		targetNonPlayers = getConfigBoolean("target-non-players", true);
		obeyLos = getConfigBoolean("obey-los", true);
		
		if (!mode.equals("attack") && !mode.equals("always")) {
			mode = "attack";
		}
		
		bars = new HashMap<Player, Integer>();
	}
	
	@Override
	public void initialize() {
		super.initialize();
		
		if (mode.equals("attack")) {
			registerEvents(new AttackListener());
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (bars.containsKey(player) && toggle) {
			turnOff(player);
			return PostCastAction.ALREADY_HANDLED;
		}
		if (state == SpellCastState.NORMAL) {
			if (!bars.containsKey(player)) {
				bars.put(player, player.getInventory().getHeldItemSlot());
				if (updater == null && mode.equals("always")) {
					updater = new Updater();
				}
			}
			startSpellDuration(player);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean isActive(Player player) {
		return bars.containsKey(player);
	}
	
	private void showHealthBar(Player player, LivingEntity entity) {
		int slot = player.getInventory().getHeldItemSlot();
		// get item
		ItemStack item = player.getItemInHand();
		if (item == null || item.getType() == Material.AIR) {
			item = new ItemStack(36, 0);
		} else {
			item = item.clone();
		}
		// get pct health
		double pct = (double)entity.getHealth() / (double)entity.getMaxHealth();
		// get bar color
		ChatColor color = ChatColor.WHITE;
		if (pct <= .2) {
			color = ChatColor.DARK_RED;
		} else if (pct <= .4) {
			color = ChatColor.RED;
		} else if (pct <= .6) {
			color = ChatColor.GOLD;
		} else if (pct <= .8) {
			color = ChatColor.YELLOW;
		} else if (!colorBlind) {
			color = ChatColor.GREEN;
		}
		// get health bar string
		StringBuilder sb = new StringBuilder(barSize + 9);
		sb.append(getRandomColor().toString());
		int remain = (int)Math.round(barSize * pct);
		sb.append(color.toString());
		for (int i = 0; i < remain; i++) {
			sb.append(symbol);
		}
		if (remain < barSize) {
			sb.append(ChatColor.DARK_GRAY.toString());
			for (int i = 0; i < barSize - remain; i++) {
				sb.append(symbol);
			}
		}
		// set health bar string
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(sb.toString());
		item.setItemMeta(meta);
		// send update
		MagicSpells.getVolatileCodeHandler().sendFakeSlotUpdate(player, slot, item);
	}
	
	//private void resetHealthBar(Player player) {
	//	resetHealthBar(player, player.getInventory().getHeldItemSlot());
	//}
	
	private void resetHealthBar(Player player, int slot) {
		MagicSpells.getVolatileCodeHandler().sendFakeSlotUpdate(player, slot, player.getItemInHand());
	}
	
	@EventHandler
	public void onItemHeldChange(PlayerItemHeldEvent event) {
		if (isActive(event.getPlayer())) {
			resetHealthBar(event.getPlayer(), event.getPreviousSlot());
		}
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void turnOff(Player player) {
		Integer i = bars.remove(player);
		if (i != null) {
			super.turnOff(player);
			player.updateInventory();
			sendMessage(player, strFade);
			
			if (updater != null && bars.size() == 0) {
				updater.stop();
				updater = null;
			}
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void turnOff() {
		for (Player player : bars.keySet()) {
			if (player.isValid()) {
				player.updateInventory();
			}
		}
		bars.clear();
		if (updater != null) {
			updater.stop();
			updater = null;
		}
	}
	
	private final String colors = "01234567890abcdef";
	private final Random random = new Random();
	private ChatColor getRandomColor() {
		return ChatColor.getByChar(colors.charAt(random.nextInt(colors.length())));
	}
	
	class AttackListener implements Listener {
		@EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
		public void onAttack(EntityDamageByEntityEvent event) {
			if (event.getEntity() instanceof LivingEntity) {
				Entity damager = event.getDamager();
				if (damager instanceof Projectile && ((Projectile)damager).getShooter() != null) {
					damager = ((Projectile)damager).getShooter();
				}
				// update bar?
			}
		}
	}
	
	class Updater implements Runnable {
		
		private int taskId;
		
		public Updater() {
			taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(MagicSpells.plugin, this, 0, interval);
		}
		
		@Override
		public void run() {
			for (Player player : bars.keySet()) {
				LivingEntity target = getTargetedEntity(player, 2, range, targetPlayers, targetNonPlayers, obeyLos, false);
				if (target != null) {
					showHealthBar(player, target);
				} else {
					//resetHealthBar(player);
				}
			}
		}
		
		public void stop() {
			Bukkit.getScheduler().cancelTask(taskId);
		}
		
	}


}

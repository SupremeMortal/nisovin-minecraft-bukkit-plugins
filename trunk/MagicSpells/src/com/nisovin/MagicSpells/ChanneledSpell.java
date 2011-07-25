package com.nisovin.MagicSpells;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.config.Configuration;

public abstract class ChanneledSpell extends Spell {

	private int channelTime;
	private int reqParticipants;
	private int reqPercent;
	private int maxDistance;
	private int castDelay;
	private boolean castWithItem;
	private boolean castByCommand;
	private String strTooFarAway;
	private String strStartChannel;
	private String strMoved;
	private String strSpellSuccess;
	
	private HashMap<String,HashMap<Player,Long>> channelers;
	private HashMap<String,Location> locations;
	private static HashSet<Player> allChannelers;
	
	public ChanneledSpell(Configuration config, String spellName) {
		super(config, spellName);
		
		addListener(Event.Type.PLAYER_MOVE);
		addListener(Event.Type.PLAYER_QUIT);
		
		channelTime = getConfigInt(config, "channel-time", 30);
		reqParticipants = getConfigInt(config, "req-participants", 1);
		reqPercent = getConfigInt(config, "req-percent", 0);
		maxDistance = getConfigInt(config, "max-distance", 0);
		castDelay = getConfigInt(config, "cast-delay", 0);
		castWithItem = getConfigBoolean(config, "can-cast-with-item", true);
		castByCommand = getConfigBoolean(config, "can-cast-by-command", true);
		strTooFarAway = getConfigString(config, "str-too-far-away", "You are too far away.");
		strStartChannel = getConfigString(config, "str-start-channel", "");
		strMoved = getConfigString(config, "str-moved", "You have stopped channeling.");
		strSpellSuccess = getConfigString(config, "str-spell-success", "");
		
		channelers = new HashMap<String,HashMap<Player,Long>>();
		if (maxDistance > 0) {
			locations = new HashMap<String,Location>();
		}
		
		if (allChannelers == null) {
			allChannelers = new HashSet<Player>();
		}
	}
	
	protected boolean addChanneler(final String key, Player player) {
		HashMap<Player,Long> c = channelers.get(key);
		
		// create the map if it doesn't exist
		if (c == null) {
			c = new HashMap<Player,Long>();
			channelers.put(key, c);
		}
		
		// remove expired channelers
		if (channelTime > 0 && c.size() > 0) {
			Iterator<Player> i = c.keySet().iterator();
			while (i.hasNext()) {
				Player p = i.next();
				if (c.get(p) + channelTime*1000 < System.currentTimeMillis()) {
					i.remove();
				}
			}
		}
		
		// check if over max distance
		if (maxDistance > 0) {
			if (c.size() == 0) {
				// first channeler, set initial location
				locations.put(key, player.getLocation());
			} else if (locations.get(key).distanceSquared(player.getLocation()) > maxDistance*maxDistance) {
				// too far away from first channeler
				sendMessage(player, strTooFarAway);
				return false;
			}
		}
		
		// add player to channelers
		c.put(player, System.currentTimeMillis());
		allChannelers.add(player);
		sendMessage(player, strStartChannel, "%k", key);
		
		// check if there are enough channelers to complete the spell
		if (c.size() >= reqParticipants && (double)c.size() / (double)Bukkit.getServer().getOnlinePlayers().length * 100.0 > reqPercent) {
			if (castDelay == 0) {
				finishSpell(key, c);
			} else {
				Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
					public void run() {
						finishSpell(key, (HashMap<Player,Long>)null);
					}
				}, castDelay);
			}
		}
		
		return true;
	}
	
	protected int getChannelerCount(String key) {
		if (channelers.containsKey(key)) {
			return channelers.get(key).size();
		} else {
			return 0;
		}
	}
	
	private void finishSpell(String key, HashMap<Player,Long> c) {
		if (c == null) {
			c = channelers.get(key);
		}
		
		finishSpell(key, locations.get(key));
		for (Player p : c.keySet()) {
			sendMessage(p, strSpellSuccess, "%k", key);
			allChannelers.remove(p);
		}
		c.clear();
		channelers.remove(key);
		locations.remove(key);
	}
	
	protected abstract void finishSpell(String key, Location location);
	
	@Override
	public boolean canCastWithItem() {
		return castWithItem;
	}
	
	@Override
	public boolean canCastByCommand() {
		return castByCommand;
	}
	
	private void removeChanneler(Player player) {
		for (HashMap<Player,Long> c : channelers.values()) {
			c.remove(player);
		}
		allChannelers.remove(player);
	}
	
	@Override
	public final void onPlayerMove(PlayerMoveEvent event) {
		if (allChannelers.contains(event.getPlayer())) {
			Location from = event.getFrom();
			Location to = event.getTo();
			if (from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
				removeChanneler(event.getPlayer());
				sendMessage(event.getPlayer(), strMoved);
			}
		}
	}
	
	@Override
	public final void onPlayerQuit(PlayerQuitEvent event) {
		if (allChannelers.contains(event.getPlayer())) {
			removeChanneler(event.getPlayer());
		}		
	}

}

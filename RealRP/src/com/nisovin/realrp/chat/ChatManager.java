package com.nisovin.realrp.chat;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChatEvent;

import com.nisovin.realrp.RealRP;
import com.nisovin.realrp.Settings;

public class ChatManager {

	private RealRP plugin;
	protected Settings settings;
	
	private HashMap<Player, Channel> activeChannels;
	private HashMap<Player, HashSet<Channel>> playerChannels;
	
	private IrcBot ircBot;
	
	public ChatManager(RealRP plugin) {
		this.plugin = plugin;
		this.settings = RealRP.settings();
		
		activeChannels = new HashMap<Player, Channel>();
		playerChannels = new HashMap<Player, HashSet<Channel>>();
		
		// load online players
		Player[] players = plugin.getServer().getOnlinePlayers();
		for (Player p : players) {
			HashSet<Channel> channels = new HashSet<Channel>();
			playerChannels.put(p, channels);
			channels.add(Channel.IC);
			activeChannels.put(p, Channel.IC);
			if (RealRP.settings().csLocalOOCEnabled) {
				channels.add(Channel.LOCAL_OOC);
			}
			if (RealRP.settings().csGlobalOOCEnabled) {
				channels.add(Channel.GLOBAL_OOC);
			}
			if (RealRP.settings().csAdminEnabled && p.hasPermission("realrp.chat.admin")) {
				channels.add(Channel.ADMIN);
			}
		}
		
		// load irc bot
		if (settings.csIRCEnabled) {
			ircBot = new IrcBot(this, settings.csIRCNetwork, settings.csIRCNickname, settings.csIRCChannel, settings.csIRCNickservPass, settings.csIRCAuthPass);
		}
	}
	
	public void onChat(PlayerChatEvent event) {
		final Player player = event.getPlayer();
		Channel channel = activeChannels.get(player);
		HashSet<Channel> channels = playerChannels.get(player);
		
		// force to IC if for some reason the player doesn't have a channel
		if (channel == null) {
			channel = Channel.IC;
			activeChannels.put(player, channel);
		}
		
		// set up channels if player doesn't have any
		if (channels == null) {
			channels = new HashSet<Channel>();
			channels.add(Channel.IC);
			channels.add(Channel.LOCAL_OOC);
			channels.add(Channel.GLOBAL_OOC);
			playerChannels.put(player, channels);
		}
		
		// check for channel prefix
		String message = event.getMessage();
		if (message.startsWith(settings.csICPrefix) && channels.contains(Channel.IC)) {
			channel = Channel.IC;
			event.setMessage(message.substring(settings.csICPrefix.length()));
		} else if (message.startsWith(settings.csLocalOOCPrefix) && channels.contains(Channel.LOCAL_OOC)) {
			channel = Channel.LOCAL_OOC;
			event.setMessage(message.substring(settings.csLocalOOCPrefix.length()));
		} else if (message.startsWith(settings.csGlobalOOCPrefix) && channels.contains(Channel.GLOBAL_OOC)) {
			channel = Channel.GLOBAL_OOC;
			event.setMessage(message.substring(settings.csGlobalOOCPrefix.length()));
		} else if (message.startsWith(settings.csAdminPrefix) && channels.contains(Channel.ADMIN)) {
			channel = Channel.ADMIN;
			event.setMessage(message.substring(settings.csAdminPrefix.length()));
		}
		
		// format message and set recipients
		String format = event.getFormat();
		Set<Player> recipients = event.getRecipients();
		if (channel == Channel.IC) {
			format = settings.csICFormat;
			removeOutOfRange(player, recipients, settings.csICRange);
			if (recipients.size() <= 1) {
				Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(RealRP.getPlugin(), new Runnable() {
					public void run() {
						player.sendMessage("Nobody hears you.");
					}
				}, 1);
			}
		} else if (channel == Channel.LOCAL_OOC) {
			format = settings.csLocalOOCFormat;
			removeOutOfRange(player, recipients, settings.csLocalOOCRange);
		} else if (channel == Channel.GLOBAL_OOC) {
			format = settings.csGlobalOOCFormat;
			HashSet<Channel> c;
			Player p;
			Iterator<Player> iter = recipients.iterator();
			while (iter.hasNext()) {
				p = iter.next();
				if (plugin.isCreatingCharacter(p)) {
					iter.remove();
				} else {
					c = playerChannels.get(p);					
					if (c != null && !c.contains(Channel.GLOBAL_OOC)) {
						iter.remove();
					}
				}
			}
		} else if (channel == Channel.ADMIN) {
			format = settings.csAdminFormat;
			recipients.clear();
			for (Map.Entry<Player, HashSet<Channel>> entry : playerChannels.entrySet()) {
				if (entry.getValue().contains(Channel.ADMIN)) {
					recipients.add(entry.getKey());
				}
			}
		} else {
			// no channel
		}
		event.setFormat(format.replace("%n","%1$s").replace("%m", "%2$s").replaceAll("&([0-9a-f])", "\u00A7$1"));
		
		// send to IRC if it's enabled and in Global OOC
		if (channel == Channel.GLOBAL_OOC && ircBot != null && ircBot.isConnected()) {
			String ircMsg = ChatColor.stripColor(settings.csIRCFormatToIRC.replace("%n", player.getDisplayName()).replace("%m", event.getMessage()));
			ircBot.sendMessage(ircMsg);
		}
		
	}
	
	public void fromIRC(String name, String message) {
		String msg = settings.csIRCFormatToGame.replace("%n", name).replace("%m", message);
		HashSet<Channel> channels;
		for (Player p : Bukkit.getServer().getOnlinePlayers()) {
			channels = playerChannels.get(p);
			if (channels != null && channels.contains(Channel.GLOBAL_OOC)) {
				RealRP.sendMessage(p, msg);
			}
		}
		System.out.println(ChatColor.stripColor(msg));
	}
	
	private void removeOutOfRange(Player speaker, Set<Player> recipients, int range) {
		recipients.clear();
		recipients.add(speaker);
		List<Entity> entities = speaker.getNearbyEntities(range, range, range);
		for (Entity entity : entities) {
			if (entity instanceof Player && !plugin.isCreatingCharacter((Player)entity)) {
				recipients.add((Player)entity);
			}
		}
	}
	
	public boolean joinChannel(Player player, String channel) {
		HashSet<Channel> channels = playerChannels.get(player);
		if (channels == null) {
			channels = new HashSet<Channel>();
			playerChannels.put(player, channels);
		}
		if (channel.equalsIgnoreCase(settings.csICName)) {
			channels.add(Channel.IC);
			activeChannels.put(player, Channel.IC);
		} else if (channel.equalsIgnoreCase(settings.csLocalOOCName)) {
			channels.add(Channel.LOCAL_OOC);
			activeChannels.put(player, Channel.LOCAL_OOC);
		} else if (channel.equalsIgnoreCase(settings.csGlobalOOCName)) {
			channels.add(Channel.GLOBAL_OOC);
			activeChannels.put(player, Channel.GLOBAL_OOC);
		} else if (channel.equalsIgnoreCase(settings.csAdminName) && player.hasPermission("realrp.chat.admin")) {
			channels.add(Channel.ADMIN);
			activeChannels.put(player, Channel.ADMIN);
		} else {
			return false;
		}
		return true;
	}
	
	public boolean leaveChannel(Player player, String channel) {
		HashSet<Channel> channels = playerChannels.get(player);
		if (channels == null) {
			channels = new HashSet<Channel>();
			playerChannels.put(player, channels);
		}
		if (channel.equalsIgnoreCase(settings.csICName)) {
			channels.remove(Channel.IC);
		} else if (channel.equalsIgnoreCase(settings.csLocalOOCName)) {
			channels.remove(Channel.LOCAL_OOC);
		} else if (channel.equalsIgnoreCase(settings.csGlobalOOCName)) {
			channels.remove(Channel.GLOBAL_OOC);
		} else if (channel.equalsIgnoreCase(settings.csAdminName)) {
			channels.remove(Channel.ADMIN);
		} else {
			return false;
		}
		return true;
		
	}
	
	public void playerJoin(Player player) {
		if (!activeChannels.containsKey(player)) {
			activeChannels.put(player, Channel.IC);
		}
		if (!playerChannels.containsKey(player)) {
			HashSet<Channel> channels = new HashSet<Channel>();
			channels.add(Channel.IC);
			channels.add(Channel.LOCAL_OOC);
			channels.add(Channel.GLOBAL_OOC);
			if (player.hasPermission("realrp.chat.admin")) {
				channels.add(Channel.ADMIN);
			}
			playerChannels.put(player, channels);
		}
		if (ircBot != null && ircBot.isConnected()) {
			ircBot.sendMessage(ChatColor.stripColor(player.getDisplayName()) + " has joined the game.");
		}
	}
	
	public void playerQuit(Player player) {
		if (ircBot != null && ircBot.isConnected()) {
			ircBot.sendMessage(ChatColor.stripColor(player.getDisplayName()) + " has left the game.");
		}
	}
	
	public void turnOff() {
		if (ircBot != null && ircBot.isConnected()) {
			ircBot.disconnect();
		}
	}
	
}

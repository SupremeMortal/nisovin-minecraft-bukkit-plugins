package com.nisovin.magicspells.zones;

import java.util.HashMap;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.MagicConfig;

public class NoMagicZoneManager {
	
	private HashMap<String, Class<? extends NoMagicZone>> zoneTypes;
	private HashMap<String, NoMagicZone> zones;

	public NoMagicZoneManager(MagicConfig config) {
		// create zone types
		zoneTypes = new HashMap<String, Class<? extends NoMagicZone>>();
		zoneTypes.put("cuboid", NoMagicZoneCuboid.class);
		zoneTypes.put("worldguard", NoMagicZoneWorldGuard.class);
		zoneTypes.put("residence", NoMagicZoneResidence.class);
		
		// get zones
		zones = new HashMap<String, NoMagicZone>();
				
		Set<String> zoneNodes = config.getKeys("no-magic-zones");
		if (zoneNodes != null) {
			for (String node : zoneNodes) {
				ConfigurationSection zoneConfig = config.getSection("no-magic-zones." + node);
				
				// get zone type
				String type = zoneConfig.getString("type", "");
				if (type.isEmpty()) {
					MagicSpells.error("Invalid no-magic zone type '" + type + "' on zone '" + node + "'");
					continue;
				}
				Class<? extends NoMagicZone> clazz = zoneTypes.get(type);
				if (clazz == null) {
					MagicSpells.error("Invalid no-magic zone type '" + type + "' on zone '" + node + "'");
					continue;
				}
				
				// create zone
				NoMagicZone zone;
				try {
					zone = clazz.newInstance();
				} catch (Exception e) {
					MagicSpells.error("Failed to create no-magic zone '" + node + "'");
					continue;
				}
				zone.create(zoneConfig);
				zones.put(node, zone);
			}
		}
	}
	
	public boolean willFizzle(Player player, Spell spell) {
		return willFizzle(player.getLocation(), spell);
	}
	
	public boolean willFizzle(Location location, Spell spell) {
		for (NoMagicZone zone : zones.values()) {
			if (zone.willFizzle(location, spell)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean inZone(Player player, String zoneName) {
		NoMagicZone zone = zones.get(zoneName);
		if (zone != null && zone.inZone(player.getLocation())) {
			return true;
		}
		return false;
	}
	
	public void sendNoMagicMessage(Player player, Spell spell) {
		for (NoMagicZone zone : zones.values()) {
			if (zone.willFizzle(player, spell)) {
				MagicSpells.sendMessage(player, zone.getMessage());
				return;
			}
		}
	}
	
	public int zoneCount() {
		return zones.size();
	}
	
}

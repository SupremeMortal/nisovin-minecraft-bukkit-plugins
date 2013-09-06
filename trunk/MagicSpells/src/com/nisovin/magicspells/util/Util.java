package com.nisovin.magicspells.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.util.ItemNameResolver.ItemTypeAndData;

public class Util {

	public static Map<String, ItemStack> predefinedItems = new HashMap<String, ItemStack>();
	
	public static ItemStack getItemStackFromString(String string) {
		try {
			if (predefinedItems.containsKey(string)) return predefinedItems.get(string).clone();

			ItemStack item;
			String s = string;
			String name = null;
			String[] lore = null;
			HashMap<Enchantment, Integer> enchants = null;
			int color = -1;
			if (s.contains("|")) {
				String[] temp = s.split("\\|");
				s = temp[0];
				if (temp.length == 1) {
					name = "";
				} else {
					name = ChatColor.translateAlternateColorCodes('&', temp[1].replace("__", " "));
					if (temp.length > 2) {
						lore = Arrays.copyOfRange(temp, 2, temp.length);
						for (int i = 0; i < lore.length; i++) {
							lore[i] = ChatColor.translateAlternateColorCodes('&', lore[i].replace("__", " "));
						}
					}
				}
			}
			if (s.contains(";")) {
				String[] temp = s.split(";", 2);
				s = temp[0];
				enchants = new HashMap<Enchantment, Integer>();
				if (temp[1].length() > 0) {
					String[] split = temp[1].split("\\+");
					for (int i = 0; i < split.length; i++) {
						String[] enchantData = split[i].split("-");
						Enchantment ench;
						if (enchantData[0].matches("[0-9]+")) {
							ench = Enchantment.getById(Integer.parseInt(enchantData[0]));
						} else {
							ench = Enchantment.getByName(enchantData[0].toUpperCase());
						}
						if (ench != null && enchantData[1].matches("[0-9]+")) {
							enchants.put(ench, Integer.parseInt(enchantData[1]));
						}
					}
				}
			}
			if (s.contains("#")) {
				String[] temp = s.split("#");
				s = temp[0];
				if (temp[1].matches("[0-9A-Fa-f]+")) {
					color = Integer.parseInt(temp[1], 16);
				}
			}
			ItemTypeAndData itemTypeAndData = MagicSpells.getItemNameResolver().resolve(s);
			if (itemTypeAndData != null) {
				item = new ItemStack(itemTypeAndData.id, 1, itemTypeAndData.data);
			} else {
				return null;
			}
			if (name != null || lore != null || color >= 0) {
				try {
					ItemMeta meta = item.getItemMeta();
					if (name != null) {
						meta.setDisplayName(name);
					}
					if (lore != null) {
						meta.setLore(Arrays.asList(lore));
					}
					if (color >= 0 && meta instanceof LeatherArmorMeta) {
						((LeatherArmorMeta)meta).setColor(Color.fromRGB(color));
					}
					item.setItemMeta(meta);
				} catch (Exception e) {
					MagicSpells.error("Failed to process item meta for item: " + s);
				}
			}
			if (enchants != null) {
				if (enchants.size() > 0) {
					item.addUnsafeEnchantments(enchants);
				} else {
					item = MagicSpells.getVolatileCodeHandler().addFakeEnchantment(item);
				}
			}
			return item;
		} catch (Exception e) {
			MagicSpells.handleException(e);
			return null;
		}
	}
	
	public static ItemStack getItemStackFromConfig(ConfigurationSection config) {
		try {
			if (!config.contains("type")) return null;
			
			// basic item
			ItemTypeAndData itemTypeAndData = MagicSpells.getItemNameResolver().resolve(config.getString("type"));
			if (itemTypeAndData == null) return null;
			ItemStack item = new ItemStack(itemTypeAndData.id, 1, itemTypeAndData.data);
			ItemMeta meta = item.getItemMeta();
			
			// name and lore
			if (config.contains("name") && config.isString("name")) {
				meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', config.getString("name")));
			}
			if (config.contains("lore")) {
				if (config.isList("lore")) {
					List<String> lore = config.getStringList("lore");
					for (int i = 0; i < lore.size(); i++) {
						lore.set(i, ChatColor.translateAlternateColorCodes('&', lore.get(i)));
					}
					meta.setLore(lore);
				} else if (config.isString("lore")) {
					List<String> lore = new ArrayList<String>();
					lore.add(ChatColor.translateAlternateColorCodes('&', config.getString("lore")));
					meta.setLore(lore);
				}
			}
			
			// enchants
			if (config.contains("enchants") && config.isList("enchants")) {
				List<String> enchants = config.getStringList("enchants");
				for (String enchant : enchants) {
					String[] data = enchant.split(" ");
					Enchantment e = null;
					try {
						int id = Integer.parseInt(data[0]);
						e = Enchantment.getById(id);
					} catch (NumberFormatException ex) {
						e = Enchantment.getByName(data[0].toUpperCase());
					}
					if (e != null) {
						int level = 0;
						if (data.length > 1) {
							try {
								level = Integer.parseInt(data[1]);
							} catch (NumberFormatException ex) {						
							}
						}
						meta.addEnchant(e, level, true);
					}
				}
			}
			
			// armor color
			if (config.contains("color") && config.isString("color") && meta instanceof LeatherArmorMeta) {
				try {
					int color = Integer.parseInt(config.getString("color").replace("#", ""), 16);
					((LeatherArmorMeta)meta).setColor(Color.fromRGB(color));
				} catch (NumberFormatException e) {				
				}
			}
			
			// potion effects
			if (config.contains("potioneffects") && config.isList("potioneffects") && meta instanceof PotionMeta) {
				((PotionMeta)meta).clearCustomEffects();
				List<String> potionEffects = config.getStringList("potioneffects");
				for (String potionEffect : potionEffects) {
					String[] data = potionEffect.split(" ");
					PotionEffectType t = null;
					try {
						int id = Integer.parseInt(data[0]);
						t = PotionEffectType.getById(id);
					} catch (NumberFormatException e) {
						t = PotionEffectType.getByName(data[0].toUpperCase());
					}
					if (t != null) {
						int level = 0;
						if (data.length > 1) {
							try {
								level = Integer.parseInt(data[1]);
							} catch (NumberFormatException ex) {						
							}
						}
						int duration = 600;
						if (data.length > 2) {
							try {
								duration = Integer.parseInt(data[2]);
							} catch (NumberFormatException ex) {						
							}
						}
						boolean ambient = false;
						if (data.length > 3 && (data[3].equalsIgnoreCase("true") || data[3].equalsIgnoreCase("yes") || data[3].equalsIgnoreCase("ambient"))) {
							ambient = true;
						}
						((PotionMeta)meta).addCustomEffect(new PotionEffect(t, duration, level, ambient), true);
					}
				}
			}
			
			// skull owner
			if (config.contains("skullowner") && config.isString("skullowner") && meta instanceof SkullMeta) {
				((SkullMeta)meta).setOwner(config.getString("skullowner"));
			}
			
			// repair cost
			if (config.contains("repaircost") && config.isInt("repaircost") && meta instanceof Repairable) {
				((Repairable)meta).setRepairCost(config.getInt("repaircost"));
			}
			
			// written book
			if (meta instanceof BookMeta) {
				if (config.contains("title") && config.isString("title")) {
					((BookMeta)meta).setTitle(ChatColor.translateAlternateColorCodes('&', config.getString("title")));
				}
				if (config.contains("author") && config.isString("author")) {
					((BookMeta)meta).setAuthor(ChatColor.translateAlternateColorCodes('&', config.getString("author")));
				}
				if (config.contains("pages") && config.isList("pages")) {
					List<String> pages = config.getStringList("pages");
					for (int i = 0; i < pages.size(); i++) {
						pages.set(i, ChatColor.translateAlternateColorCodes('&', pages.get(i)));
					}
					((BookMeta)meta).setPages(pages);
				}
			}
			
			item.setItemMeta(meta);
			return item;
		} catch (Exception e) {
			return null;
		}
	}
	
	public static void setLoreData(ItemStack item, String data) {
		ItemMeta meta = item.getItemMeta();
		List<String> lore;
		if (meta.hasLore()) {
			lore = meta.getLore();
			if (lore.size() > 0) {
				String s = ChatColor.stripColor(lore.get(lore.size() - 1));
				if (s.startsWith("MS$")) {
					lore.remove(lore.size() - 1);
				}
			}
		} else {
			lore = new ArrayList<String>();
		}
		lore.add(ChatColor.BLACK.toString() + ChatColor.MAGIC.toString() + "MS$:" + data);
		meta.setLore(lore);
		item.setItemMeta(meta);
	}
	
	public static String getLoreData(ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		if (meta.hasLore()) {
			List<String> lore = meta.getLore();
			if (lore.size() > 0) {
				String s = ChatColor.stripColor(lore.get(lore.size() - 1));
				if (s.startsWith("MS$")) {
					return s.substring(4);
				}
			}
		}
		return null;
	}
	
	public static void removeLoreData(ItemStack item) {
		ItemMeta meta = item.getItemMeta();
		List<String> lore;
		if (meta.hasLore()) {
			lore = meta.getLore();
			if (lore.size() > 0) {
				String s = ChatColor.stripColor(lore.get(lore.size() - 1));
				if (s.startsWith("MS$")) {
					lore.remove(lore.size() - 1);
					if (lore.size() > 0) {
						meta.setLore(lore);
					} else {
						meta.setLore(null);
					}
					item.setItemMeta(meta);
				}
			}
		}
	}
	
	public static void setFacing(Player player, Vector vector) {
		Location loc = player.getLocation();
		setLocationFacingFromVector(loc, vector);
		player.teleport(loc);
	}
	
	public static void setLocationFacingFromVector(Location location, Vector vector) {
		double yaw = getYawOfVector(vector);
		double pitch = Math.toDegrees(-Math.asin(vector.getY()));				
		location.setYaw((float)yaw);
		location.setPitch((float)pitch);
	}
	
	public static double getYawOfVector(Vector vector) {
		return Math.toDegrees(Math.atan2(-vector.getX(), vector.getZ()));
	}
	
	public static boolean arrayContains(int[] array, int value) {
		for (int i : array) {
			if (i == value) {
				return true;
			}
		}
		return false;
	}

	public static boolean arrayContains(String[] array, String value) {
		for (String i : array) {
			if (i.equals(value)) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean arrayContains(Object[] array, Object value) {
		for (Object i : array) {
			if (i != null && i.equals(value)) {
				return true;
			}
		}
		return false;
	}
	
	public static String arrayJoin(String[] array, char with) {
		if (array == null || array.length == 0) {
			return "";
		}
		int len = array.length;
		StringBuilder sb = new StringBuilder(16 + len * 8);
		sb.append(array[0]);
		for (int i = 1; i < len; i++) {
			sb.append(with);
			sb.append(array[i]);
		}
		return sb.toString();
	}
	
	public static String listJoin(List<String> list) {
		if (list == null || list.size() == 0) {
			return "";
		}
		int len = list.size();
		StringBuilder sb = new StringBuilder(len * 12);
		sb.append(list.get(0));
		for (int i = 1; i < len; i++) {
			sb.append(' ');
			sb.append(list.get(i));
		}
		return sb.toString();
	}
	
	public static String[] splitParams(String string, int max) {
		String[] words = string.trim().split(" ");
		if (words.length <= 1) {
			return words;
		}
		ArrayList<String> list = new ArrayList<String>();		
		char quote = ' ';
		String building = "";
		
		for (String word : words) {
			if (word.length() == 0) continue;
			if (max > 0 && list.size() == max - 1) {
				if (!building.isEmpty()) building += " ";
				building += word;
			} else if (quote == ' ') {
				if (word.length() == 1 || (word.charAt(0) != '"' && word.charAt(0) != '\'')) {
					list.add(word);
				} else {
					quote = word.charAt(0);
					if (quote == word.charAt(word.length() - 1)) {
						quote = ' ';
						list.add(word.substring(1, word.length() - 1));
					} else {
						building = word.substring(1);
					}
				}
			} else {
				if (word.charAt(word.length() - 1) == quote) {
					list.add(building + " " + word.substring(0, word.length() - 1));
					building = "";
					quote = ' ';
				} else {
					building += " " + word;
				}
			}
		}
		if (!building.isEmpty()) {
			list.add(building);
		}
		return list.toArray(new String[list.size()]);
	}
	
	public static String[] splitParams(String string) {
		return splitParams(string, 0);
	}
	
	public static String[] splitParams(String[] split, int max) {
		return splitParams(arrayJoin(split, ' '), max);
	}
	
	public static String[] splitParams(String[] split) {
		return splitParams(arrayJoin(split, ' '), 0);
	}
	
	public static List<String> tabCompleteSpellName(CommandSender sender, String partial) {
		List<String> matches = new ArrayList<String>();
		if (sender instanceof Player) {
			Spellbook spellbook = MagicSpells.getSpellbook((Player)sender);
			for (Spell spell : spellbook.getSpells()) {
				if (spellbook.canTeach(spell)) {
					if (spell.getName().toLowerCase().startsWith(partial)) {
						matches.add(spell.getName());
					} else {
						String[] aliases = spell.getAliases();
						if (aliases != null && aliases.length > 0) {
							for (String alias : aliases) {
								if (alias.toLowerCase().startsWith(partial)) {
									matches.add(alias);
								}
							}
						}
					}
				}
			}
		} else if (sender.isOp()) {
			for (Spell spell : MagicSpells.spells()) {
				if (spell.getName().toLowerCase().startsWith(partial)) {
					matches.add(spell.getName());
				} else {
					String[] aliases = spell.getAliases();
					if (aliases != null && aliases.length > 0) {
						for (String alias : aliases) {
							if (alias.toLowerCase().startsWith(partial)) {
								matches.add(alias);
							}
						}
					}
				}
			}
		}
		if (matches.size() > 0) {
			return matches;
		}
		return null;
	}
	
	public static boolean removeFromInventory(Inventory inventory, ItemStack item) {
		int amt = item.getAmount();
		ItemStack[] items = inventory.getContents();
		for (int i = 0; i < items.length; i++) {
			if (items[i] != null && item.isSimilar(items[i])) {
				if (items[i].getAmount() > amt) {
					items[i].setAmount(items[i].getAmount() - amt);
					amt = 0;
					break;
				} else if (items[i].getAmount() == amt) {
					items[i] = null;
					amt = 0;
					break;
				} else {
					amt -= items[i].getAmount();
					items[i] = null;
				}
			}
		}
		if (amt == 0) {
			inventory.setContents(items);
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean addToInventory(Inventory inventory, ItemStack item) {
		int amt = item.getAmount();
		ItemStack[] items = inventory.getContents();
		for (int i = 0; i < items.length; i++) {
			if (items[i] != null && item.isSimilar(items[i])) {
				if (items[i].getAmount() + amt <= items[i].getMaxStackSize()) {
					items[i].setAmount(items[i].getAmount() + amt);
					amt = 0;
					break;
				} else {
					int diff = items[i].getMaxStackSize() - items[i].getAmount();
					items[i].setAmount(items[i].getMaxStackSize());
					amt -= diff;
				}
			}
		}
		if (amt > 0) {
			for (int i = 0; i < items.length; i++) {
				if (items[i] == null) {
					if (amt > item.getMaxStackSize()) {
						items[i] = item.clone();
						items[i].setAmount(item.getMaxStackSize());
						amt -= item.getMaxStackSize();
					} else {
						items[i] = item.clone();
						items[i].setAmount(amt);
						amt = 0;
						break;
					}
				}
			}
		}
		if (amt == 0) {
			inventory.setContents(items);
			return true;
		} else {
			return false;
		}
	}
	
	public static void rotateVector(Vector v, float degrees) {
		double rad = Math.toRadians(degrees);
		double sin = Math.sin(rad);
		double cos = Math.cos(rad);
		double x = (v.getX() * cos) - (v.getZ() * sin);
		double z = (v.getX() * sin) + (v.getZ() * cos);
		v.setX(x);
		v.setZ(z);
	}
	
	public static boolean downloadFile(String url, File file) {
		/*try {
			URL website = new URL(url);
		    ReadableByteChannel rbc = Channels.newChannel(website.openStream());
		    FileOutputStream fos = new FileOutputStream(file);
		    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
		    fos.close();
		    rbc.close();
		    return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}*/
		return false;
	}
	
}

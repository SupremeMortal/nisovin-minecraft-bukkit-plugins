package com.nisovin.magicspells;

import java.io.File;
import java.lang.reflect.Constructor;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;


import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import com.nisovin.magicspells.events.MagicSpellsLoadedEvent;
import com.nisovin.magicspells.events.SpellLearnEvent;
import com.nisovin.magicspells.events.SpellLearnEvent.LearnSource;
import com.nisovin.magicspells.spells.*;
import com.nisovin.magicspells.util.MagicConfig;

public class MagicSpells extends JavaPlugin {

	public static MagicSpells plugin;

	static CraftBukkitHandle craftbukkit;
	
	static boolean debug;
	static int debugLevel;
	static ChatColor textColor;
	static int broadcastRange;
	
	static boolean opsHaveAllSpells;
	static boolean defaultAllPermsFalse;
	
	static boolean separatePlayerSpellsPerWorld;
	static boolean allowCycleToNoSpell;
	static boolean onlyCycleToCastableSpells;
	static int spellIconSlot;
	static boolean allowCastWithFist;
	static boolean ignoreDefaultBindings;
	static boolean showStrCostOnMissingReagents;
	static HashSet<Byte> losTransparentBlocks;
	static List<Integer> ignoreCastItemDurability;
	static HashMap<EntityType, String> entityNames;
	static int globalCooldown;
	static boolean castOnAnimate;
	
	static boolean enableManaBars;
	static int manaPotionCooldown;
	static String strManaPotionOnCooldown;
	static HashMap<ItemStack,Integer> manaPotions;
	
	static String strCastUsage;
	static String strUnknownSpell;
	static String strSpellChange;
	static String strSpellChangeEmpty;
	static String strOnCooldown;
	static String strMissingReagents;
	static String strCantCast;
	static String strWrongWorld;
	static String strCantBind;
	static String strConsoleName;
	
	static HashMap<String,Spell> spells; // map internal names to spells
	static HashMap<String,Spell> spellNames; // map configured names to spells
	static ArrayList<Spell> spellsOrdered; // spells in loaded order
	static HashMap<String,Spellbook> spellbooks; // player spellbooks
	static HashMap<String,Spell> incantations; // map incantation strings to spells
		
	static ManaHandler mana;
	static HashMap<Player,Long> manaPotionCooldowns;
	static NoMagicZoneHandler noMagicZones;
	static BuffManager buffManager;
	
	@Override
	public void onEnable() {
		plugin = this;		
		load();
	}
	
	private void load() {		
		PluginManager pm = plugin.getServer().getPluginManager();
		
		// create storage stuff
		spells = new HashMap<String,Spell>();
		spellNames = new HashMap<String,Spell>();
		spellsOrdered = new ArrayList<Spell>();
		spellbooks = new HashMap<String,Spellbook>();
		incantations = new HashMap<String,Spell>();
		
		// make sure directories are created
		this.getDataFolder().mkdir();
		new File(this.getDataFolder(), "spellbooks").mkdir();
		
		// load config
		File configFile = new File(getDataFolder(), "config.yml");
		if (!configFile.exists()) saveDefaultConfig();
		MagicConfig config = new MagicConfig(configFile);
		if (!config.isLoaded()) {
			MagicSpells.log(Level.SEVERE, "Error in config file, stopping config load");
			return;
		}
		
		if (config.getBoolean("general.enable-volatile-features", true)) {
			craftbukkit = new CraftBukkitHandleEnabled();
		} else {
			craftbukkit = new CraftBukkitHandleDisabled();
		}
		
		debug = config.getBoolean("general.debug", false);
		debugLevel = config.getInt("general.debug-level", 3);
		textColor = ChatColor.getByChar(config.getString("general.text-color", ChatColor.DARK_AQUA.getChar() + ""));
		broadcastRange = config.getInt("general.broadcast-range", 20);
		
		opsHaveAllSpells = config.getBoolean("general.ops-have-all-spells", true);
		defaultAllPermsFalse = config.getBoolean("general.default-all-perms-false", false);

		separatePlayerSpellsPerWorld = config.getBoolean("general.separate-player-spells-per-world", false);
		allowCycleToNoSpell = config.getBoolean("general.allow-cycle-to-no-spell", false);
		onlyCycleToCastableSpells = config.getBoolean("general.only-cycle-to-castable-spells", true);
		spellIconSlot = config.getInt("general.spell-icon-slot", -1);
		allowCastWithFist = config.getBoolean("general.allow-cast-with-fist", false);
		ignoreDefaultBindings = config.getBoolean("general.ignore-default-bindings", false);
		showStrCostOnMissingReagents = config.getBoolean("general.show-str-cost-on-missing-reagents", true);
		losTransparentBlocks = new HashSet<Byte>(config.getByteList("general.los-transparent-blocks", new ArrayList<Byte>()));
		if (losTransparentBlocks.size() == 0) {
			losTransparentBlocks.add((byte)Material.AIR.getId());
			losTransparentBlocks.add((byte)Material.TORCH.getId());
			losTransparentBlocks.add((byte)Material.REDSTONE_WIRE.getId());
			losTransparentBlocks.add((byte)Material.REDSTONE_TORCH_ON.getId());
			losTransparentBlocks.add((byte)Material.REDSTONE_TORCH_OFF.getId());
			losTransparentBlocks.add((byte)Material.YELLOW_FLOWER.getId());
			losTransparentBlocks.add((byte)Material.RED_ROSE.getId());
			losTransparentBlocks.add((byte)Material.BROWN_MUSHROOM.getId());
			losTransparentBlocks.add((byte)Material.RED_MUSHROOM.getId());
			losTransparentBlocks.add((byte)Material.LONG_GRASS.getId());
			losTransparentBlocks.add((byte)Material.DEAD_BUSH.getId());
			losTransparentBlocks.add((byte)Material.DIODE_BLOCK_ON.getId());
			losTransparentBlocks.add((byte)Material.DIODE_BLOCK_OFF.getId());
		}
		ignoreCastItemDurability = config.getIntList("general.ignore-cast-item-durability", new ArrayList<Integer>());
		globalCooldown = config.getInt("general.global-cooldown", 500);
		castOnAnimate = config.getBoolean("general.cast-on-animate", true);
		
		entityNames = new HashMap<EntityType, String>();
		if (config.contains("general.entity-names")) {
			Set<String> keys = config.getSection("general.entity-names").getKeys(false);
			for (String key : keys) {
				EntityType entityType = EntityType.fromName(key);
				if (entityType != null) {
					entityNames.put(entityType, config.getString("general.entity-names." + key, entityType.getName().toLowerCase()));
				}
			}
		}
		
		strCastUsage = config.getString("general.str-cast-usage", "Usage: /cast <spell>. Use /cast list to see a list of spells.");
		strUnknownSpell = config.getString("general.str-unknown-spell", "You do not know a spell with that name.");
		strSpellChange = config.getString("general.str-spell-change", "You are now using the %s spell.");
		strSpellChangeEmpty = config.getString("general.str-spell-change-empty", "You are no longer using a spell.");
		strOnCooldown = config.getString("general.str-on-cooldown", "That spell is on cooldown.");
		strMissingReagents = config.getString("general.str-missing-reagents", "You do not have the reagents for that spell.");
		strCantCast = config.getString("general.str-cant-cast", "You can't cast that spell right now.");
		strCantBind = config.getString("general.str-cant-bind", "You cannot bind that spell to that item.");
		strWrongWorld = config.getString("general.str-wrong-world", "You cannot cast that spell here.");
		strConsoleName = config.getString("general.console-name", "Admin");
		
		enableManaBars = config.getBoolean("general.mana.enable-mana-bars", true);
		manaPotionCooldown = config.getInt("general.mana.mana-potion-cooldown", 30);
		strManaPotionOnCooldown = config.getString("general.mana.str-mana-potion-on-cooldown", "You cannot use another mana potion yet.");
				
		// init permissions
		addPermission(pm, "noreagents", defaultAllPermsFalse? PermissionDefault.FALSE : PermissionDefault.OP, "Allows casting without needing reagents");
		addPermission(pm, "nocooldown", defaultAllPermsFalse? PermissionDefault.FALSE : PermissionDefault.OP, "Allows casting without being affected by cooldowns");
		addPermission(pm, "notarget", PermissionDefault.FALSE, "Prevents being targeted by any targeted spells");
		addPermission(pm, "silent", PermissionDefault.FALSE, "Prevents cast messages from being broadcast to players");
		HashMap<String, Boolean> permGrantChildren = new HashMap<String,Boolean>();
		HashMap<String, Boolean> permLearnChildren = new HashMap<String,Boolean>();
		HashMap<String, Boolean> permCastChildren = new HashMap<String,Boolean>();
		HashMap<String, Boolean> permTeachChildren = new HashMap<String,Boolean>();
		
		// load spells
		loadSpells(config, pm, permGrantChildren, permLearnChildren, permCastChildren, permTeachChildren);
		loadMultiSpells(config, pm, permGrantChildren, permLearnChildren, permCastChildren, permTeachChildren);
		log("Spells loaded: " + spells.size());
		if (spells.size() == 0) {
			MagicSpells.log(Level.SEVERE, "No spells loaded!");
			return;
		}
		
		// finalize spell permissions
		addPermission(pm, "grant.*", PermissionDefault.FALSE, permGrantChildren);
		addPermission(pm, "learn.*", defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE, permLearnChildren);
		addPermission(pm, "cast.*", defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE, permCastChildren);
		addPermission(pm, "teach.*", defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE, permTeachChildren);
		
		// advanced perms
		addPermission(pm, "advanced.list", PermissionDefault.FALSE);
		addPermission(pm, "advanced.forget", PermissionDefault.FALSE);
		addPermission(pm, "advanced.scroll", PermissionDefault.FALSE);
		HashMap<String, Boolean> advancedPermChildren = new HashMap<String,Boolean>();
		advancedPermChildren.put("magicspells.advanced.list", true);
		advancedPermChildren.put("magicspells.advanced.forget", true);
		advancedPermChildren.put("magicspells.advanced.scroll", true);
		addPermission(pm, "advanced.*", defaultAllPermsFalse? PermissionDefault.FALSE : PermissionDefault.OP, advancedPermChildren);
		
		// load in-game spell names, incantations, and initialize spells
		for (Spell spell : spells.values()) {
			spellNames.put(spell.getName(), spell);
			String[] aliases = spell.getAliases();
			if (aliases != null && aliases.length > 0) {
				for (String alias : aliases) {
					if (!spellNames.containsKey(alias)) {
						spellNames.put(alias, spell);
					}
				}
			}
			List<String> incs = spell.getIncantations();
			if (incs != null && incs.size() > 0) {
				for (String s : incs) {
					incantations.put(s.toLowerCase(), spell);
				}
			}
			spell.initialize();
		}
		
		// load online player spellbooks
		for (Player p : getServer().getOnlinePlayers()) {
			spellbooks.put(p.getName(), new Spellbook(p, this));
		}
		
		// setup mana bar manager
		if (enableManaBars) {
			mana = new ManaBarManager(config);
			for (Player p : getServer().getOnlinePlayers()) {
				mana.createManaBar(p);
			}
		}
		
		// setup buff manager
		int buffCheckInterval = config.getInt("general.buff-check-interval", 0);
		if (buffCheckInterval > 0) {
			buffManager = new BuffManager(buffCheckInterval);
		}
		
		// load mana potions
		List<String> manaPots = config.getStringList("general.mana.mana-potions", null);
		if (manaPots != null && manaPots.size() > 0) {
			manaPotions = new HashMap<ItemStack,Integer>();
			for (int i = 0; i < manaPots.size(); i++) {
				String[] data = manaPots.get(i).split(" ");
				ItemStack item;
				if (data[0].contains(":")) {
					String[] data2 = data[0].split(":");
					item = new ItemStack(Integer.parseInt(data2[0]), 1, Short.parseShort(data2[1]));
				} else {
					item = new ItemStack(Integer.parseInt(data[0]), 1);					
				}
				manaPotions.put(item, Integer.parseInt(data[1]));
			}
			manaPotionCooldowns = new HashMap<Player,Long>();
		}
		
		// load no-magic zones
		noMagicZones = new NoMagicZoneManager(config);
		if (noMagicZones.zoneCount() == 0) {
			noMagicZones = null;
		}
		
		// load listeners
		pm.registerEvents(new MagicPlayerListener(this), this);
		pm.registerEvents(new MagicSpellListener(this), this);
		if (incantations.size() > 0) {
			pm.registerEvents(new MagicChatListener(this), this);
		}
		
		// set up metrics
		//setupMetrics();
		
		// call loaded event
		pm.callEvent(new MagicSpellsLoadedEvent(this));
	}
	
	private void loadSpells(MagicConfig config, PluginManager pm, HashMap<String, Boolean> permGrantChildren, HashMap<String, Boolean> permLearnChildren, HashMap<String, Boolean> permCastChildren, HashMap<String, Boolean> permTeachChildren) {
		// load spells from plugin folder
		final List<File> jarList = new ArrayList<File>();
		for (File file : getDataFolder().listFiles()) {
			if (file.getName().endsWith(".jar")) {
				jarList.add(file);
			}
		}

		// create class loader
		URL[] urls = new URL[jarList.size()+1];
		ClassLoader cl = getClassLoader();
		try {		
			urls[0] = getDataFolder().toURI().toURL();
			for(int i = 1; i <= jarList.size(); i++) {
				urls[i] = jarList.get(i-1).toURI().toURL();
			}
			cl = new URLClassLoader(urls, getClassLoader());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		// get spells from config
		Set<String> spellKeys = config.getSpellKeys();
		if (spellKeys == null) return;
		for (String spellName : spellKeys) {
			String className = "";
			if (config.contains("spells." + spellName + ".spell-class")) {
				className = config.getString("spells." + spellName + ".spell-class", "");
			}
			if (className == null || className.isEmpty()) {
				error("Spell '" + spellName + "' does not have a spell-class property");
				continue;
			} else if (className.startsWith(".")) {
				className = "com.nisovin.magicspells.spells" + className;
			}
			if (config.getBoolean("spells." + spellName + ".enabled", true)) {
				try {
					// load spell class
					Class<? extends Spell> spellClass = cl.loadClass(className).asSubclass(Spell.class);
					Constructor<? extends Spell> constructor = spellClass.getConstructor(MagicConfig.class, String.class);
					constructor.setAccessible(true);
					Spell spell = constructor.newInstance(config, spellName);
					spells.put(spellName, spell);
					spellsOrdered.add(spell);
					
					// add permissions
					addPermission(pm, "grant." + spellName, PermissionDefault.FALSE);
					addPermission(pm, "learn." + spellName, defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE);
					addPermission(pm, "cast." + spellName, defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE);
					addPermission(pm, "teach." + spellName, defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE);
					permGrantChildren.put("magicspells.grant." + spellName, true);
					permLearnChildren.put("magicspells.learn." + spellName, true);
					permCastChildren.put("magicspells.cast." + spellName, true);
					permTeachChildren.put("magicspells.teach." + spellName, true);
					
					// done
					debug(2, "Loaded spell: " + spellName);
					
				} catch (ClassNotFoundException e) {
					error("Unable to load spell " + spellName + " (missing class " + className + ")");
					if (className.contains("instant")) {
						error("(Maybe try " + className.replace("com.nisovin.magicspells.spells.instant.", ".targeted.") + ")");
					}
				} catch (NoSuchMethodException e) {
					error("Unable to load spell " + spellName + " (malformed class)");
				} catch (Exception e) {
					error("Unable to load spell " + spellName + " (unknown error)");
					e.printStackTrace();
				}
			}
		}
	}
	
	private void loadMultiSpells(MagicConfig config, PluginManager pm, HashMap<String, Boolean> permGrantChildren, HashMap<String, Boolean> permLearnChildren, HashMap<String, Boolean> permCastChildren, HashMap<String, Boolean> permTeachChildren) {
		// load multi-spells
		Set<String> multiSpells = config.getKeys("multispells");
		if (multiSpells != null && multiSpells.size() > 0) {
			error("Please update your Multi Spells to the new layout! To do so,");
			error("just remove the 'multispells:' config header so that your multi spells");
			error("are in the same section as your normal spells. Then, add the");
			error("spell-class: \".MultiSpell\"");
			error("option to all of your Multi Spells. Now you're good to go!");
			for (String spellName : multiSpells) {
				if (config.getBoolean("multispells." + spellName + ".enabled", true)) {
					// initialize spell
					OldMultiSpell multiSpell = new OldMultiSpell(config, spellName);
					spells.put(spellName, multiSpell);
					spellsOrdered.add(multiSpell);
					// add permissions
					addPermission(pm, "grant." + spellName, PermissionDefault.FALSE);
					addPermission(pm, "learn." + spellName, defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE);
					addPermission(pm, "cast." + spellName, defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE);
					addPermission(pm, "teach." + spellName, defaultAllPermsFalse ? PermissionDefault.FALSE : PermissionDefault.TRUE);
					permGrantChildren.put("magicspells.grant." + spellName, true);
					permLearnChildren.put("magicspells.learn." + spellName, true);
					permCastChildren.put("magicspells.cast." + spellName, true);
					permTeachChildren.put("magicspells.teach." + spellName, true);
					// load complete
					debug(2, "Loaded multi-spell: " + spellName);
				}
			}
		}
	}
	
	private void addPermission(PluginManager pm, String perm, PermissionDefault permDefault) {
		addPermission(pm, perm, permDefault, null, null);
	}
	
	private void addPermission(PluginManager pm, String perm, PermissionDefault permDefault, String description) {
		addPermission(pm, perm, permDefault, null, description);
	}
	
	private void addPermission(PluginManager pm, String perm, PermissionDefault permDefault, Map<String,Boolean> children) {
		addPermission(pm, perm, permDefault, children, null);
	}
	
	private void addPermission(PluginManager pm, String perm, PermissionDefault permDefault, Map<String,Boolean> children, String description) {
		if (pm.getPermission("magicspells." + perm) == null) {
			if (description == null) {
				pm.addPermission(new Permission("magicspells." + perm, permDefault, children));
			} else {
				pm.addPermission(new Permission("magicspells." + perm, description, permDefault, children));
			}
		}
	}
	
	/*private void setupMetrics() {
		try {
			Metrics metrics = new Metrics();
			
			metrics.addCustomData(this, new Metrics.Plotter("Spell Count") {
				public int getValue() {
					return spells.size();
				}
			});
			
			metrics.beginMeasuringPlugin(this);
		} catch (IOException e) {
		}
	}*/
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String [] args) {
		if (command.getName().equalsIgnoreCase("magicspellcast")) {
			if (args == null || args.length == 0) {
				if (sender instanceof Player) {
					sendMessage((Player)sender, strCastUsage);
				} else {
					sender.sendMessage(textColor + strCastUsage);
				}
			} else if (sender.isOp() && args[0].equals("reload")) {
				if (args.length == 1) {
					unload();
					load();
					sender.sendMessage(textColor + "MagicSpells config reloaded.");
				} else {
					List<Player> players = getServer().matchPlayer(args[1]);
					if (players.size() != 1) {
						sender.sendMessage(textColor + "Player not found.");
					} else {
						Player player = players.get(0);
						spellbooks.put(player.getName(), new Spellbook(player, this));
						sender.sendMessage(textColor + player.getName() + "'s spellbook reloaded.");
					}
				}
			} else if (sender.isOp() && args[0].equals("debug")) {
				debug = !debug;
				sender.sendMessage("MagicSpells: debug mode " + (debug?"enabled":"disabled"));
			} else if (sender.isOp() && args[0].equals("configexplode")) {
				MagicConfig.explode();
				sender.sendMessage("MagicSpells: spell config exploded");
			} else if (sender instanceof Player) {
				Player player = (Player)sender;
				Spellbook spellbook = getSpellbook(player);
				Spell spell = getSpellByInGameName(args[0]);
				if (spell != null && spell.canCastByCommand() && spellbook.hasSpell(spell)) {
					if (spell.isValidItemForCastCommand(player.getItemInHand())) {
						String[] spellArgs = null;
						if (args.length > 1) {
							spellArgs = new String[args.length-1];
							for (int i = 1; i < args.length; i++) {
								spellArgs[i-1] = args[i];
							}
						}
						spell.cast(player, spellArgs);
					} else {
						sendMessage(player, spell.getStrWrongCastItem());
					}
				} else {
					sendMessage(player, strUnknownSpell);
				}
			} else { // not a player
				Spell spell = spellNames.get(args[0]);
				if (spell == null) {
					sender.sendMessage("Unknown spell.");
				} else {
					String[] spellArgs = null;
					if (args.length > 1) {
						spellArgs = new String[args.length-1];
						for (int i = 1; i < args.length; i++) {
							spellArgs[i-1] = args[i];
						}
					}
					boolean ok = spell.castFromConsole(sender, spellArgs);
					if (!ok) {
						sender.sendMessage("Cannot cast that spell from console.");
					}
				}
			}
			return true;
		} else if (command.getName().equalsIgnoreCase("magicspellmana")) {
			if (enableManaBars && sender instanceof Player) {
				Player player = (Player)sender;
				mana.showMana(player, true);
			}
			return true;
		}
		return false;
	}
	
	/**
	 * Gets the instance of the MagicSpells plugin
	 * @return the MagicSpells plugin
	 */
	public static MagicSpells getInstance() {
		return plugin;
	}
	
	public static Collection<Spell> spells() {
		return spells.values();
	}
	
	/**
	 * Gets a spell by its internal name (the key name in the config file)
	 * @param spellName the internal name of the spell to find
	 * @return the Spell found, or null if no spell with that name was found
	 */
	public static Spell getSpellByInternalName(String spellName) {
		return spells.get(spellName);
	}
	
	/**
	 * Gets a spell by its in-game name (the name specified with the 'name' config option)
	 * @param spellName the in-game name of the spell to find
	 * @return the Spell found, or null if no spell with that name was found
	 */
	public static Spell getSpellByInGameName(String spellName) {
		return spellNames.get(spellName);
	}
	
	/**
	 * Gets a player's spellbook, which contains known spells and handles spell permissions. 
	 * If a player does not have a spellbook, one will be created.
	 * @param player the player to get a spellbook for
	 * @return the player's spellbook
	 */
	public static Spellbook getSpellbook(Player player) {
		Spellbook spellbook = spellbooks.get(player.getName());
		if (spellbook == null) {
			spellbook = new Spellbook(player, plugin);
			spellbooks.put(player.getName(), spellbook);
		}
		return spellbook;
	}
	
	/**
	 * Gets a list of blocks that are considered transparent
	 * @return list of block types
	 */
	public static HashSet<Byte> getTransparentBlocks() {
		return losTransparentBlocks;
	}
	
	/**
	 * Gets a map of entity types and their configured names, to be used when sending messages to players
	 * @return the map
	 */
	public static HashMap<EntityType, String> getEntityNames() {
		return entityNames;
	}
	
	/**
	 * Checks whether to ignore the durability on the given type when using it as a cast item.
	 * @param type the type to check
	 * @return whether to ignore durability
	 */
	public static boolean ignoreCastItemDurability(int type) {
		if (ignoreCastItemDurability != null && ignoreCastItemDurability.contains(type)) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Gets the handler for no-magic zones.
	 * @return the no-magic zone handler
	 */
	public static NoMagicZoneHandler getNoMagicZoneHandler() {
		return noMagicZones;
	}
	
	public static BuffManager getBuffManager() {
		return buffManager;
	}
	
	/**
	 * Sets the handler for no-magic zones
	 * @param handler the no-magic zone handler
	 */
	public static void setNoMagicZoneHandler(NoMagicZoneHandler handler) {
		noMagicZones = handler;
	}
	
	/**
	 * Gets the mana handler, which handles all mana transactions.
	 * @return the mana handler
	 */
	public static ManaHandler getManaHandler() {
		return mana;
	}
	
	/**
	 * Sets the mana handler, which handles all mana transactions.
	 * @param handler the mana handler
	 */
	public static void setManaHandler(ManaHandler handler) {
		mana.turnOff();
		mana = handler;
	}
	
	public static CraftBukkitHandle getVolatileCodeHandler() {
		return craftbukkit;
	}
	
	/**
	 * Formats a string by performing the specified replacements.
	 * @param message the string to format
	 * @param replacements the replacements to make, in pairs.
	 * @return the formatted string
	 */
	static public String formatMessage(String message, String... replacements) {
		if (message == null) return null;
		
		String msg = message;
		for (int i = 0; i < replacements.length; i+=2) {
			msg = msg.replace(replacements[i], replacements[i+1]);
		}
		return msg;
	}
	
	/**
	 * Sends a message to a player, first making the specified replacements. This method also does color replacement and has multi-line functionality.
	 * @param player the player to send the message to
	 * @param message the message to send
	 * @param replacements the replacements to be made, in pairs
	 */
	static public void sendMessage(Player player, String message, String... replacements) {
		sendMessage(player, formatMessage(message, replacements));
	}
	
	/**
	 * Sends a message to a player. This method also does color replacement and has multi-line functionality.
	 * @param player the player to send the message to
	 * @param message the message to send
	 */
	static public void sendMessage(Player player, String message) {
		if (message != null && !message.equals("")) {
			String [] msgs = message.replaceAll("&([0-9a-fk-or])", "\u00A7$1").split("\n");
			for (String msg : msgs) {
				if (!msg.equals("")) {
					player.sendMessage(MagicSpells.textColor + msg);
				}
			}
		}
	}
	
	/**
	 * Writes a debug message to the console if the debug option is enabled.
	 * Uses debug level 2.
	 * @param message the message to write to the console
	 */
	public static void debug(String message) {
		debug(2, message);
	}
	
	/**
	 * Writes a debug message to the console if the debug option is enabled.
	 * @param level the debug level to log with
	 * @param message the message to write to the console
	 */
	public static void debug(int level, String message) {
		if (MagicSpells.debug && level <= debugLevel) {
			log(Level.INFO, message);
		}
	}
	
	public static void log(String message) {
		log(Level.INFO, message);
	}
	
	public static void error(String message) {
		log(Level.WARNING, message);
	}
	
	/**
	 * Writes an error message to the console.
	 * @param level the error level
	 * @param message the error message
	 */
	public static void log(Level level, String message) {
		plugin.getLogger().log(level, message);
	}
	
	/**
	 * Teaches a player a spell (adds it to their spellbook)
	 * @param player the player to teach
	 * @param spellName the spell name, either the in-game name or the internal name
	 * @return whether the spell was taught to the player
	 */
	public static boolean teachSpell(Player player, String spellName) {
		Spell spell = spellNames.get(spellName);
		if (spell == null) {
			spell = spells.get(spellName);
			if (spell == null) {
				return false;
			}
		}
		
		Spellbook spellbook = getSpellbook(player);
		
		if (spellbook == null || spellbook.hasSpell(spell) || !spellbook.canLearn(spell)) {
			return false;
		} else {
			// call event
			SpellLearnEvent event = new SpellLearnEvent(spell, player, LearnSource.OTHER, null);
			plugin.getServer().getPluginManager().callEvent(event);
			if (event.isCancelled()) {
				return false;
			} else {
				spellbook.addSpell(spell);
				spellbook.save();
				return true;
			}
		}
	}
	
	public void unload() {
		for (Spell spell : spells.values()) {
			spell.turnOff();
		}
		spells.clear();
		spells = null;
		spellNames.clear();
		spellNames = null;
		spellbooks.clear();
		spellbooks = null;
		if (mana != null) {
			mana.turnOff();
			mana = null;
		}
		if (buffManager != null) {
			buffManager.turnOff();
			buffManager = null;
		}
		getServer().getPluginManager().removePermission("magicspells.grant.*");
		getServer().getPluginManager().removePermission("magicspells.cast.*");
		getServer().getPluginManager().removePermission("magicspells.learn.*");
		getServer().getPluginManager().removePermission("magicspells.teach.*");
		HandlerList.unregisterAll(this);	
	}
	
	@Override
	public void onDisable() {		
		unload();
	}
	
}

package com.nisovin.magicspells.spells.command;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Scanner;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.nisovin.magicspells.CommandSpell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.events.SpellLearnEvent;
import com.nisovin.magicspells.events.SpellLearnEvent.LearnSource;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.MagicLocation;

public class SpellbookSpell extends CommandSpell {

	@SuppressWarnings("unused")
	private static final String SPELL_NAME = "spellbook";
	
	private int defaultUses;
	private boolean destroyBookcase;
	private Material spellbookBlock;
	private String strUsage;
	private String strNoSpell;
	private String strCantTeach;
	private String strNoTarget;
	private String strHasSpellbook;
	private String strCantDestroy;
	private String strLearnError;
	private String strCantLearn;
	private String strAlreadyKnown;
	private String strLearned;
	
	private ArrayList<MagicLocation> bookLocations;
	private ArrayList<String> bookSpells;
	private ArrayList<Integer> bookUses;
	
	public SpellbookSpell(MagicConfig config, String spellName) {
		super(config,spellName);
		
		defaultUses = config.getInt("spells." + spellName + ".default-uses", -1);
		destroyBookcase = config.getBoolean("spells." + spellName + ".destroy-when-used-up", false);
		spellbookBlock = Material.getMaterial(config.getInt("spell." + spellName + ".spellbook-block", Material.BOOKSHELF.getId()));
		strUsage = config.getString("spells." + spellName + ".str-usage", "Usage: /cast spellbook <spell> [uses]");
		strNoSpell = config.getString("spells." + spellName + ".str-no-spell", "You do not know a spell by that name.");
		strCantTeach = config.getString("spells." + spellName + ".str-cant-teach", "You can't create a spellbook with that spell.");
		strNoTarget = config.getString("spells." + spellName + ".str-no-target", "You must target a bookcase to create a spellbook.");
		strHasSpellbook = config.getString("spells." + spellName + ".str-has-spellbook", "That bookcase already has a spellbook.");
		strCantDestroy = config.getString("spells." + spellName + ".str-cant-destroy", "You cannot destroy a bookcase with a spellbook.");
		strLearnError = config.getString("spells." + spellName + ".str-learn-error", "");
		strCantLearn = config.getString("spells." + spellName + ".str-cant-learn", "You cannot learn the spell in this spellbook.");
		strAlreadyKnown = config.getString("spells." + spellName + ".str-already-known", "You already know the %s spell.");
		strLearned = config.getString("spells." + spellName + ".str-learned", "You have learned the %s spell!");
		
		bookLocations = new ArrayList<MagicLocation>();
		bookSpells = new ArrayList<String>();
		bookUses = new ArrayList<Integer>();
		
		addListener(Event.Type.PLAYER_INTERACT);
		addListener(Event.Type.BLOCK_BREAK);
		
		loadSpellbooks();
	}
	
	@Override
	protected PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			if (args == null || args.length < 1 || args.length > 2 || (args.length == 2 && !args[1].matches("^[0-9]+$"))) {
				// fail: show usage string
				sendMessage(player, strUsage);
			} else {
				// check for reload
				if (player.isOp() && args[0].equalsIgnoreCase("reload")) {
					bookLocations = new ArrayList<MagicLocation>();
					bookSpells = new ArrayList<String>();
					bookUses = new ArrayList<Integer>();
					loadSpellbooks();
					player.sendMessage("Spellbook file reloaded.");
					return PostCastAction.ALREADY_HANDLED;
				}
				Spellbook spellbook = MagicSpells.getSpellbook(player);
				Spell spell = MagicSpells.getSpellByInGameName(args[0]);
				if (spellbook == null || spell == null || !spellbook.hasSpell(spell)) {
					// fail: no such spell
					sendMessage(player, strNoSpell);
				} else if (!MagicSpells.getSpellbook(player).canTeach(spell)) {
					// fail: can't teach
					sendMessage(player, strCantTeach);
				} else {
					Block target = player.getTargetBlock(null, 10);
					if (target == null || target.getType() != spellbookBlock) {
						// fail: must target a bookcase
						sendMessage(player, strNoTarget);
					} else if (bookLocations.contains(target.getLocation())) {
						// fail: already a spellbook there
						sendMessage(player, strHasSpellbook);
					} else {
						// create spellbook
						bookLocations.add(new MagicLocation(target.getLocation()));
						bookSpells.add(spell.getInternalName());
						if (args.length == 1) {
							bookUses.add(defaultUses);
						} else {
							bookUses.add(Integer.parseInt(args[1]));
						}
						saveSpellbooks();
						sendMessage(player, formatMessage(strCastSelf, "%s", spell.getName()));
						return PostCastAction.NO_MESSAGES;
					}
				}
			}
			return PostCastAction.ALREADY_HANDLED;
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private void removeSpellbook(int index) {
		bookLocations.remove(index);
		bookSpells.remove(index);
		bookUses.remove(index);
		saveSpellbooks();
	}
	
	@Override
	public void onPlayerInteract(PlayerInteractEvent event) {
		if (event.hasBlock() && event.getClickedBlock().getType() == spellbookBlock) {
			MagicLocation loc = new MagicLocation(event.getClickedBlock().getLocation());
			if (bookLocations.contains(loc)) {
				Player player = event.getPlayer();
				int i = bookLocations.indexOf(loc);
				Spellbook spellbook = MagicSpells.getSpellbook(player);
				Spell spell = MagicSpells.getSpellByInternalName(bookSpells.get(i));
				if (spellbook == null || spell == null) {
					// fail: something's wrong
					sendMessage(player, strLearnError);
				} else if (!spellbook.canLearn(spell)) {
					// fail: can't learn
					sendMessage(player, formatMessage(strCantLearn, "%s", spell.getName()));
				} else if (spellbook.hasSpell(spell)) {
					// fail: already known
					sendMessage(player, formatMessage(strAlreadyKnown, "%s", spell.getName()));
				} else {
					// call learn event
					SpellLearnEvent learnEvent = new SpellLearnEvent(spell, player, LearnSource.SPELLBOOK, event.getClickedBlock());
					Bukkit.getPluginManager().callEvent(learnEvent);
					if (learnEvent.isCancelled()) {
						// fail: plugin cancelled it
						sendMessage(player, formatMessage(strCantLearn, "%s", spell.getName()));
					} else {
						// teach the spell
						spellbook.addSpell(spell);
						spellbook.save();
						sendMessage(player, formatMessage(strLearned, "%s", spell.getName()));
						int uses = bookUses.get(i);
						if (uses > 0) {
							uses--;
							if (uses == 0) {
								// remove the spellbook
								if (destroyBookcase) {
									bookLocations.get(i).getLocation().getBlock().setType(Material.AIR);
								}
								removeSpellbook(i);
							} else {
								bookUses.set(i, uses);
							}
						}						
					}
				}
			}
		}
	}
	
	@Override
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.getBlock().getType() == spellbookBlock) {
			MagicLocation loc = new MagicLocation(event.getBlock().getLocation());
			if (bookLocations.contains(loc)) {
				if (event.getPlayer().isOp()) {
					// remove the bookcase
					int i = bookLocations.indexOf(loc);
					removeSpellbook(i);
				} else {
					// cancel it
					event.setCancelled(true);
					sendMessage(event.getPlayer(), strCantDestroy);
				}
			}			
		}
	}
	
	@Override
	public boolean castFromConsole(CommandSender sender, String[] args) {
		if (sender.isOp() && args != null && args.length > 0 && args[0].equalsIgnoreCase("reload")) {
			bookLocations = new ArrayList<MagicLocation>();
			bookSpells = new ArrayList<String>();
			bookUses = new ArrayList<Integer>();
			loadSpellbooks();
			sender.sendMessage("Spellbook file reloaded.");
			return true;
		} else {
			return false;
		}
	}
	
	private void loadSpellbooks() {
		try {
			Scanner scanner = new Scanner(new File(MagicSpells.plugin.getDataFolder(), "books.txt"));
			while (scanner.hasNext()) {
				String line = scanner.nextLine();
				if (!line.equals("")) {
					try {
						String[] data = line.split(":");
						MagicLocation loc = new MagicLocation(data[0], Integer.parseInt(data[1]), Integer.parseInt(data[2]), Integer.parseInt(data[3]));
						int uses = Integer.parseInt(data[5]);
						bookLocations.add(loc);
						bookSpells.add(data[4]);
						bookUses.add(uses);
					} catch (Exception e) {
						MagicSpells.plugin.getServer().getLogger().severe("MagicSpells: Failed to load spellbook: " + line);
					}
				}
			}
		} catch (FileNotFoundException e) {
		} 
	}
	
	private void saveSpellbooks() {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(MagicSpells.plugin.getDataFolder(), "books.txt"), false));
			MagicLocation loc;
			for (int i = 0; i < bookLocations.size(); i++) {
				loc = bookLocations.get(i);
				writer.write(loc.getWorld() + ":" + (int)loc.getX() + ":" + (int)loc.getY() + ":" + (int)loc.getZ() + ":");
				writer.write(bookSpells.get(i) + ":" + bookUses.get(i));
				writer.newLine();
			}
			writer.close();
		} catch (Exception e) {
			MagicSpells.plugin.getServer().getLogger().severe("MagicSpells: Error saving spellbooks");
		}
	}
	
}
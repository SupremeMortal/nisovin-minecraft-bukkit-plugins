package com.nisovin.MagicSpells.Spells;

import org.bukkit.entity.Player;
import org.bukkit.util.config.Configuration;

import com.nisovin.MagicSpells.CommandSpell;
import com.nisovin.MagicSpells.MagicSpells;
import com.nisovin.MagicSpells.Spell;
import com.nisovin.MagicSpells.Spellbook;

public class ListSpell extends CommandSpell {
	
	private static final String SPELL_NAME = "list";
	
	private String strNoSpells;
	private String strPrefix;
	
	public static void load(Configuration config) {
		load(config, SPELL_NAME);
	}
	
	public static void load(Configuration config, String spellName) {
		if (config.getBoolean("spells." + spellName + ".enabled", true)) {
			MagicSpells.spells.put(spellName, new ListSpell(config, spellName));
		}
	}

	public ListSpell(Configuration config, String spellName) {
		super(config, spellName);
		
		strNoSpells = config.getString("spells." + spellName + ".str-no-spells", "You do not know any spells.");
		strPrefix = config.getString("spells." + spellName + ".str-prefix", "Known spells:");
	}

	@Override
	protected boolean castSpell(Player player, SpellCastState state, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Spellbook spellbook = MagicSpells.spellbooks.get(player.getName());
			if (spellbook == null || spellbook.getSpells().size() == 0) {
				// no spells
				sendMessage(player, strNoSpells);
			} else {
				String s = "";
				for (Spell spell : spellbook.getSpells()) {
					if (s.equals("")) {
						s = spell.getName();
					} else {
						s += ", " + spell.getName();
					}
				}
				sendMessage(player, strPrefix + " " + s);
			}
			return true;
		}		
		return false;
	}

}

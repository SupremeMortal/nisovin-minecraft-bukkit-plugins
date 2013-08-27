package com.nisovin.magicspells.spells;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.Spellbook;
import com.nisovin.magicspells.util.MagicConfig;

public class BowSpell extends Spell {

	private static BowSpellHandler handler;
	
	String bowName;
	String spellNameOnShoot;
	Spell spellOnShoot;
	
	public BowSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		bowName = ChatColor.translateAlternateColorCodes('&', getConfigString("bow-name", null));
		spellNameOnShoot = getConfigString("spell", null);
	}
	
	@Override
	public void initialize() {
		super.initialize();
		
		if (spellNameOnShoot != null && !spellNameOnShoot.isEmpty()) {
			spellOnShoot = MagicSpells.getSpellByInternalName(spellNameOnShoot);
			if (spellOnShoot == null) {
				MagicSpells.error("Bow spell '" + internalName + "' has invalid spell defined: '" + spellNameOnShoot + "'");
			}
		}
		
		if (handler == null) {
			handler = new BowSpellHandler();
		}
		handler.registerSpell(this);
	}
	
	@Override
	public void turnOff() {
		super.turnOff();
		if (handler != null) {
			handler.turnOff();
			handler = null;
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		return PostCastAction.ALREADY_HANDLED;
	}

	@Override
	public boolean canCastWithItem() {
		return false;
	}

	@Override
	public boolean canCastByCommand() {
		return false;
	}
	
	class BowSpellHandler implements Listener {
		
		Map<String, BowSpell> spells = new HashMap<String, BowSpell>();
		
		public BowSpellHandler() {
			registerEvents(this);
		}
		
		public void registerSpell(BowSpell spell) {
			spells.put(spell.bowName, spell);
		}
		
		@EventHandler
		public void onArrowLaunch(EntityShootBowEvent event) {
			if (event.getEntity().getType() != EntityType.PLAYER) return;
			Player shooter = (Player)event.getEntity();
			ItemStack inHand = shooter.getItemInHand();
			if (inHand == null || inHand.getType() != Material.BOW) return;
			String bowName = inHand.getItemMeta().getDisplayName();
			if (bowName != null && !bowName.isEmpty()) {
				Spellbook spellbook = MagicSpells.getSpellbook(shooter);
				BowSpell spell = spells.get(bowName);
				if (spell != null && spellbook.hasSpell(spell) && spellbook.canCast(spell)) {
					event.setCancelled(true);
					event.getProjectile().remove();
					spell.spellOnShoot.cast(shooter, event.getForce(), null);
				}
			}
		}
		
		public void turnOff() {
			unregisterEvents(this);
			spells.clear();
		}
		
	}
}

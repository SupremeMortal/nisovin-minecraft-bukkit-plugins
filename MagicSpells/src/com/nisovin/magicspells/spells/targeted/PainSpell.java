package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Bukkit;
import org.bukkit.EntityEffect;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class PainSpell extends TargetedSpell implements TargetedEntitySpell {

	private int damage;
	private boolean ignoreArmor;
	private boolean obeyLos;
	private boolean targetPlayers;
	private boolean checkPlugins;
	
	public PainSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		damage = getConfigInt("damage", 4);
		ignoreArmor = getConfigBoolean("ignore-armor", false);
		obeyLos = getConfigBoolean("obey-los", true);
		targetPlayers = getConfigBoolean("target-players", false);
		checkPlugins = getConfigBoolean("check-plugins", true);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			LivingEntity target = getTargetedEntity(player, minRange, range, targetPlayers, obeyLos);
			if (target == null) {
				// fail -- no target
				return noTarget(player);
			} else {
				boolean done = causePain(player, target, power);
				if (!done) {
					return noTarget(player);
				} else {
					sendMessages(player, target);
					return PostCastAction.NO_MESSAGES;
				}
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private boolean causePain(Player player, LivingEntity target, float power) {
		if (target.isDead()) return false;
		int dam = Math.round(damage*power);
		EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(player, target, DamageCause.ENTITY_ATTACK, dam);
		if (target instanceof Player && checkPlugins) {
			// handle the event myself so I can detect cancellation properly
			Bukkit.getServer().getPluginManager().callEvent(event);
			if (event.isCancelled()) {
				return false;
			}
			dam = event.getDamage();
			target.setLastDamageCause(event);
		}
		if (ignoreArmor) {
			int health = target.getHealth() - dam;
			if (health < 0) health = 0;
			if (health == 0) {
				target.setLastDamageCause(event);
			}
			target.setHealth(health);
			target.playEffect(EntityEffect.HURT);
		} else {
			target.damage(dam, player);
		}
		playSpellEffects(player, target);
		return true;
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (target instanceof Player && !targetPlayers) {
			return false;
		} else {
			return causePain(caster, target, power);
		}
	}

}

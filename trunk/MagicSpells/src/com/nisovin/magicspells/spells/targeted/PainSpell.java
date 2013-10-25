package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Bukkit;
import org.bukkit.EntityEffect;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.util.MagicConfig;

public class PainSpell extends TargetedSpell implements TargetedEntitySpell {

	private double damage;
	private boolean ignoreArmor;
	private boolean checkPlugins;
	
	public PainSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		damage = getConfigFloat("damage", 4);
		ignoreArmor = getConfigBoolean("ignore-armor", false);
		checkPlugins = getConfigBoolean("check-plugins", true);
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			LivingEntity target = getTargetedEntity(player);
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
		double dam = damage * power;
		if (target instanceof Player && checkPlugins && player != null) {
			// handle the event myself so I can detect cancellation properly
			EntityDamageByEntityEvent event = new EntityDamageByEntityEvent(player, target, DamageCause.ENTITY_ATTACK, dam);
			Bukkit.getServer().getPluginManager().callEvent(event);
			if (event.isCancelled()) {
				return false;
			}
			dam = event.getDamage();
			target.setLastDamageCause(event);
		}
		if (ignoreArmor) {
			double health = target.getHealth() - dam;
			if (health < 0) health = 0;
			if (health == 0 && player != null) {
				MagicSpells.getVolatileCodeHandler().setKiller(target, player);
			}
			target.setHealth(health);
			target.playEffect(EntityEffect.HURT);
		} else {
			target.damage(dam, player);
		}
		if (player != null) {
			playSpellEffects(player, target);
		} else {
			playSpellEffects(EffectPosition.TARGET, target);
		}
		return true;
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (!validTargetList.canTarget(caster, target)) {
			return false;
		} else {
			return causePain(caster, target, power);
		}
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		if (!validTargetList.canTarget(target)) {
			return false;
		} else {
			return causePain(null, target, power);
		}
	}

}

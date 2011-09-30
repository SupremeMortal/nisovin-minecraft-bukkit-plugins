package com.nisovin.magicspells.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;

import com.nisovin.magicspells.Spell;

@SuppressWarnings("serial")
public class SpellLearnEvent extends Event implements Cancellable {

	private Spell spell;
	private Player learner;
	private LearnSource source;
	private Object teacher;
	private boolean cancelled;
	
	public SpellLearnEvent(Spell spell, Player learner, LearnSource source, Object teacher) {
		super("SpellLearn");
		
		this.spell = spell;
		this.learner = learner;
		this.source = source;
		this.teacher = teacher;
		this.cancelled = false;
	}

	/**
	 * Gets the spell that is going to be learned
	 * @return the learned spell
	 */
	public Spell getSpell() {
		return spell;
	}
	
	/**
	 * Gets the player that will be learning the spell
	 * @return the learning player
	 */
	public Player getLearner() {
		return learner;
	}
	
	/**
	 * Gets the source of the learning (teach, spellbook, tome, other)
	 * @return the source
	 */
	public LearnSource getSource() {
		return source;
	}
	
	/**
	 * Gets the object that is teaching the spell
	 * @return the player/console for teach, the block for spellbook, or the book item for tome, or null
	 */
	public Object getTeacher() {
		return teacher;
	}
	
	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}
	
	public enum LearnSource {
		TEACH,
		SPELLBOOK,
		TOME,
		OTHER
	}

}

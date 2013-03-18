package com.nisovin.magicspells.spells;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.ConversationFactory;
import org.bukkit.conversations.Prompt;
import org.bukkit.conversations.StringPrompt;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.util.MagicConfig;

public class ExternalCommandSpell extends TargetedEntitySpell {
	
	@SuppressWarnings("unused")
	private static final String SPELL_NAME = "external";

	private boolean castWithItem;
	private boolean castByCommand;
	private List<String> commandToExecute;
	private List<String> commandToExecuteLater;
	private int commandDelay;
	private List<String> commandToBlock;
	private List<String> temporaryPermissions;
	private boolean temporaryOp;
	private boolean requirePlayerTarget;
	private boolean blockChatOutput;
	private boolean beneficial;
	private boolean executeAsTargetInstead;
	private boolean executeOnConsoleInstead;
	private boolean obeyLos;
	private String strCantUseCommand;
	private String strNoTarget;
	private String strBlockedOutput;
	
	private ConversationFactory convoFac;
	private Prompt convoPrompt;

	public ExternalCommandSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		castWithItem = getConfigBoolean("can-cast-with-item", true);
		castByCommand = getConfigBoolean("can-cast-by-command", true);
		commandToExecute = getConfigStringList("command-to-execute", null);
		commandToExecuteLater = getConfigStringList("command-to-execute-later", null);
		commandDelay = getConfigInt("command-delay", 0);
		commandToBlock = getConfigStringList("command-to-block", null);
		temporaryPermissions = getConfigStringList("temporary-permissions", null);
		temporaryOp = getConfigBoolean("temporary-op", false);
		requirePlayerTarget = getConfigBoolean("require-player-target", false);
		blockChatOutput = getConfigBoolean("block-chat-output", false);
		beneficial = getConfigBoolean("beneficial", false);
		executeAsTargetInstead = getConfigBoolean("execute-as-target-instead", false);
		executeOnConsoleInstead = getConfigBoolean("execute-on-console-instead", false);
		obeyLos = getConfigBoolean("obey-los", true);
		strCantUseCommand = getConfigString("str-cant-use-command", "&4You don't have permission to do that.");
		strNoTarget = getConfigString("str-no-target", "No target found.");
		strBlockedOutput = getConfigString("str-blocked-output", "");
		
		if (blockChatOutput) {
			convoPrompt = new StringPrompt() {	
				public String getPromptText(ConversationContext arg0) {
					return strBlockedOutput;
				}
				public Prompt acceptInput(ConversationContext arg0, String arg1) {
					return Prompt.END_OF_CONVERSATION;
				}
			};
			convoFac = new ConversationFactory(MagicSpells.plugin).withModality(true).withTimeout(1);
		}
	}

	@Override
	public PostCastAction castSpell(Player player, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			// get target if necessary
			Player target = null;
			if (requirePlayerTarget) {
				target = getTargetedPlayer(player, range, obeyLos);
				if (target == null) {
					sendMessage(player, strNoTarget);
					return PostCastAction.ALREADY_HANDLED;
				}
			}
			process(player, target, args);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	private void process(Player player, Player target, String[] args) {
		// grant permissions
		if (temporaryPermissions != null) {
			for (String perm : temporaryPermissions) {
				if (!executeAsTargetInstead) {
					if (!player.hasPermission(perm)) {
						player.addAttachment(MagicSpells.plugin, perm.trim(), true, 5);
					}
				} else {
					if (!target.hasPermission(perm)) {
						target.addAttachment(MagicSpells.plugin, perm.trim(), true, 5);
					}
				}
			}
		}
		// temp op
		boolean opped = false;
		if (temporaryOp && !player.isOp()) {
			opped = true;
			player.setOp(true);
		}
		// perform commands
		try {
			if (commandToExecute != null && commandToExecute.size() > 0) {
				for (String comm : commandToExecute) {
					if (args != null && args.length > 0) {
						for (int i = 0; i < args.length; i++) {
							comm = comm.replace("%"+(i+1), args[i]);
						}
					}
					comm = comm.replace("%a", player.getName());
					if (target != null) {
						comm = comm.replace("%t", target.getName());
					}
					if (executeAsTargetInstead) {
						Conversation convo = null;
						if (blockChatOutput) {
							convo = convoFac.buildConversation(target);
							convo.begin();
						}
						target.performCommand(comm);
						if (convo != null) {
							convo.abandon();
						}
					} else if (executeOnConsoleInstead) {
						Bukkit.dispatchCommand(Bukkit.getConsoleSender(), comm);
					} else {
						Conversation convo = null;
						if (blockChatOutput) {
							convo = convoFac.buildConversation(player);
							convo.begin();
						}
						player.performCommand(comm);
						if (convo != null) {
							convo.abandon();
						}
					}
				}
			}
		} catch (Exception e) {
			// catch all exceptions to make sure we don't leave someone opped
			e.printStackTrace();
		}
		// deop
		if (opped) {
			player.setOp(false);
		}
		// effects
		if (target != null) {
			playSpellEffects(player, target);
		} else {
			playSpellEffects(EffectPosition.CASTER, player);
		}
		// add delayed command
		if (commandToExecuteLater != null && commandToExecuteLater.size() > 0 && !commandToExecuteLater.get(0).isEmpty()) {
			Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new DelayedCommand(player, target), commandDelay);
		}
	}

	@Override
	public boolean castAtEntity(Player caster, LivingEntity target, float power) {
		if (requirePlayerTarget && target instanceof Player) {
			process(caster, (Player)target, null);
			return true;
		} else {
			return false;
		}
	}
	
	@EventHandler
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		if (!event.getPlayer().isOp() && commandToBlock != null && commandToBlock.size() > 0) {
			String msg = event.getMessage();
			for (String comm : commandToBlock) {
				comm = comm.trim();
				if (!comm.equals("") && msg.startsWith("/" + commandToBlock)) {
					event.setCancelled(true);
					sendMessage(event.getPlayer(), strCantUseCommand);
					return;
				}
			}
		}
	}

	@Override
	public boolean canCastByCommand() {
		return castByCommand;
	}

	@Override
	public boolean canCastWithItem() {
		return castWithItem;
	}
	
	@Override
	public boolean isBeneficial() {
		return beneficial;
	}
	
	private class DelayedCommand implements Runnable {

		private Player player;
		private Player target;
		
		public DelayedCommand(Player player, Player target) {
			this.player = player;
			this.target = target;
		}
		
		@Override
		public void run() {
			// grant permissions
			if (temporaryPermissions != null) {
				for (String perm : temporaryPermissions) {
					if (!executeAsTargetInstead) {
						if (!player.hasPermission(perm)) {
							player.addAttachment(MagicSpells.plugin, perm, true, 5);
						}
					} else {
						if (!target.hasPermission(perm)) {
							target.addAttachment(MagicSpells.plugin, perm, true, 5);
						}
					}
				}
			}
			// temporary op
			boolean opped = false;
			if (temporaryOp && !player.isOp()) {
				opped = true;
				player.setOp(true);
			}
			// run commands
			try {
				for (String comm : commandToExecuteLater) {
					if (comm != null && !comm.isEmpty()) {
						comm = comm.replace("%a", player.getName());
						if (target != null) {
							comm = comm.replace("%t", target.getName());
						}
						if (executeAsTargetInstead) {
							Conversation convo = null;
							if (blockChatOutput) {
								convo = convoFac.buildConversation(target);
								convo.begin();
							}
							target.performCommand(comm);
							if (convo != null) {
								convo.abandon();
							}
						} else if (executeOnConsoleInstead) {
							Bukkit.dispatchCommand(Bukkit.getConsoleSender(), comm);
						} else {
							Conversation convo = null;
							if (blockChatOutput) {
								convo = convoFac.buildConversation(player);
								convo.begin();
							}
							player.performCommand(comm);
							if (convo != null) {
								convo.abandon();
							}
						}
					}
				}
			} catch (Exception e) {
				// catch exceptions to make sure we don't leave someone opped
				e.printStackTrace();
			}
			// deop
			if (opped) {
				player.setOp(false);
			}
			// graphical effect
			playSpellEffects(EffectPosition.DISABLED, player);
		}
		
	}

}

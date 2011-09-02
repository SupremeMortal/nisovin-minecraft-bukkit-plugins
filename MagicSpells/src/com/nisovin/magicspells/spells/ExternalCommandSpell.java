package com.nisovin.magicspells.spells;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.util.config.Configuration;

import com.nisovin.magicspells.InstantSpell;
import com.nisovin.magicspells.MagicSpells;

public class ExternalCommandSpell extends InstantSpell {
	
	@SuppressWarnings("unused")
	private static final String SPELL_NAME = "external";

	private boolean castWithItem;
	private boolean castByCommand;
	private List<String> commandToExecute;
	private List<String> commandToExecuteLater;
	private int commandDelay;
	private List<String> commandToBlock;
	private List<String> temporaryPermissions;
	private boolean requirePlayerTarget;
	private boolean obeyLos;
	private String strCantUseCommand;
	private String strNoTarget;

	public ExternalCommandSpell(Configuration config, String spellName) {
		super(config, spellName);
		
		addListener(Event.Type.PLAYER_COMMAND_PREPROCESS);
		
		castWithItem = config.getBoolean("spells." + spellName + ".can-cast-with-item", true);
		castByCommand = config.getBoolean("spells." + spellName + ".can-cast-by-command", true);
		commandToExecute = getConfigStringList("command-to-execute", null);
		commandToExecuteLater = getConfigStringList("command-to-execute-later", null);
		commandDelay = getConfigInt("command-delay", 0);
		commandToBlock = getConfigStringList("command-to-block", null);
		temporaryPermissions = getConfigStringList("temporary-permissions", null);
		requirePlayerTarget = getConfigBoolean("require-player-target", false);
		obeyLos = getConfigBoolean("obey-los", true);
		strCantUseCommand = config.getString("spells." + spellName + ".str-cant-use-command", "&4You don't have permission to do that.");
		strNoTarget = getConfigString("str-no-target", "No target found.");
	}

	@Override
	protected PostCastAction castSpell(Player player, SpellCastState state, String[] args) {
		if (commandToExecute.equals("")) {
			Bukkit.getServer().getLogger().severe("MagicSpells: External command spell '" + name + "' has no command to execute.");
			return PostCastAction.ALREADY_HANDLED;
		} else if (state == SpellCastState.NORMAL) {
			// get target if necessary
			Player target = null;
			if (requirePlayerTarget) {
				target = getTargetedPlayer(player, range, obeyLos);
				if (target == null) {
					sendMessage(player, strNoTarget);
					return PostCastAction.ALREADY_HANDLED;
				}
			}
			// grant permissions
			if (temporaryPermissions != null) {
				for (String perm : temporaryPermissions) {
					if (!player.hasPermission(perm)) {
						player.addAttachment(MagicSpells.plugin, perm, true, 5);
					}
				}
			}
			// perform commands
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
				player.performCommand(comm);
			}
			if (commandToExecuteLater != null && commandToExecuteLater.size() > 0 && !commandToExecuteLater.get(0).isEmpty()) {
				Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new DelayedCommand(player, target), commandDelay);
			}
		}
		return PostCastAction.HANDLE_NORMALLY;
	}
	
	@Override
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
	
	private class DelayedCommand implements Runnable {

		private Player player;
		private Player target;
		
		public DelayedCommand(Player player, Player target) {
			this.player = player;
			this.target = target;
		}
		
		@Override
		public void run() {
			for (String comm : commandToExecuteLater) {
				if (comm != null && !comm.isEmpty()) {
					comm = comm.replace("%a", player.getName());
					if (target != null) {
						comm = comm.replace("%t", target.getName());
					}
					player.performCommand(comm);
				}
			}			
		}
		
	}

}

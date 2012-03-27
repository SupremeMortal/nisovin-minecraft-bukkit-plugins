package com.nisovin.yapp.menu;

import java.util.List;

import org.bukkit.conversations.Conversable;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.conversations.Prompt;

import com.nisovin.yapp.Group;
import com.nisovin.yapp.MainPlugin;
import com.nisovin.yapp.PermissionContainer;

public class SetPrimaryGroup extends MenuPrompt {

	@Override
	public String getPromptText(ConversationContext context) {
		PermissionContainer obj = getObject(context);
		String world = getWorld(context);
		String type = getType(context);
		List<Group> groups = obj.getActualGroupList(world);
		
		Conversable c = context.getForWhom();
		if (groups == null) {
			c.sendRawMessage(Menu.TEXT_COLOR + "The " + type + " " + Menu.HIGHLIGHT_COLOR + obj.getName() + Menu.TEXT_COLOR + " has no groups defined" + (world != null ? " on world " + Menu.HIGHLIGHT_COLOR + world + Menu.TEXT_COLOR : ""));
			return Menu.TEXT_COLOR + "Please type " + Menu.KEYLETTER_COLOR + "<" + Menu.TEXT_COLOR + " to return to the previous menu";
		} else {
			c.sendRawMessage(Menu.TEXT_COLOR + "The " + type + " " + Menu.HIGHLIGHT_COLOR + obj.getName() + Menu.TEXT_COLOR + " inherits the following groups" + (world != null ? " on world " + Menu.HIGHLIGHT_COLOR + world + Menu.TEXT_COLOR : "") + ":");
			String s = "";
			for (Group g : groups) {
				s += Menu.HIGHLIGHT_COLOR + g.getName();
				if (s.length() > 40) {
					c.sendRawMessage("   " + s);
					s = "";
				} else {
					s += Menu.TEXT_COLOR + ", ";
				}
			}
			if (s.length() > 0) {
				c.sendRawMessage("   " + s);
			}
			c.sendRawMessage(Menu.TEXT_COLOR + "The primary group is " + Menu.HIGHLIGHT_COLOR + groups.get(0).getName());
			return Menu.TEXT_COLOR + "Please type the new primary group: ";
		}
	}

	@Override
	public Prompt accept(ConversationContext context, String input) {
		Group group = MainPlugin.getGroup(input);
		if (group == null) {
			return showMessage(context, Menu.ERROR_COLOR + "That group does not exist", this);
		} else {
			PermissionContainer obj = getObject(context);
			String world = getWorld(context);
			String type = getType(context);
			boolean ok = obj.setPrimaryGroup(world, group);
			if (!ok) {
				return showMessage(context, Menu.ERROR_COLOR + "Cannot set the primary group to that group", this);
			} else {
				return showMessage(context, Menu.TEXT_COLOR + "The primary group for " + type + " " + Menu.HIGHLIGHT_COLOR + obj.getName() + Menu.TEXT_COLOR + " has been set to " + Menu.HIGHLIGHT_COLOR + group.getName(), Menu.MODIFY_OPTIONS);
			}
		}
	}

	@Override
	public Prompt getPreviousPrompt(ConversationContext context) {
		return Menu.MODIFY_OPTIONS;
	}

}

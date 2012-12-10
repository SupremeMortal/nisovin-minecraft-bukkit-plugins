package com.nisovin.magicspells;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

import net.minecraft.server.ChunkCoordIntPair;
import net.minecraft.server.DataWatcher;
import net.minecraft.server.EntityCreature;
import net.minecraft.server.EntityFallingBlock;
import net.minecraft.server.EntityLiving;
import net.minecraft.server.EntityPlayer;
import net.minecraft.server.EntitySmallFireball;
import net.minecraft.server.EntityTNTPrimed;
import net.minecraft.server.MobEffect;
import net.minecraft.server.NBTTagCompound;
import net.minecraft.server.NBTTagList;
import net.minecraft.server.NBTTagString;
import net.minecraft.server.Packet103SetSlot;
import net.minecraft.server.Packet42RemoveMobEffect;
import net.minecraft.server.Packet43SetExperience;
import net.minecraft.server.Packet62NamedSoundEffect;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftCreature;
import org.bukkit.craftbukkit.entity.CraftFallingSand;
import org.bukkit.craftbukkit.entity.CraftLivingEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.entity.CraftTNTPrimed;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Creature;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;


class VolatileCodeEnabled implements VolatileCodeHandle {

	@Override
	public void addPotionGraphicalEffect(LivingEntity entity, int color, int duration) {
		final EntityLiving el = ((CraftLivingEntity)entity).getHandle();
		final DataWatcher dw = el.getDataWatcher();
		dw.watch(8, Integer.valueOf(color));
		
		Bukkit.getScheduler().scheduleSyncDelayedTask(MagicSpells.plugin, new Runnable() {
			public void run() {
				int c = 0;
				if (!el.effects.isEmpty()) {
					c = net.minecraft.server.PotionBrewer.a(el.effects.values());
				}
				dw.watch(8, Integer.valueOf(c));
			}
		}, duration);
	}

	@Override
	public void entityPathTo(LivingEntity creature, LivingEntity target) {
		EntityCreature entity = ((CraftCreature)creature).getHandle();
		entity.pathEntity = entity.world.findPath(entity, ((CraftLivingEntity)target).getHandle(), 16.0F, true, false, false, false);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void queueChunksForUpdate(Player player, Set<Chunk> chunks) {
		for (Chunk chunk : chunks) {
			ChunkCoordIntPair intPair = new ChunkCoordIntPair(chunk.getX(), chunk.getZ());
			((CraftPlayer)player).getHandle().chunkCoordIntPairQueue.add(intPair);
		}
	}

	@Override
	public void sendFakeSlotUpdate(Player player, int slot, ItemStack item) {
		net.minecraft.server.ItemStack nmsItem;
		if (item != null) {
			nmsItem = CraftItemStack.createNMSItemStack(item);
		} else {
			nmsItem = null;
		}
		Packet103SetSlot packet = new Packet103SetSlot(0, (short)slot+36, nmsItem);
		((CraftPlayer)player).getHandle().netServerHandler.sendPacket(packet);
	}

	@Override
	public void stackByData(int itemId, String var) {
		try {
			boolean ok = false;
			try {
				// attempt to make books with different data values stack separately
				Method method = net.minecraft.server.Item.class.getDeclaredMethod(var, boolean.class);
				if (method.getReturnType() == net.minecraft.server.Item.class) {
					method.setAccessible(true);
					method.invoke(net.minecraft.server.Item.byId[itemId], true);
					ok = true;
				}
			} catch (Exception e) {
			}
			if (!ok) {
				// otherwise limit stack size to 1
				Field field = net.minecraft.server.Item.class.getDeclaredField("maxStackSize");
				field.setAccessible(true);
				field.setInt(net.minecraft.server.Item.byId[itemId], 1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void toggleLeverOrButton(Block block) {
		net.minecraft.server.Block.byId[block.getType().getId()].interact(((CraftWorld)block.getWorld()).getHandle(), block.getX(), block.getY(), block.getZ(), null, 0, 0, 0, 0);
	}

	@Override
	public void pressPressurePlate(Block block) {
		block.setData((byte) (block.getData() ^ 0x1));
		net.minecraft.server.World w = ((CraftWorld)block.getWorld()).getHandle();
		w.applyPhysics(block.getX(), block.getY(), block.getZ(), block.getType().getId());
		w.applyPhysics(block.getX(), block.getY()-1, block.getZ(), block.getType().getId());
	}

	@Override
	public void removeMobEffect(LivingEntity entity, PotionEffectType type) {
        try {
            // remove effect
    		entity.removePotionEffect(type);
            // alert player that effect is gone
            if (entity instanceof Player) {
                    EntityPlayer player = ((CraftPlayer)entity).getHandle();
                    player.netServerHandler.sendPacket(new Packet42RemoveMobEffect(player.id, new MobEffect(type.getId(), 0, 0)));
            }
            // remove graphical effect
            ((CraftLivingEntity)entity).getHandle().getDataWatcher().watch(8, Integer.valueOf(0));
        } catch (Exception e) {
            e.printStackTrace();
        }
	}

	@Override
	public boolean simulateTnt(Location target, float explosionSize, boolean fire) {
        EntityTNTPrimed e = new EntityTNTPrimed(((CraftWorld)target.getWorld()).getHandle(), target.getX(), target.getY(), target.getZ());
        CraftTNTPrimed c = new CraftTNTPrimed((CraftServer)Bukkit.getServer(), e);
        ExplosionPrimeEvent event = new ExplosionPrimeEvent(c, explosionSize, fire);
        Bukkit.getServer().getPluginManager().callEvent(event);
        return event.isCancelled();
	}

	@Override
	public boolean createExplosionByPlayer(Player player, Location location, float size, boolean fire, boolean breakBlocks) {
		return !((CraftWorld)location.getWorld()).getHandle().createExplosion(((CraftPlayer)player).getHandle(), location.getX(), location.getY(), location.getZ(), size, fire, breakBlocks).wasCanceled;
	}

	@Override
	public void setExperienceBar(Player player, int level, float percent) {
		Packet43SetExperience packet = new Packet43SetExperience(percent, player.getTotalExperience(), level);
		((CraftPlayer)player).getHandle().netServerHandler.sendPacket(packet);
	}

	@Override
	public Fireball shootSmallFireball(Player player) {
		net.minecraft.server.World w = ((CraftWorld)player.getWorld()).getHandle();
		Location playerLoc = player.getLocation();
		Vector loc = player.getEyeLocation().toVector().add(player.getLocation().getDirection().multiply(10));
		
		double d0 = loc.getX() - playerLoc.getX();
        double d1 = loc.getY() - (playerLoc.getY() + 1.5);
        double d2 = loc.getZ() - playerLoc.getZ();
		EntitySmallFireball entitysmallfireball = new EntitySmallFireball(w, ((CraftPlayer)player).getHandle(), d0, d1, d2);

        entitysmallfireball.locY = playerLoc.getY() + 1.5;
        w.addEntity(entitysmallfireball);
        
        return (Fireball)entitysmallfireball.getBukkitEntity();
	}

	@Override
	public void setTarget(LivingEntity entity, LivingEntity target) {
		if (entity instanceof Creature) {
			((Creature)entity).setTarget(target);
		}
		((CraftLivingEntity)entity).getHandle().b(((CraftLivingEntity)target).getHandle());
	}

	@Override
	public ItemStack setStringOnItemStack(ItemStack item, String key, String value) {
		if (!(item instanceof CraftItemStack)) item = new CraftItemStack(item);
		NBTTagCompound tag = ((CraftItemStack)item).getHandle().tag;
		if (tag == null) {
			tag = new NBTTagCompound();
			((CraftItemStack)item).getHandle().tag = tag;
		}
		tag.setString(key, value);
		return item;
	}

	@Override
	public String getStringOnItemStack(ItemStack item, String key) {
		NBTTagCompound tag = ((CraftItemStack)item).getHandle().tag;
		if (tag != null) {
			if (tag.hasKey(key)) {
				return tag.getString(key);
			}
		}
		return null;
	}

	@Override
	public void removeStringOnItemStack(ItemStack item, String key) {
		NBTTagCompound tag = ((CraftItemStack)item).getHandle().tag;
		if (tag != null) {
			tag.remove(key);
		}
	}

	@Override
	public void playSound(Location location, String sound, float volume, float pitch) {
		((CraftWorld)location.getWorld()).getHandle().makeSound(location.getX(), location.getY(), location.getZ(), sound, volume, pitch);
	}

	@Override
	public void playSound(Player player, String sound, float volume, float pitch) {
		Location loc = player.getLocation();
		Packet62NamedSoundEffect packet = new Packet62NamedSoundEffect(sound, loc.getX(), loc.getY(), loc.getZ(), volume, pitch);
		((CraftPlayer)player).getHandle().netServerHandler.sendPacket(packet);
	}

	@Override
	public String getItemName(ItemStack item) {
		if (item instanceof CraftItemStack) {
			NBTTagCompound tag = ((CraftItemStack)item).getHandle().tag;
			if (tag != null) {
				NBTTagCompound disp = tag.getCompound("display");
				if (disp != null && disp.hasKey("Name")) {
					return disp.getString("Name");
				}
			}
		}
		return "";
	}

	@Override
	public ItemStack setItemName(ItemStack item, String name) {
		CraftItemStack craftItem;
		net.minecraft.server.ItemStack nmsItem;
		
		if (item instanceof CraftItemStack) {
			craftItem = (CraftItemStack)item;
			nmsItem = craftItem.getHandle();
		} else {
			craftItem = new CraftItemStack(item);
			nmsItem = craftItem.getHandle();
		}
		
		NBTTagCompound tag = nmsItem.tag;
		if (tag == null) {
			tag = new NBTTagCompound();
			nmsItem.tag = tag;
		}
		NBTTagCompound disp = tag.getCompound("display");
		if (disp == null) {
			disp = new NBTTagCompound("display");
		}
		disp.setString("Name", ChatColor.translateAlternateColorCodes('&', name));
		tag.setCompound("display", disp);
		
		return craftItem;
	}

	@Override
	public ItemStack setItemLore(ItemStack item, String... lore) {
		CraftItemStack craftItem;
		net.minecraft.server.ItemStack nmsItem;
		
		if (item instanceof CraftItemStack) {
			craftItem = (CraftItemStack)item;
			nmsItem = craftItem.getHandle();
		} else {
			craftItem = new CraftItemStack(item);
			nmsItem = craftItem.getHandle();
		}
		
		NBTTagCompound tag = nmsItem.tag;
		if (tag == null) {
			tag = new NBTTagCompound();
			nmsItem.tag = tag;
		}
		NBTTagCompound disp = tag.getCompound("display");
		if (disp == null) {
			disp = new NBTTagCompound("display");
		}
		NBTTagList list = new NBTTagList();
		for (String l : lore) {
			list.add(new NBTTagString("", ChatColor.translateAlternateColorCodes('&', l)));
		}
		disp.set("Lore", list);
		tag.setCompound("display", disp);
		
		return craftItem;
	}

	@Override
	public boolean itemStackTagsEqual(ItemStack item1, ItemStack item2) {
		NBTTagCompound tag1 = null, tag2 = null;
		if (item1 != null && item1 instanceof CraftItemStack) {
			tag1 = ((CraftItemStack)item1).getHandle().tag;
		}
		if (item2 != null && item2 instanceof CraftItemStack) {
			tag2 = ((CraftItemStack)item2).getHandle().tag;
		}
		if (tag1 == null && tag2 == null) return true;
		if (tag1 == null || tag2 == null) return false;
		return tag1.equals(tag2);
	}

	@Override
	public ItemStack addFakeEnchantment(ItemStack item) {
		CraftItemStack craftItem;
		net.minecraft.server.ItemStack nmsItem;
		
		if (item instanceof CraftItemStack) {
			craftItem = (CraftItemStack)item;
			nmsItem = craftItem.getHandle();
		} else {
			craftItem = new CraftItemStack(item);
			nmsItem = craftItem.getHandle();
		}
		
		NBTTagCompound tag = nmsItem.tag;
		if (tag == null) {
			tag = new NBTTagCompound();
			nmsItem.tag = tag;
		}
		if (!tag.hasKey("ench")) {
			tag.set("ench", new NBTTagList());
		}
		
		return craftItem;
	}

	@Override
	public ItemStack setArmorColor(ItemStack item, int color) {
		CraftItemStack craftItem;
		net.minecraft.server.ItemStack nmsItem;
		
		if (item instanceof CraftItemStack) {
			craftItem = (CraftItemStack)item;
			nmsItem = craftItem.getHandle();
		} else {
			craftItem = new CraftItemStack(item);
			nmsItem = craftItem.getHandle();
		}
		
		NBTTagCompound tag = nmsItem.tag;
		if (tag == null) {
			tag = new NBTTagCompound();
			nmsItem.tag = tag;
		}
		NBTTagCompound disp = tag.getCompound("display");
		if (disp == null) {
			disp = new NBTTagCompound("display");
		}
		disp.setInt("color", color);
		tag.setCompound("display", disp);
		
		return craftItem;
	}

	@Override
	public void setFallingBlockHurtEntities(FallingBlock block, float damage, int max) {
		EntityFallingBlock efb = ((CraftFallingSand)block).getHandle();
		try {
			Field field = EntityFallingBlock.class.getDeclaredField("hurtEntities");
			field.setAccessible(true);
			field.setBoolean(efb, true);
			
			field = EntityFallingBlock.class.getDeclaredField("fallHurtAmount");
			field.setAccessible(true);
			field.setFloat(efb, damage);
			
			field = EntityFallingBlock.class.getDeclaredField("fallHurtMax");
			field.setAccessible(true);
			field.setInt(efb, max);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void addPotionEffect(LivingEntity entity, PotionEffect effect, boolean ambient) {
		if (!ambient) {
			entity.addPotionEffect(effect, true);
		} else {
			if (entity.hasPotionEffect(effect.getType())) {
				entity.removePotionEffect(effect.getType());
			}
			((CraftLivingEntity)entity).getHandle().addEffect(new MobEffect(effect.getType().getId(), effect.getDuration(), effect.getAmplifier(), true));
		}
	}

}

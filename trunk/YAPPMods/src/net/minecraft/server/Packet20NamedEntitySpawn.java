package net.minecraft.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

public class Packet20NamedEntitySpawn extends Packet
{
	
	public static HashMap<String, String> yappMap = new HashMap<String, String>();
	
  public int a;
  public String b;
  public int c;
  public int d;
  public int e;
  public byte f;
  public byte g;
  public int h;
  private DataWatcher i;
  private List j;

  public Packet20NamedEntitySpawn()
  {
  }

  public Packet20NamedEntitySpawn(EntityHuman entityhuman)
  {
    this.a = entityhuman.id;

    String name = entityhuman.name;
    if (yappMap.containsKey(name)) {
    	name = yappMap.get(name);
    }
    if (name.length() > 16)
      this.b = name.substring(0, 16);
    else {
      this.b = name;
    }

    this.c = MathHelper.floor(entityhuman.locX * 32.0D);
    this.d = MathHelper.floor(entityhuman.locY * 32.0D);
    this.e = MathHelper.floor(entityhuman.locZ * 32.0D);
    this.f = (byte)(int)(entityhuman.yaw * 256.0F / 360.0F);
    this.g = (byte)(int)(entityhuman.pitch * 256.0F / 360.0F);
    ItemStack itemstack = entityhuman.inventory.getItemInHand();

    this.h = (itemstack == null ? 0 : itemstack.id);
    this.i = entityhuman.getDataWatcher();
  }

  public void a(DataInputStream datainputstream) throws IOException {
    this.a = datainputstream.readInt();
    this.b = a(datainputstream, 16);
    this.c = datainputstream.readInt();
    this.d = datainputstream.readInt();
    this.e = datainputstream.readInt();
    this.f = datainputstream.readByte();
    this.g = datainputstream.readByte();
    this.h = datainputstream.readShort();
    this.j = DataWatcher.a(datainputstream);
  }

  public void a(DataOutputStream dataoutputstream) throws IOException {
    dataoutputstream.writeInt(this.a);
    a(this.b, dataoutputstream);
    dataoutputstream.writeInt(this.c);
    dataoutputstream.writeInt(this.d);
    dataoutputstream.writeInt(this.e);
    dataoutputstream.writeByte(this.f);
    dataoutputstream.writeByte(this.g);
    dataoutputstream.writeShort(this.h);
    this.i.a(dataoutputstream);
  }

  public void handle(NetHandler nethandler) {
    nethandler.a(this);
  }

  public int a() {
    return 28;
  }
}
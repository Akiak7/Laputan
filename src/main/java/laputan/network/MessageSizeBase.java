	package laputan.network;

	import io.netty.buffer.ByteBuf;
	import net.minecraft.client.Minecraft;
	import net.minecraft.entity.Entity;
	import net.minecraft.entity.EntityLiving;
	import net.minecraft.nbt.NBTTagCompound;
	import net.minecraftforge.fml.common.network.simpleimpl.*;

	public class MessageSizeBase implements IMessage {
	    public int entityId;
	    public float baseW, baseH;

	    public MessageSizeBase() {}
	    public MessageSizeBase(int entityId, float baseW, float baseH) {
	        this.entityId = entityId;
	        this.baseW = baseW;
	        this.baseH = baseH;
	    }

	    @Override public void fromBytes(ByteBuf buf) {
	        entityId = buf.readInt();
	        baseW    = buf.readFloat();
	        baseH    = buf.readFloat();
	    }

	    @Override public void toBytes(ByteBuf buf) {
	        buf.writeInt(entityId);
	        buf.writeFloat(baseW);
	        buf.writeFloat(baseH);
	    }

	    public static class Handler implements IMessageHandler<MessageSizeBase, IMessage> {
	        @Override public IMessage onMessage(MessageSizeBase msg, MessageContext ctx) {
	            Minecraft mc = Minecraft.getMinecraft();
	            mc.addScheduledTask(() -> {
	                if (mc.world == null) return;
	                Entity e0 = mc.world.getEntityByID(msg.entityId);
	                if (!(e0 instanceof EntityLiving)) return;
	                EntityLiving e = (EntityLiving) e0;

	                // Stamp client-side tags and cache with the server’s canonical base
	                NBTTagCompound d = e.getEntityData();
	                d.setFloat("laputan_base_w", msg.baseW);
	                d.setFloat("laputan_base_h", msg.baseH);
	                d.setBoolean("laputan_base_child", e.isChild());

	                laputan.handlers.EntitySizeHandler.setCachedBase(e, msg.baseW, msg.baseH);

	                // Make sure current AABB matches base × last_scale immediately
	                float sLocal = d.hasKey("laputan_last_scale") ? d.getFloat("laputan_last_scale") : 1.0F;
	                float w = Math.max(0.001F, msg.baseW * sLocal);
	                float h = Math.max(0.001F, msg.baseH * sLocal);
	                laputan.handlers.EntitySizeHandler.setEntitySize(e, w, h);
	            });
	            return null;
	        }
	    }
	}

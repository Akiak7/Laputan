		package laputan.network;

		import io.netty.buffer.ByteBuf;
		import laputan.capabilities.ISizeCapability;
		import laputan.capabilities.SizeProvider;
		import net.minecraft.client.Minecraft;
		import net.minecraft.entity.Entity;
		import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
		import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
		import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
		import net.minecraft.nbt.NBTTagCompound;

		public class MessageSizeChange implements IMessage {

			public float scale = 1F;
			public int entityID = 0;

			public MessageSizeChange() {

			}

			public MessageSizeChange(float scale, int id) {
				this.scale = scale;
				this.entityID = id;
			}

			@Override
			public void fromBytes(ByteBuf buf) {
				this.scale = buf.readFloat();
				this.entityID = buf.readInt();
			}

			@Override
			public void toBytes(ByteBuf buf) {
				buf.writeFloat(scale);
				buf.writeInt(entityID);
			}

			public static class MessageHolder implements IMessageHandler<MessageSizeChange, IMessage> {

				@Override
				public IMessage onMessage(MessageSizeChange message, MessageContext ctx) {
					// In MessageSizeChange.onMessage (client thread):
					Minecraft mc = Minecraft.getMinecraft();  
					mc.addScheduledTask(() -> {
						Entity entity = mc.world.getEntityByID(message.entityID);
		if (entity instanceof net.minecraft.entity.EntityLiving && entity.hasCapability(SizeProvider.sizeCapability, null)) {
		    net.minecraft.entity.EntityLiving e = (net.minecraft.entity.EntityLiving) entity;
		    ISizeCapability cap = e.getCapability(SizeProvider.sizeCapability, null);
		    if (cap == null) return;

		    float now = message.scale <= 0F ? 1.0F : message.scale;

		    // store cap + last_scale; the tick code will apply the AABB using cached base
		    NBTTagCompound d = e.getEntityData();
		    cap.setScale(now);
		    d.setFloat("laputan_last_scale", now);
		}

					});
					return null;
				}

			}

		}

                        package laputan.handlers;

                        import net.minecraft.client.renderer.GlStateManager;
                        import net.minecraft.entity.EntityLivingBase;
                        import net.minecraftforge.client.event.RenderLivingEvent;
                        import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
                        import net.minecraftforge.fml.common.eventhandler.EventPriority;
                        import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
                        import net.minecraftforge.fml.relauncher.Side;
                        import net.minecraftforge.fml.relauncher.SideOnly;

                        import laputan.Laputan;
                        import laputan.util.EntitySizeUtil;

                        import net.minecraft.entity.EntityLiving;
                        import net.minecraft.nbt.NBTTagCompound;
                        //import laputan.capabilities.ISizeCapability;
                        //import laputan.capabilities.SizeProvider;


                        @SideOnly(Side.CLIENT)
                        @EventBusSubscriber(value = Side.CLIENT, modid = Laputan.MODID)
                        public class RenderEntityHandler {

                            private static final ThreadLocal<java.util.Set<Integer>> PUSHED = ThreadLocal.withInitial(java.util.HashSet::new);
    private static final ThreadLocal<java.util.Set<Integer>> NAME_PUSHED = ThreadLocal.withInitial(java.util.HashSet::new);

                            // Model render
                            @SubscribeEvent(priority = EventPriority.LOWEST) // run after others
                            public static void renderEntityPre(RenderLivingEvent.Pre event) {
                                if (event.isCanceled()) return;  // important: don’t push if someone canceled
                                final EntityLivingBase e = event.getEntity();
                        final float s = EntitySizeUtil.getEntityScale(e);
                        if (Math.abs(s - 1.0F) <= 1.0e-4F) return; // no push/pop if not scaled

                        GlStateManager.pushMatrix();
                        GlStateManager.scale(s, s, s);
                        GlStateManager.translate(
                            (event.getX() / s) - event.getX(),
                            (event.getY() / s) - event.getY(),
                            (event.getZ() / s) - event.getZ()
                        );
                        if (e.isSneaking()) {
                            GlStateManager.translate(0.0F, 0.125F / s, 0.0F);
                            GlStateManager.translate(0.0F, -0.125F, 0.0F);
                        }
                        PUSHED.get().add(e.getEntityId());
                            }

                            @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
                            public static void renderEntityPost(RenderLivingEvent.Post event) {
                                //final float s = EntitySizeUtil.getEntityScale(event.getEntity());
                        final int id = event.getEntity().getEntityId();
        if (PUSHED.get().remove(id)) {
            GlStateManager.popMatrix();
        }

                            }

                            // Nameplate: separate push/pop just for the vertical adjustment
                            @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
                            public static void renderEntityNamePre(RenderLivingEvent.Specials.Pre event) {
                                if (event.isCanceled()) return;
                                final EntityLivingBase e = event.getEntity();
                                final float s = EntitySizeUtil.getEntityScale(e);
                    if (Math.abs(s - 1.0F) <= 1.0e-4F) return;
                    GlStateManager.pushMatrix();

                                final boolean sneaking = e.isSneaking();
                                final float vanillaOffset  = e.height + 0.5F - (sneaking ? 0.25F : 0.0F);
                                final float adjustedOffset = (e.height / s) + 0.5F - (sneaking ? 0.25F : 0.0F);

                                GlStateManager.translate(0.0F, -vanillaOffset, 0.0F);
                                GlStateManager.translate(0.0F,  adjustedOffset, 0.0F);
                                NAME_PUSHED.get().add(e.getEntityId());
                            }

                            @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
                    public static void renderEntityNamePost(RenderLivingEvent.Specials.Post event) {
                        //final float s = EntitySizeUtil.getEntityScale(event.getEntity());
                        final int id = event.getEntity().getEntityId();
        if (NAME_PUSHED.get().remove(id)) {
            GlStateManager.popMatrix();
        }
                    }

            @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
            public static void guardHitboxPre(RenderLivingEvent.Pre event) { 
                if (event.isCanceled()) return;  // important: don’t push if someone canceled
            final EntityLivingBase lb = event.getEntity(); 
            if (!(lb instanceof net.minecraft.entity.EntityLiving)) return; 
            final net.minecraft.entity.EntityLiving e = (net.minecraft.entity.EntityLiving) lb; 
            final NBTTagCompound d = e.getEntityData(); 
            if (!d.hasKey("laputan_base_w") || !d.hasKey("laputan_base_h")) return; 
            final float sLocal = d.hasKey("laputan_last_scale") ? d.getFloat("laputan_last_scale") : 1.0F; 
            float targetW = Math.max(0.001F, d.getFloat("laputan_base_w") * sLocal); 
            float targetH = Math.max(0.001F, d.getFloat("laputan_base_h") * sLocal); 
            final float eps = 1.0e-3F; 
            if (Math.abs(e.width - targetW) > eps || Math.abs(e.height - targetH) > eps) { 
                laputan.handlers.EntitySizeHandler.setEntitySize(e, targetW, targetH); 
            } 
            }
                        }

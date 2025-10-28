package laputan.util;

import laputan.capabilities.ISizeCapability;
import laputan.capabilities.SizeProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class EntitySizeUtil {
	
	public static final float HARD_MIN = 1.0f / 64.0f; // 0.015625
	public static final float HARD_MAX = 64.0f;
	
	public static float getEntityScale(Entity e) {
    ISizeCapability cap = e.hasCapability(SizeProvider.sizeCapability, null)
        ? e.getCapability(SizeProvider.sizeCapability, null) : null;

    if (cap != null) {
        float s = cap.getScale();
        if (s > 0f && s != 1.0f) return s;  // already set
    }

// Predict deterministically, no mutation on server
if (e.world != null && e.world.isRemote && e instanceof net.minecraft.entity.EntityLiving) {
    final String KEY = "laputan_predicted_scale";
    net.minecraft.nbt.NBTTagCompound data = e.getEntityData();
    float predicted = data.hasKey(KEY) ? data.getFloat(KEY) : 0F;
    if (predicted > 0F && predicted != 1.0F) {
        return predicted;
    }
    // compute once, store, and reuse until the server packet sets the real cap
    net.minecraft.entity.EntityLiving el = (net.minecraft.entity.EntityLiving) e;
    laputan.Config.SizeRange r = laputan.Config.getSizeRangeFor(el);
    float s = (r != null) ? r.randomSizeFor(el) : 1.0F;
    if (s <= 0F) s = 1.0F;
    data.setFloat(KEY, s);
    return s;
}
return 1.0F;

}

}

                                                    package laputan.handlers;

                                                    import net.minecraft.entity.Entity;
                                                    import net.minecraft.entity.EntityLiving;
                                                    import net.minecraft.entity.EntityLivingBase;
                                                    import net.minecraft.entity.SharedMonsterAttributes;
                                                    import net.minecraft.entity.ai.attributes.AttributeModifier;
                                                    import net.minecraft.entity.ai.attributes.IAttributeInstance;
                                                    import net.minecraft.util.ResourceLocation;
                                                    import net.minecraft.util.math.AxisAlignedBB;
                                                    import net.minecraft.util.math.MathHelper;
                                                    import net.minecraftforge.event.AttachCapabilitiesEvent;
                                                    import net.minecraftforge.event.entity.EntityJoinWorldEvent;
                                                    import net.minecraftforge.event.entity.EntityMountEvent;
                                                    import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;
                                                    import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
                                                    import net.minecraftforge.event.entity.living.LivingHurtEvent;
                                                    import net.minecraftforge.event.world.WorldEvent;
                                                    import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
                                                    import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
                                                    import net.minecraft.nbt.NBTTagCompound;
                                                    import net.minecraft.entity.player.EntityPlayerMP;
                                                    import net.minecraftforge.fml.common.eventhandler.EventPriority;
                                                    import java.util.WeakHashMap;
                                                    import java.util.Iterator;
                                                    import java.util.Map;
                                                    import java.util.UUID;
                                                    import net.minecraftforge.fml.relauncher.Side;
                                                    import net.minecraftforge.fml.relauncher.SideOnly;

                                                    import laputan.Config;
                                                    import laputan.Laputan;
                                                    import laputan.capabilities.ISizeCapability;
                                                    import laputan.capabilities.SizeProvider;
                                                    import laputan.network.MessageSizeChange;
                                                    import laputan.network.PacketHandler;
                                                    import laputan.util.EntitySizeUtil;

                                                    import laputan.network.MessageSizeBase;


                                                    @EventBusSubscriber(modid = Laputan.MODID)
                                                    public class EntitySizeHandler {

                                                        private static final class BaseWH {
                                                            final float bw, bh;
                                                            BaseWH(float bw, float bh) { this.bw = bw; this.bh = bh; }
                                                        }

                                                        private static final Map<Entity, BaseWH> CACHE_CLIENT = new WeakHashMap<>();
                                                        private static final Map<Entity, BaseWH> CACHE_SERVER = new WeakHashMap<>();

    private static Map<Entity, BaseWH> cacheFor(Entity e) {
        return (e.world != null && e.world.isRemote) ? CACHE_CLIENT : CACHE_SERVER;
    }

    private static final int REBASE_SUS_TICKS = 5;

    private static boolean isSuspiciousBaseDims(float width, float height) {
        if (width <= 0.0F || height <= 0.0F) {
            return true;
        }
        final float minNominal = 0.6F;
        return width < minNominal && height < minNominal;
    }

                                                        private static double fastPowMinus1(double s, double exp) {
                                                        // s^exp - 1 with common branches
                                                            if (exp == 1.0)  return (s - 1.0);
                                                            if (exp == 0.5)  return (Math.sqrt(s) - 1.0);
                                                        if (exp == 0.0)  return 0.0;              // s^0 - 1
                                                        return Math.pow(s, exp) - 1.0;
                                                    }
                                                    private static double fastPow(double s, double exp) {
                                                        if (exp == 1.0)  return s;
                                                        if (exp == 0.5)  return Math.sqrt(s);
                                                        if (exp == 0.0)  return 1.0;
                                                        return Math.pow(s, exp);
                                                    }

                                                    // --- Laputan attribute modifiers ---
                                                    // Multiply-total (op=2)
                                                    public static final AttributeModifier SPEED_MODIFIER = new AttributeModifier(
                                                      UUID.fromString("1E7E2380-2E87-45B6-A90C-869563A27FA3"), "laputan_speed_mult", 0.0, 2);
                                                    public static final AttributeModifier ATTACK_DAMAGE_MODIFIER = new AttributeModifier(
                                                      UUID.fromString("174DEEAF-A876-4666-BFEB-709960E09021"), "laputan_attack_mult", 0.0, 2);
                                                    public static final AttributeModifier MAX_HEALTH_MODIFIER = new AttributeModifier(
                                                      UUID.fromString("7A9A9A97-6A0E-42D6-9F6A-8A9F2B9A9A97"), "laputan_health_mult", 0.0, 2);
                                                    public static final AttributeModifier KB_RESIST_MODIFIER = new AttributeModifier(
                                                      UUID.fromString("2A6A3F1C-2D7D-49E1-BB0A-4F1E9B9C3A77"), "laputan_kb_mult", 0.0, 2);
                                                    public static final AttributeModifier FOLLOW_RANGE_MODIFIER = new AttributeModifier(
                                                        UUID.fromString("C1E43B1B-0E3E-4E53-9B1C-2C3F5D4E6A70"), "laputan_follow_range_mult", 0.0, 2);
                                                    public static final AttributeModifier FLYING_SPEED_MODIFIER = new AttributeModifier(
    UUID.fromString("7FE8C7E5-1B0F-4F8E-9F7F-9C5C8A1B2D3E"), "laputan_flying_mult", 0.0, 2);

// Multiply-base (op=1) — variance wobble (±%)
                                                    public static final AttributeModifier SPEED_VAR_MOD = new AttributeModifier(
                                                      UUID.fromString("B5E9C2A6-92E9-4F1C-8E6A-41C7D6F9B1E1"), "laputan_speed_var", 0.0, 1);
                                                    public static final AttributeModifier ATTACK_VAR_MOD = new AttributeModifier(
                                                      UUID.fromString("0C3F6C26-23A7-4C69-9C2D-8D6C0F4E1A21"), "laputan_attack_var", 0.0, 1);
                                                    public static final AttributeModifier HEALTH_VAR_MOD = new AttributeModifier(
                                                      UUID.fromString("4B7E1D63-0F1E-4B17-9B4A-5F2E3D1C7A55"), "laputan_health_var", 0.0, 1);
                                                    public static final AttributeModifier KB_VAR_MOD = new AttributeModifier(
                                                      UUID.fromString("8D6E2F17-3A92-4E0A-8A6B-1F3C9D7E2B44"), "laputan_kb_var", 0.0, 1);
                                                    public static final AttributeModifier FOLLOW_RANGE_VAR_MOD = new AttributeModifier(
    UUID.fromString("F493D811-6E5C-4F43-AD69-9BA6D3F5B2C4"), "laputan_follow_range_var", 0.0, 1);
                                                    public static final AttributeModifier FLYING_VAR_MOD = new AttributeModifier(
    UUID.fromString("2C4B8E1A-5D6F-47A1-B0F8-2D1E3C5A7B9C"), "laputan_flying_var", 0.0, 1);

public static final AttributeModifier HEALTH_QUANTIZE_MOD = new AttributeModifier(
    UUID.fromString("A6C7B3B3-9F8E-4F42-8A1C-7F2E9A8C3B11"), "laputan_health_quant", 0.0, 0);
public static final AttributeModifier ATTACK_QUANTIZE_MOD = new AttributeModifier(
    UUID.fromString("D2E5C1F7-4D4D-47B5-A3E5-3D7B8C1F2A99"), "laputan_attack_quant", 0.0, 0);

                                                    private static void sendBaseToTrackers(EntityLiving e, float bw, float bh) {
                                                        if (e.world == null || e.world.isRemote) return;
                                                        net.minecraft.world.WorldServer ws = (net.minecraft.world.WorldServer) e.world;
                                                        laputan.network.MessageSizeBase msg =
                                                        new laputan.network.MessageSizeBase(e.getEntityId(), bw, bh);

                                                        for (net.minecraft.entity.player.EntityPlayer p : ws.getEntityTracker().getTrackingPlayers(e)) {
                                                            if (p instanceof net.minecraft.entity.player.EntityPlayerMP) {
                                                                PacketHandler.INSTANCE.sendTo(msg, (net.minecraft.entity.player.EntityPlayerMP) p);
                                                            }
                                                        }
                                                    }

                                                    public static void setCachedBase(Entity e, float bw, float bh) {
                                                        Map<Entity, BaseWH> m = (e.world != null)
                                                        ? cacheFor(e)
                                                        : (net.minecraftforge.fml.common.FMLCommonHandler.instance().getSide().isClient()
                                                            ? CACHE_CLIENT : CACHE_SERVER);
                                                        m.put(e, new BaseWH(bw, bh));
                                                    }

                                                    private static void purgeLaputanMods(IAttributeInstance attr) {
                                                        if (attr == null) return;
                                                        for (int op = 0; op <= 2; op++) {
                                                            java.util.List<AttributeModifier> copy =
                                                            new java.util.ArrayList<>(attr.getModifiersByOperation(op));
                                                            for (AttributeModifier mod : copy) {
                                                                String n = mod.getName();
                                                                if (n != null && n.startsWith("laputan_")) {
                                                                    attr.removeModifier(mod);
                                                                }
                                                            }
                                                        }
                                                    }

                                                    @SubscribeEvent
                                                    public static void onAddCapabilities(AttachCapabilitiesEvent<Entity> event) {
                                                        if (!(event.getObject() instanceof EntityLiving)) return;

                                                        EntityLiving el = (EntityLiving) event.getObject();
                                                        // You can keep this; it doesn’t touch world
                                                        if (Config.RESIZING_BLACKLIST.contains(el.getClass())) return;

                                                        // ATTACH — no world access, no hasCapability here
                                                        event.addCapability(new ResourceLocation(Laputan.MODID, "size"), new SizeProvider());
                                                    }

                                                        @SubscribeEvent(priority = EventPriority.LOWEST) // try to run after others
                                                        public static void onLivingUpdate(LivingUpdateEvent event) {
                                                            if (!(event.getEntityLiving() instanceof EntityLiving)) return;
                                                            if (event.getEntityLiving().world.isRemote) return;

                                                            EntityLiving entity = (EntityLiving) event.getEntityLiving();
                                                            if (!entity.hasCapability(SizeProvider.sizeCapability, null)) return;

        final boolean server = !entity.world.isRemote;
        ISizeCapability cap = entity.getCapability(SizeProvider.sizeCapability, null);
        if (cap == null) return;

        NBTTagCompound data = entity.getEntityData();
        int susCountdown = data.getInteger("laputan_rebase_sus");
        if (susCountdown > 0) {
            data.setInteger("laputan_rebase_sus", susCountdown - 1);
        }

        final Map<Entity, BaseWH> cache = cacheFor(entity);

                                                            final EntityLivingBase rider = event.getEntityLiving();
                                                            if (rider.ticksExisted >= 1 &&
    rider.getEntityData().getBoolean("laputan_defer_dismount") &&
    rider.isRiding()) {

    final Entity mount = rider.getRidingEntity();
    if (mount != null && Config.MOUNT_MIN_SCALE > 0f &&
        mount instanceof EntityLivingBase &&
        EntitySizeUtil.getEntityScale((EntityLivingBase) mount) < Config.MOUNT_MIN_SCALE) {
        rider.dismountRidingEntity();
    }
    rider.getEntityData().removeTag("laputan_defer_dismount");
}

                                                        // === SERVER FALLBACK: ensure scale is initialized even if EntityJoinWorld never ran ===
                                                            if (server) {
                                                                NBTTagCompound d = entity.getEntityData();
                                                        // Only if nothing has initialized yet (neither JoinWorld nor a prior fallback)
                                                                if (!d.getBoolean("laputan_scale_init") && !d.getBoolean("laputan_scale_init_srv")) {
                                                                    Config.SizeRange r = Config.getSizeRangeFor(entity);
                                                                    float s0 = (r != null) ? r.randomSizeFor(entity) : 1.0F;
                                                                    if (s0 <= 0F) s0 = 1.0F;

                                                                    cap.setScale(s0);

                                                            // stamp base dims once (child → grown)
                                                                    if (!d.hasKey("laputan_base_w")) {
                                                                        float bw = entity.isChild() ? entity.width * 2F : entity.width;
                                                                        float bh = entity.isChild() ? entity.height * 2F : entity.height;
                                                                        d.setFloat("laputan_base_w", bw);
                                                                        d.setFloat("laputan_base_h", bh);
                                                                        // ... after you set bw/bh and put into cache:
                                                                        if (server) sendBaseToTrackers(entity, bw, bh);

                                                                    }
                                                                    if (!d.hasKey("laputan_base_step")) {
                                                                        d.setFloat("laputan_base_step", entity.stepHeight);
                                                                    }


                                                            // force the attr pass this tick
                                                                    d.setFloat("laputan_last_server_scale", 0F);

                                                            // mark both flags so we don't re-run
                                                                    d.setBoolean("laputan_scale_init_srv", true);
                                                                    d.setBoolean("laputan_scale_init", true);
                                                                }
                                                            }

                                                        // Authoritative server scale. DO NOT recompute here.
                                                            //if (cap == null) return;
                                                            final float sCap = (cap.getScale() > 0F) ? cap.getScale() : 1.0F;

                                                        // Scale to use for hitbox on THIS SIDE:
                                                        // - server: sCap
                                                        // - client: cap if known, else predicted deterministic (cached) from EntitySizeUtil
                                                            final float sLocal = server
                                                            ? sCap
                                                            : ((sCap > 0F && sCap != 1.0F) ? sCap : EntitySizeUtil.getEntityScale(entity));

                                                    // Cache base dims once per side (child -> grown dims)
        if (!cache.containsKey(entity)) {

            if (server) {
// 1) Prefer the canonical base if it already exists (don’t overwrite!)
                if (data.hasKey("laputan_base_w") && data.hasKey("laputan_base_h")) {
                    float bw = data.getFloat("laputan_base_w");
                    float bh = data.getFloat("laputan_base_h");
                    cache.put(entity, new BaseWH(bw, bh));
// keep the child flag up to date, but do NOT rewrite bw/bh
                    data.setBoolean("laputan_base_child", entity.isChild());
                    data.setInteger("laputan_rebase_sus", 0);
// No need to spam trackers; they’ll get base on StartTracking
                } else {
// 2) Mint canonical base once if truly missing
                    float cw = entity.width, ch = entity.height;
                    if (isSuspiciousBaseDims(cw, ch)) {
                        int susTicks = Math.max(data.getInteger("laputan_rebase_sus"), REBASE_SUS_TICKS);
                        data.setInteger("laputan_rebase_sus", susTicks);
                        return;
                    }

                    float bw = entity.isChild() ? cw * 2F : cw;
                    float bh = entity.isChild() ? ch * 2F : ch;

                    cache.put(entity, new BaseWH(bw, bh));
                    data.setFloat("laputan_base_w", bw);
                    data.setFloat("laputan_base_h", bh);
                    data.setBoolean("laputan_base_child", entity.isChild());

// Tell current trackers the canonical base we just minted
                    sendBaseToTrackers(entity, bw, bh);
                    data.setInteger("laputan_rebase_sus", 0);
                }
            } else {
// Client: only adopt if the server already told us the base
                if (data.hasKey("laputan_base_w") && data.hasKey("laputan_base_h")) {
                    cache.put(entity, new BaseWH(data.getFloat("laputan_base_w"), data.getFloat("laputan_base_h")));
                } else {
// No canonical base yet — do NOT size this tick
                    return;
                }
            }
        }

        BaseWH base = cache.get(entity);

// Per-side flags (client & server each keep their own)
                                                            boolean applied  = data.getBoolean("laputan_applied");
                                                            float lastScale  = data.hasKey("laputan_last_scale") ? data.getFloat("laputan_last_scale") : 0F;
                                                            boolean sizeChangedNow = false;
                                                            boolean wasChild = data.getBoolean("laputan_base_child");
                                                            boolean isChild  = entity.isChild();
        if (wasChild != isChild) {
            float bw = isChild ? entity.width * 2F : entity.width;
            float bh = isChild ? entity.height * 2F : entity.height;
            data.setFloat("laputan_base_w", bw);
            data.setFloat("laputan_base_h", bh);
            data.setBoolean("laputan_base_child", isChild);
            if (!data.hasKey("laputan_base_step")) {
                data.setFloat("laputan_base_step", entity.stepHeight);
            }
// re-apply size once with sLocal (server and client paths already handle it)
            BaseWH baseNew = new BaseWH(bw, bh);
            cache.put(entity, baseNew);
        }

                                                        // (A) HITBOX — BOTH SIDES, using sLocal (no recompute, no packets)
                                                    // Compute the target box every tick
                                                            float targetW = Math.max(0.001F, base.bw  * sLocal);
                                                            float targetH = Math.max(0.001F, base.bh * sLocal);

                                                            final float epsWH = 1e-3F;
                                                            boolean dimsMismatch = Math.abs(entity.width  - targetW) > epsWH
                                                            || Math.abs(entity.height - targetH) > epsWH;

                                                            if (!applied || Math.abs(lastScale - sLocal) > 1e-3F || dimsMismatch) {
                                                        setEntitySize(entity, targetW, targetH);    // feet-pinned version
                                                        data.setBoolean("laputan_applied", true);
                                                        data.setFloat("laputan_last_scale", sLocal);
                                                        sizeChangedNow = true;

                                                        // NEW – 1-tick lock to avoid immediately rebasing off our own write
                                                        data.setInteger("laputan_lock_until", entity.ticksExisted + 1);

                                                    }


                                                        // Server-only change detection for attributes uses the authoritative server scale,
                                                        // tracked independently so client prediction can’t trip it.
                                                    boolean attrsDone     = data.getBoolean("laputan_attrs_applied");
                                                    float lastServerScale = data.hasKey("laputan_last_server_scale") ? data.getFloat("laputan_last_server_scale") : 0F;
                                                    boolean scaleChanged  = Math.abs(lastServerScale - sCap) > 1e-3F;

                                                            // If we persisted the flag but not the modifiers (setSaved(false)), force a reapply.
                                                    if (server && attrsDone) {
                                                        IAttributeInstance spd = entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
                                                        IAttributeInstance dmg = entity.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
                                                        IAttributeInstance hp  = entity.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
                                                        IAttributeInstance kb  = entity.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE);
                                                        IAttributeInstance fr  = entity.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE);
IAttributeInstance fly = entity.getEntityAttribute(SharedMonsterAttributes.FLYING_SPEED);

boolean haveAny =
    (spd != null && (spd.getModifier(SPEED_MODIFIER.getID())         != null || spd.getModifier(SPEED_VAR_MOD.getID())   != null)) ||
    (dmg != null && (dmg.getModifier(ATTACK_DAMAGE_MODIFIER.getID()) != null || dmg.getModifier(ATTACK_VAR_MOD.getID())  != null)) ||
    (hp  != null && (hp .getModifier(MAX_HEALTH_MODIFIER.getID())    != null || hp .getModifier(HEALTH_VAR_MOD.getID())   != null)) ||
    (kb  != null && (kb .getModifier(KB_RESIST_MODIFIER.getID())     != null || kb .getModifier(KB_VAR_MOD.getID())      != null)) ||
    (fr  != null && (fr .getModifier(FOLLOW_RANGE_MODIFIER.getID())  != null || fr .getModifier(FOLLOW_RANGE_VAR_MOD.getID()) != null)) ||
    (fly != null && (fly.getModifier(FLYING_SPEED_MODIFIER.getID())  != null || fly.getModifier(FLYING_VAR_MOD.getID())  != null));


                                                        if (!haveAny) {
                                                            attrsDone = false;
                                                            data.setBoolean("laputan_attrs_applied", false);
                                                        }
                                                    }


                                                        // (B) ATTRIBUTES — SERVER ONLY (use sCap inside your existing block)
                                                    if (server && (!attrsDone || scaleChanged)) {
                                                        data.setFloat("laputan_last_server_scale", sCap);

                                                        boolean aiEnabledNow = false;

                                                        IAttributeInstance spdAttr = entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);

// roll variance once (uniform ±pct -> amount in [-p, +p]; store as FRACTION, not %)
                                                        if (!data.getBoolean("laputan_var_init")) {
                                                            java.util.Random r = entity.getRNG();
                                                            float vH = (float)((r.nextDouble()*2.0 - 1.0) * (Config.HEALTH_VARIANCE_PCT / 100.0));
                                                            float vD = (float)((r.nextDouble()*2.0 - 1.0) * (Config.DAMAGE_VARIANCE_PCT / 100.0));
                                                            float vS = (float)((r.nextDouble()*2.0 - 1.0) * (Config.SPEED_VARIANCE_PCT  / 100.0));
                                                            float vK = (float)((r.nextDouble()*2.0 - 1.0) * (Config.KB_VARIANCE_PCT     / 100.0));
                                                            float vFR= (float)((r.nextDouble()*2.0 - 1.0) * (Config.FOLLOW_RANGE_VARIANCE_PCT / 100.0));
    float vFy= (float)((r.nextDouble()*2.0 - 1.0) * (Config.FLYING_VARIANCE_PCT       / 100.0));

    // Keep multipliers positive and sane
    vH = MathHelper.clamp(vH, -0.95F, 5.0F);   // health never goes near zero; allow big packs if you want
    vD = MathHelper.clamp(vD, -0.95F, 5.0F);   // damage never zero
    vS = MathHelper.clamp(vS, -0.95F, 3.0F);   // speed not negative nor absurdly fast from variance alone
    vK = MathHelper.clamp(vK, -0.99F, 0.99F);  // knockback variance stays finite
    vFR= MathHelper.clamp(vFR,-0.95F, 3.0F);
    vFy= MathHelper.clamp(vFy,-0.95F, 3.0F);

    data.setFloat("laputan_var_hp",  vH);
    data.setFloat("laputan_var_dmg", vD);
    data.setFloat("laputan_var_spd", vS);
    data.setFloat("laputan_var_kb",  vK);
    data.setFloat("laputan_var_fr",  vFR);
    data.setFloat("laputan_var_fly", vFy);
    data.setBoolean("laputan_var_init", true);
}


                                                            // exponents
final double aH = Config.HEALTH_EXP, aD = Config.DAMAGE_EXP, aV = Config.SPEED_EXP, aK = Config.KB_EXP;
final double aFR = Config.FOLLOW_RANGE_EXP, aFy = Config.FLYING_EXP;
final double mH = fastPowMinus1(sCap, aH);
final double mD = fastPowMinus1(sCap, aD);
final double mV = fastPowMinus1(sCap, aV);
final double mK = fastPowMinus1(sCap, aK);
final double mFR= fastPowMinus1(sCap, aFR);
final double mFy= fastPowMinus1(sCap, aFy);

                                                                //IAttributeInstance spdAttr = entity.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED);
if (spdAttr != null) {
    purgeLaputanMods(spdAttr);

// defensive: remove any legacy Laputan mods by UUID
    AttributeModifier oldVar = spdAttr.getModifier(SPEED_VAR_MOD.getID());
    if (oldVar != null) spdAttr.removeModifier(oldVar);
    AttributeModifier oldMul = spdAttr.getModifier(SPEED_MODIFIER.getID());
    if (oldMul != null) spdAttr.removeModifier(oldMul);

// size multiplier (op=2)
    spdAttr.applyModifier(new AttributeModifier(
        SPEED_MODIFIER.getID(), SPEED_MODIFIER.getName(), mV, 2
    ).setSaved(false));

// variance multiplier (op=1)
    double baseS = spdAttr.getBaseValue();
double minS  = Math.max(0.015, 0.35 * baseS);     // must still move
double maxS  = Math.min(0.80, 3.00 * baseS);      // not absurdly fast

double vS = data.getFloat("laputan_var_spd");     // fraction in [-.., +..]
double targetMulBase = 1.0 + vS;                  // op=1 amount = vS

// clamp variance so final stays within [minS, maxS] after size multiplier
double afterSize = baseS * (1.0 + mV);
double minVar = (minS / Math.max(1.0e-6, afterSize)) - 1.0;
double maxVar = (maxS / Math.max(1.0e-6, afterSize)) - 1.0;
double varEff = MathHelper.clamp(targetMulBase - 1.0, minVar, maxVar);

spdAttr.applyModifier(new AttributeModifier(
    SPEED_VAR_MOD.getID(), SPEED_VAR_MOD.getName(), varEff, 1
).setSaved(false));

}


IAttributeInstance dmgAttr = entity.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
if (dmgAttr != null) {
    purgeLaputanMods(dmgAttr);
    AttributeModifier oldVarD = dmgAttr.getModifier(ATTACK_VAR_MOD.getID());
    if (oldVarD != null) dmgAttr.removeModifier(oldVarD);
    AttributeModifier oldMulD = dmgAttr.getModifier(ATTACK_DAMAGE_MODIFIER.getID());
    if (oldMulD != null) dmgAttr.removeModifier(oldMulD);

// size (op=2)
    dmgAttr.applyModifier(new AttributeModifier(
        ATTACK_DAMAGE_MODIFIER.getID(), ATTACK_DAMAGE_MODIFIER.getName(), mD, 2
    ).setSaved(false));

// variance (op=1), keep strictly > 0
    double baseD = dmgAttr.getBaseValue();
    double vD = Math.max(-0.95, data.getFloat("laputan_var_dmg"));
    dmgAttr.applyModifier(new AttributeModifier(
        ATTACK_VAR_MOD.getID(), ATTACK_VAR_MOD.getName(), vD, 1
    ).setSaved(false));

    // ----- Quantize to half-hearts (1.0 HP) if enabled -----
if (Config.QUANTIZE_DAMAGE_TO_HALF_HEARTS) {
    // current final after op=1 & op=2
    double finalD = dmgAttr.getAttributeValue();

    // round to nearest 1.0 (half-heart) and enforce nonzero (>=1.0)
    double roundedD = Math.max(1.0, Math.floor(finalD + 0.5));

    // convert desired delta to op=0 space: (R - F)/((1+op1)*(1+op2))
    double combined = (1.0 + vD) * (1.0 + mD);
    if (combined < 1.0e-6) combined = 1.0e-6;

    double add0 = (roundedD - finalD) / combined;

    // apply the tiny correction
    AttributeModifier oldQ = dmgAttr.getModifier(ATTACK_QUANTIZE_MOD.getID());
    if (oldQ != null) dmgAttr.removeModifier(oldQ);
    dmgAttr.applyModifier(new AttributeModifier(
        ATTACK_QUANTIZE_MOD.getID(), ATTACK_QUANTIZE_MOD.getName(), add0, 0
    ).setSaved(false));
}

}
IAttributeInstance hpAttr = entity.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH);
if (hpAttr != null) {
    purgeLaputanMods(hpAttr);
    AttributeModifier oldVarH = hpAttr.getModifier(HEALTH_VAR_MOD.getID());
    if (oldVarH != null) hpAttr.removeModifier(oldVarH);
    AttributeModifier oldMulH = hpAttr.getModifier(MAX_HEALTH_MODIFIER.getID());
    if (oldMulH != null) hpAttr.removeModifier(oldMulH);

// size (op=2)
    hpAttr.applyModifier(new AttributeModifier(
        MAX_HEALTH_MODIFIER.getID(), MAX_HEALTH_MODIFIER.getName(), mH, 2
    ).setSaved(false));

// variance (op=1), keep strictly > 0
    double baseH = hpAttr.getBaseValue();
    double vH = Math.max(-0.95, data.getFloat("laputan_var_hp"));
    hpAttr.applyModifier(new AttributeModifier(
        HEALTH_VAR_MOD.getID(), HEALTH_VAR_MOD.getName(), vH, 1
    ).setSaved(false));

    // ----- Quantize to half-hearts (1.0 HP) if enabled -----
if (Config.QUANTIZE_HEALTH_TO_HALF_HEARTS) {
    double finalH = hpAttr.getAttributeValue();

    // nearest 1.0 and at least 1.0 (never zero)
    double roundedH = Math.max(1.0, Math.floor(finalH + 0.5));

    double combined = (1.0 + vH) * (1.0 + mH);
    if (combined < 1.0e-6) combined = 1.0e-6;

    double add0 = (roundedH - finalH) / combined;

    AttributeModifier oldQ = hpAttr.getModifier(HEALTH_QUANTIZE_MOD.getID());
    if (oldQ != null) hpAttr.removeModifier(oldQ);
    hpAttr.applyModifier(new AttributeModifier(
        HEALTH_QUANTIZE_MOD.getID(), HEALTH_QUANTIZE_MOD.getName(), add0, 0
    ).setSaved(false));
}

// rescale current HP proportionally to new max
    float oldMax = (float) baseH;
    float oldHP  = entity.getHealth();
    float newMax = (float) hpAttr.getAttributeValue();
    float newHP  = Math.min(newMax, (oldMax > 0f ? oldHP * (newMax / oldMax) : newMax));
    entity.setHealth(newHP);



}

IAttributeInstance kbAttr = entity.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE);
if (kbAttr != null) {
    purgeLaputanMods(kbAttr);
    AttributeModifier oldVarK = kbAttr.getModifier(KB_VAR_MOD.getID());
    if (oldVarK != null) kbAttr.removeModifier(oldVarK);
    AttributeModifier oldMulK = kbAttr.getModifier(KB_RESIST_MODIFIER.getID());
    if (oldMulK != null) kbAttr.removeModifier(oldMulK);

// size (op=2), keep sane
    final double mKc = Math.min(mK, 0.9);
    kbAttr.applyModifier(new AttributeModifier(
        KB_RESIST_MODIFIER.getID(), KB_RESIST_MODIFIER.getName(), mKc, 2
    ).setSaved(false));

// variance (op=1) but ensure final < 1.0 and >= 0
double baseK = kbAttr.getBaseValue();                 // usually 0..1
double vK = MathHelper.clamp(data.getFloat("laputan_var_kb"), -0.99, 0.99);

double afterSizeK = baseK * (1.0 + mKc);
double maxAllowed = 0.999;                            // never fully immune
double minAllowed = 0.0;

double maxVar = (afterSizeK > 0.0) ? (maxAllowed / afterSizeK) - 1.0 : 10.0;
double minVar = (afterSizeK > 0.0) ? (minAllowed / afterSizeK) - 1.0 : -0.99;

double varEffK = MathHelper.clamp(vK, (float)minVar, (float)maxVar);

// If afterSizeK already >= maxAllowed (can happen with crazy bases), force slight negative variance
if (afterSizeK >= maxAllowed) varEffK = Math.min(varEffK, -0.05);

kbAttr.applyModifier(new AttributeModifier(
    KB_VAR_MOD.getID(), KB_VAR_MOD.getName(), varEffK, 1
).setSaved(false));

}

IAttributeInstance frAttr = entity.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE);
if (frAttr != null) {
    purgeLaputanMods(frAttr);
    AttributeModifier oldFRm = frAttr.getModifier(FOLLOW_RANGE_MODIFIER.getID());
    if (oldFRm != null) frAttr.removeModifier(oldFRm);
    AttributeModifier oldFRv = frAttr.getModifier(FOLLOW_RANGE_VAR_MOD.getID());
    if (oldFRv != null) frAttr.removeModifier(oldFRv);

    double baseFR = frAttr.getBaseValue();
    if (Double.isFinite(baseFR) && baseFR > 0.0) {
        // size (op=2)
        frAttr.applyModifier(new AttributeModifier(
            FOLLOW_RANGE_MODIFIER.getID(), FOLLOW_RANGE_MODIFIER.getName(), mFR, 2
        ).setSaved(false));

        // variance (op=1) with clamp to 8..64 final
        double afterSize = baseFR * (1.0 + mFR);
        double vFR = data.getFloat("laputan_var_fr");   // [-.., +..]
        double minFR = 8.0, maxFR = 64.0;
        double minVar = (minFR / Math.max(1.0e-6, afterSize)) - 1.0;
        double maxVar = (maxFR / Math.max(1.0e-6, afterSize)) - 1.0;
        double varEff = MathHelper.clamp(vFR, (float)minVar, (float)maxVar);

        frAttr.applyModifier(new AttributeModifier(
            FOLLOW_RANGE_VAR_MOD.getID(), FOLLOW_RANGE_VAR_MOD.getName(), varEff, 1
        ).setSaved(false));
    }
}


IAttributeInstance flyAttr = entity.getEntityAttribute(SharedMonsterAttributes.FLYING_SPEED);
if (flyAttr != null) {
    purgeLaputanMods(flyAttr);

    AttributeModifier oldVarF = flyAttr.getModifier(FLYING_VAR_MOD.getID());
    if (oldVarF != null) flyAttr.removeModifier(oldVarF);
    AttributeModifier oldMulF = flyAttr.getModifier(FLYING_SPEED_MODIFIER.getID());
    if (oldMulF != null) flyAttr.removeModifier(oldMulF);

    // size (op=2)
    flyAttr.applyModifier(new AttributeModifier(
        FLYING_SPEED_MODIFIER.getID(), FLYING_SPEED_MODIFIER.getName(), mFy, 2
    ).setSaved(false));

    // variance (op=1) with sane clamps
    double baseF = flyAttr.getBaseValue();
    // Keep reasonable band; allow a bit faster than ground
    double minF  = Math.max(0.02, 0.35 * baseF);
    double maxF  = Math.min(1.20, 3.50 * baseF);

    double vFy = data.getFloat("laputan_var_fly");   // [-.., +..]
    double afterSizeF = baseF * (1.0 + mFy);
    double minVarF = (minF / Math.max(1.0e-6, afterSizeF)) - 1.0;
    double maxVarF = (maxF / Math.max(1.0e-6, afterSizeF)) - 1.0;
    double varEffF = MathHelper.clamp(vFy, (float)minVarF, (float)maxVarF);

    flyAttr.applyModifier(new AttributeModifier(
        FLYING_VAR_MOD.getID(), FLYING_VAR_MOD.getName(), varEffF, 1
    ).setSaved(false));

    // Optional: minimum flying-speed bump like ground speed (commented out by default)
    // UUID MIN_FLY_UUID = UUID.fromString("c2a2a489-3c25-4e5a-a6ab-0b4a9a1e2f77");
    // AttributeModifier oldMinFly = flyAttr.getModifier(MIN_FLY_UUID);
    // if (oldMinFly != null) flyAttr.removeModifier(oldMinFly);
    // double valF = flyAttr.getAttributeValue();
    // if (valF < minF) {
    //     flyAttr.applyModifier(new AttributeModifier(
    //         MIN_FLY_UUID, "laputan_min_flying_speed_bump", (minF - valF), 0
    //     ).setSaved(false));
    // }
}


if (!data.hasKey("laputan_base_step")) {
    data.setFloat("laputan_base_step", entity.stepHeight);
}
float baseStep = data.getFloat("laputan_base_step");
entity.stepHeight = MathHelper.clamp(baseStep * sCap, 0.15F, 1.0F);

data.setBoolean("laputan_attrs_applied", true);

                                                    // Laputan.LOG.info("attrs APPLY id={} sCap={} hitbox=({},{})", entity.getEntityId(), sCap, entity.width, entity.height);

                                                    // ----- WAKE-UP / FLOORS (SERVER ONLY) -----

                                                    // 1) Ensure AI is enabled (some packs spawn with NoAI)
if (entity.isAIDisabled()) {
    entity.setNoAI(false);
    aiEnabledNow = true;
}

                                                    // 2) Clamp movement speed to a minimum so they actually move (relative to base)
if (spdAttr != null) {
    final double baseSpeed = spdAttr.getBaseValue();
    final double minSpeed  = Math.max(0.015, 0.35 * baseSpeed);

    UUID MIN_SPEED_UUID = UUID.fromString("6b9c5a2a-2fb6-4ef3-9a6f-5b7f6e63d2f1");
    AttributeModifier oldMin = spdAttr.getModifier(MIN_SPEED_UUID);
    if (oldMin != null) spdAttr.removeModifier(oldMin);

    double val = spdAttr.getAttributeValue();
    if (val < minSpeed) {
        spdAttr.applyModifier(new AttributeModifier(
            MIN_SPEED_UUID, "laputan_min_speed_bump", (minSpeed - val), 0
        ).setSaved(false));
    }
}


                                                    // Cooldown: avoid thrashing navigator clears
int lastWake = data.getInteger("laputan_last_wake_tick");
                                                    // 3–5 ticks is plenty; start with 4
boolean recentlyWoke = entity.ticksExisted - lastWake < 4;

                                                    // 3/4) Only wake if something actually changed for pathing
if ((sizeChangedNow || aiEnabledNow) && !recentlyWoke) {
    if (entity.getNavigator() != null) entity.getNavigator().clearPath();
    entity.motionX += 1.0e-4;
    entity.velocityChanged = true;
    data.setInteger("laputan_last_wake_tick", entity.ticksExisted);
}


}
}

@SubscribeEvent(priority = EventPriority.LOWEST)
public static void entityJoinWorld(EntityJoinWorldEvent event) {
    if (event.getWorld().isRemote) return;
    if (!(event.getEntity() instanceof EntityLiving)) return;

    EntityLiving el = (EntityLiving) event.getEntity();
    if (!el.hasCapability(SizeProvider.sizeCapability, null)) return;
    ISizeCapability cap = el.getCapability(SizeProvider.sizeCapability, null);
    if (cap == null) return;

    NBTTagCompound data = el.getEntityData();
    if (!data.getBoolean("laputan_scale_init")) {
        Config.SizeRange r = Config.getSizeRangeFor(el);
        float s = (r != null) ? r.randomSizeFor(el) : 1.0F;
        if (s <= 0F) s = 1.0F;
        cap.setScale(s);
        data.setBoolean("laputan_scale_init", true);
    }

}

@SubscribeEvent
public static void onStartTracking(net.minecraftforge.event.entity.player.PlayerEvent.StartTracking ev) {
    if (!(ev.getTarget() instanceof EntityLiving)) return;
    EntityLiving e = (EntityLiving) ev.getTarget();
    if (!e.hasCapability(SizeProvider.sizeCapability, null)) return;

                    // scale first
    ISizeCapability cap = e.getCapability(SizeProvider.sizeCapability, null);
    float s = (cap != null ? cap.getScale() : 1.0F);
    PacketHandler.INSTANCE.sendTo(new MessageSizeChange(s, e.getEntityId()),
        (EntityPlayerMP) ev.getEntityPlayer());

                    // base (compute once if missing)
    NBTTagCompound d = e.getEntityData();
    boolean hasBase = d.hasKey("laputan_base_w") && d.hasKey("laputan_base_h");
    if (!hasBase) {
        float cw = e.width, ch = e.height;
        if (isSuspiciousBaseDims(cw, ch)) {
            int susTicks = Math.max(d.getInteger("laputan_rebase_sus"), REBASE_SUS_TICKS);
            d.setInteger("laputan_rebase_sus", susTicks);
            return;
        }

        float bw = e.isChild() ? cw * 2F : cw;
        float bh = e.isChild() ? ch * 2F : ch;
        d.setFloat("laputan_base_w", bw);
        d.setFloat("laputan_base_h", bh);
        d.setBoolean("laputan_base_child", e.isChild());
        d.setInteger("laputan_rebase_sus", 0);
        hasBase = true;
    }

    if (hasBase) {
        PacketHandler.INSTANCE.sendTo(
            new MessageSizeBase(e.getEntityId(), d.getFloat("laputan_base_w"), d.getFloat("laputan_base_h")),
            (EntityPlayerMP) ev.getEntityPlayer()
        );
    }
}


@SubscribeEvent
public static void worldUnload(WorldEvent.Unload event) {
                        // Clear the per-side base-dim cache for the world that's going away
    Map<Entity, BaseWH> cache = event.getWorld().isRemote ? CACHE_CLIENT : CACHE_SERVER;
    cache.entrySet().removeIf(en -> {
        Entity e = en.getKey();
                            return e == null || e.world == event.getWorld(); // identity compare is fine here
                        });
}


@SubscribeEvent
public static void entityMount(EntityMountEvent event) {
if (!event.isMounting() || event.getWorldObj().isRemote) return;

final Entity mount = event.getEntityBeingMounted();
if (Config.MOUNT_MIN_SCALE > 0f && mount instanceof EntityLivingBase) {
    if (EntitySizeUtil.getEntityScale((EntityLivingBase) mount) < Config.MOUNT_MIN_SCALE) {
        event.getEntityMounting().getEntityData().setBoolean("laputan_defer_dismount", true);
    }
}

}

@SubscribeEvent 
public static void playSoundAtEntity(PlaySoundAtEntityEvent event) { 
    final Entity e = event.getEntity(); 
    if (e == null || e.world == null || !e.world.isRemote) return; 
// client only 
    if (!e.hasCapability(SizeProvider.sizeCapability, null)) return; 
    float s = EntitySizeUtil.getEntityScale(e); 
    if (s <= 0f) s = 0.05f; 
// EXTREME test profile 
    float volMul = (float)Math.pow(MathHelper.sqrt(s), 1.5f) * 2.0f; 
    float pitchMul = (float)Math.pow(s, -1.8f); 
    event.setVolume(MathHelper.clamp(event.getVolume() * volMul, 0.01f, 8.0f)); 
    event.setPitch(MathHelper.clamp(event.getPitch() * pitchMul, 0.25f, 3.0f)); 
}

public static void setEntitySize(EntityLiving e, float w, float h) {
    if (w < 0.001F) w = 0.001F;
    if (h < 0.001F) h = 0.001F;
    if (w == e.width && h == e.height) return;

    if (e.isChild()) { w *= 0.5F; h *= 0.5F; }

                                                           // Pin feet: keep minY (posY) fixed while we rebuild the AABB
    final double baseY = e.posY;
    e.width  = w;
    e.height = h;

    double r = w / 2.0D;
    AxisAlignedBB bb = new AxisAlignedBB(
        e.posX - r, baseY, e.posZ - r,
        e.posX + r, baseY + e.height, e.posZ + r
    );
    e.setEntityBoundingBox(bb);
                                                        e.posY = baseY; // keep pos in sync

                                                        // If still intersecting, tiny corrective nudge in the relieving direction
                                                        if (!e.world.isRemote && !e.world.getCollisionBoxes(e, e.getEntityBoundingBox()).isEmpty()) {
                                                            e.posY += 1.0e-3D;
                                                            e.setEntityBoundingBox(e.getEntityBoundingBox().offset(0, 1.0e-3D, 0));
                                                        }
                                                    }

                                                    private static void setEntitySizeGeneric(Entity e, float w, float h) {
                                                        if (e instanceof EntityLiving) { setEntitySize((EntityLiving)e, w, h); return; }
                                                        if (w < 0.001F) w = 0.001F;
                                                        if (h < 0.001F) h = 0.001F;
                                                        double r = w / 2.0D;
                                                        AxisAlignedBB bb = new AxisAlignedBB(
                                                            e.posX - r, e.posY, e.posZ - r,
                                                            e.posX + r, e.posY + h, e.posZ + r
                                                        );
                                                        e.setEntityBoundingBox(bb);
                                                        e.width = w; e.height = h;
                                                    }

                                                }

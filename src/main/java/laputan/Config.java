			package laputan;

			import java.util.ArrayList;
			import java.util.Arrays;
			import java.util.HashMap;
			import java.util.List;
			import java.util.Map;
			import java.util.Map.Entry;
			import java.util.Random;

			import com.google.common.collect.Lists;

			import laputan.util.EntitySizeUtil;
			import net.minecraft.entity.EntityLiving;
			import net.minecraft.util.ResourceLocation;
			import net.minecraft.util.math.MathHelper;
			import net.minecraftforge.common.config.ConfigCategory;
			import net.minecraftforge.common.config.Configuration;
			import net.minecraftforge.common.config.Property;
			import net.minecraftforge.fml.common.registry.EntityEntry;
			import net.minecraftforge.fml.common.registry.ForgeRegistries;

			public class Config {
				
				private final static String CATEGORY_GENERAL = "all.general";
				
				private final static List<String> PROPERTY_ORDER_GENERAL = new ArrayList<String>();
				
				private static List<String> resizingBlacklistStrings;
				public static final java.util.Set<Class> RESIZING_BLACKLIST = new java.util.HashSet<Class>();
				private static List<String> entitySigmaDivStrings;
		public static final Map<Class, SizeRange> ENTITY_SIZES = new HashMap<Class, SizeRange>(); // unchanged name/type

			// ===== Balance (attribute scaling) =====
			public static double HEALTH_EXP = 0.85D;  // s^exp for max health
			public static double DAMAGE_EXP = 0.85D;  // s^exp for attack damage
			public static double SPEED_EXP  = -0.5D; // s^exp for movement speed
			public static double KB_EXP     = 0.85D;  // s^exp for knockback resistance
			// NEW:
public static double FOLLOW_RANGE_EXP = 0.50D; // sqrt(size) by default
public static double FLYING_EXP       = -0.50D; // match ground speed default

// ===== Variance (percentage wobble, uniform in [-pct, +pct], rolled once per entity) =====
public static float HEALTH_VARIANCE_PCT = 0.0F;
public static float DAMAGE_VARIANCE_PCT = 0.0F;
public static float SPEED_VARIANCE_PCT  = 0.0F;
public static float KB_VARIANCE_PCT     = 0.0F;
public static float FOLLOW_RANGE_VARIANCE_PCT = 0.0F;
public static float FLYING_VARIANCE_PCT       = 0.0F;
private static final String CATEGORY_VARIANCE = "variance";

// new category
private static final String CATEGORY_ROUNDING = "rounding";

// quantization toggles
public static boolean QUANTIZE_HEALTH_TO_HALF_HEARTS = true;
public static boolean QUANTIZE_DAMAGE_TO_HALF_HEARTS = true;

// near other generals
public static float MOUNT_MIN_SCALE = 0.75F; // Set to 0 to disable the check


			private static final String CATEGORY_BALANCE = "balance";

			// 0.0 = median-anchored (μ = logGM), 1.0 = mode-anchored (μ = logGM + σ²)
		public static float SIZE_ANCHOR_BIAS = 1.0F;

			public static void readConfig() {
				Configuration cfg = Laputan.config;
				try {
					cfg.load();
					initGeneralConfig(cfg);
				} catch (Exception e1) {

				} finally {
					if (cfg.hasChanged()) {
						cfg.save();
					}
				}
			}

			private static void initGeneralConfig(Configuration cfg) {
				cfg.addCustomCategoryComment(CATEGORY_GENERAL, "General Options");

			// --- Balance: exponents that scale attributes from size s ---
				cfg.addCustomCategoryComment(CATEGORY_BALANCE,
					"Attribute scaling exponents.\n" +
					"Values are used as s^exp. Example: s=2 with health_exponent=1.75 -> 2^1.75 ≈ 3.36x max health.");

				HEALTH_EXP = cfg.getFloat("health_exponent", CATEGORY_BALANCE,
					0.85F, 0.0F, 8.0F, "Max health multiplier = s^exp");

				DAMAGE_EXP = cfg.getFloat("damage_exponent", CATEGORY_BALANCE,
					0.85F, 0.0F, 8.0F, "Attack damage multiplier = s^exp");

				SPEED_EXP  = cfg.getFloat("speed_exponent", CATEGORY_BALANCE,
					-0.5F, -8.0F, 8.0F, "Movement speed multiplier = s^exp (negative slows big mobs)");

				KB_EXP     = cfg.getFloat("kb_exponent", CATEGORY_BALANCE,
					0.85F, 0.0F, 8.0F, "Knockback resistance multiplier = s^exp");

				FOLLOW_RANGE_EXP = cfg.getFloat("follow_range_exponent", CATEGORY_BALANCE,
    0.50F, -8.0F, 8.0F, "Follow range multiplier = s^exp (e.g., 0.5 means sqrt(size)).");

FLYING_EXP = cfg.getFloat("flying_speed_exponent", CATEGORY_BALANCE,
    -0.50F, -8.0F, 8.0F, "Flying speed multiplier = s^exp (usually similar to ground speed).");


cfg.addCustomCategoryComment(CATEGORY_VARIANCE,
    "Percentage attribute variance (uniform ±pct), rolled once per entity and persisted.\n" +
    "Example: 30 -> each mob gets a multiplier in [0.70, 1.30] on that attribute (on top of size scaling).");

HEALTH_VARIANCE_PCT = cfg.getFloat("health_variance_pct", CATEGORY_VARIANCE,
    0.0F, 0.0F, 100.0F, "±% variance on max health.");
DAMAGE_VARIANCE_PCT = cfg.getFloat("damage_variance_pct", CATEGORY_VARIANCE,
    0.0F, 0.0F, 100.0F, "±% variance on attack damage.");
SPEED_VARIANCE_PCT  = cfg.getFloat("speed_variance_pct",  CATEGORY_VARIANCE,
    0.0F, 0.0F, 100.0F, "±% variance on movement speed.");
KB_VARIANCE_PCT     = cfg.getFloat("kb_variance_pct",     CATEGORY_VARIANCE,
    0.0F, 0.0F, 100.0F, "±% variance on knockback resistance (kept < 1).");
FOLLOW_RANGE_VARIANCE_PCT = cfg.getFloat("follow_range_variance_pct", CATEGORY_VARIANCE,
    0.0F, 0.0F, 100.0F, "±% variance on follow range (clamped 8..64).");

FLYING_VARIANCE_PCT = cfg.getFloat("flying_speed_variance_pct", CATEGORY_VARIANCE,
    0.0F, 0.0F, 100.0F, "±% variance on flying speed.");

	cfg.addCustomCategoryComment(CATEGORY_ROUNDING,
    "Quantize certain attributes to half-hearts (1.0 HP steps).\n" +
    "If enabled, the FINAL value (after size & variance) is rounded to the nearest 1.0.");

QUANTIZE_HEALTH_TO_HALF_HEARTS = cfg.getBoolean(
    "quantize_health_to_half_hearts", CATEGORY_ROUNDING, true,
    "Round final Max Health to nearest half-heart (1.0 HP). Min 1.0."
);
QUANTIZE_DAMAGE_TO_HALF_HEARTS = cfg.getBoolean(
    "quantize_damage_to_half_hearts", CATEGORY_ROUNDING, true,
    "Round final Attack Damage to nearest half-heart (1.0 HP). Min 1.0."
);
				resizingBlacklistStrings = Lists.newArrayList(cfg.getStringList(
		  "Entity Resizing Blacklist", CATEGORY_GENERAL,
		  new String[] { "minecraft:giant", "minecraft:elder_guardian", "minecraft:shulker" },
		  "Entities on this list will not be resized or receive any attribute changes. Previously-resized ones return to normal on next spawn.")
		);

// Per-entity base + wildness list (wildness = 64 - divisor)
entitySigmaDivStrings = Lists.newArrayList(cfg.getStringList(
  "Entity Sigma Divisors", CATEGORY_GENERAL,
  new String[] { },
  "Per-entity base scale and sigma for size RNG.\n" +
  "FORMAT: <entity id>|<base>,<sigma>\n" +
  "Examples: 'minecraft:cow|1.0,24.0'\n" +
  "Base is the mob's base scaling. Sigma is the amount of size variation to apply (higher = wilder)\n" +
  "Sigma clamps to [0.0, 48.0]. If sigma == 0 or removed, size variation is DISABLED for that mob.\n" +
  "Overly large sigma values may crash the game."
));


		SIZE_ANCHOR_BIAS = cfg.getFloat(
		    "size_anchor_bias", CATEGORY_GENERAL,
		    1.0F, 0.0F, 1.0F,
		    "Anchor for the size distribution.\n" +
		    "0.0 = median at 1.0 (balanced). 1.0 = mode at 1.0 (skews larger as σ grows).\n" +
		    "Just leave it."
		);

		MOUNT_MIN_SCALE = cfg.getFloat(
    "mount_min_scale", CATEGORY_GENERAL,
    0.75F, 0.0F, 8.0F,
    "If a mount's size scale is below this, prevent/undo riding. Set to 0 to disable."
);

		PROPERTY_ORDER_GENERAL.add("Entity Resizing Blacklist");
		PROPERTY_ORDER_GENERAL.add("Entity Sigma Divisors");
		PROPERTY_ORDER_GENERAL.add("size_anchor_bias");
		PROPERTY_ORDER_GENERAL.add("mount_min_scale");


				cfg.setCategoryPropertyOrder(CATEGORY_GENERAL, PROPERTY_ORDER_GENERAL);
			}

			public static void postInit() {
			    // 1) Build blacklist from strings
			    for (String entityName : resizingBlacklistStrings) {
			        ResourceLocation key = new ResourceLocation(entityName);
			        if (ForgeRegistries.ENTITIES.containsKey(key)) {
			            EntityEntry e = ForgeRegistries.ENTITIES.getValue(key);
			            RESIZING_BLACKLIST.add(e.getEntityClass());
			        }
			    }

		ENTITY_SIZES.clear();
		for (String listEntry : entitySigmaDivStrings) {
		    int separator = listEntry.indexOf("|");
		    if (separator < 1) continue;

		    ResourceLocation key = new ResourceLocation(listEntry.substring(0, separator));
		    if (ForgeRegistries.ENTITIES.containsKey(key)) {
		        EntityEntry e = ForgeRegistries.ENTITIES.getValue(key);
		        // parse base, wildness
		        ENTITY_SIZES.put(e.getEntityClass(), new SizeRange(listEntry.substring(separator + 1)));
		    }
		}

// Ensure every living, non-blacklisted entity has an entry with default base=1.0, sigma=0.0
for (Entry<ResourceLocation, EntityEntry> entry : ForgeRegistries.ENTITIES.getEntries()) {
    Class<?> cls = entry.getValue().getEntityClass();
    if (!EntityLiving.class.isAssignableFrom(cls)) continue;
    if (RESIZING_BLACKLIST.contains(cls)) continue;

    if (!ENTITY_SIZES.containsKey(cls)) {
        ENTITY_SIZES.put(cls, new SizeRange("1.0, 0.0")); // internal divisor
        entitySigmaDivStrings.add(entry.getKey().toString() + "|1.0, 0.0"); // displayed wildness
    }
}


	// Write back the (possibly extended) list
	ConfigCategory general = Laputan.config.getCategory(CATEGORY_GENERAL);
	Property sigma = general.get("Entity Sigma Divisors");
	sigma.set(entitySigmaDivStrings.toArray(new String[0]));


			}

public static class SizeRange {
    float base;         // center (median/mode depending on bias)
    float sigmaDiv;     // INTERNAL divisor in [1..64] (higher = tighter)
    boolean noVariation;

    // Accepts "<base>,<wildness>" (NEW) or "<divisor>" (OLD single-number)
    private SizeRange(String configEntry) {
        float b = 1.0f;
        float wild = 0.0f; // display default matching divisor
        boolean nv = false;

        try {
            String s = configEntry.trim();
            int comma = s.indexOf(",");
            if (comma >= 0) {
                String bStr = s.substring(0, comma).trim();
                String wStr = s.substring(comma + 1).trim();
                b = Float.parseFloat(bStr);
                wild = Float.parseFloat(wStr);
            } else {
            // Single value = BASE ONLY; wildness defaults to 0
            b = Float.parseFloat(s);
            wild = 0.0f;
            }
        } catch (Exception ignored) { }

        // Clamp display wildness and compute internal divisor
        wild = MathHelper.clamp(wild, 0.0f, 64.0f);
        if (wild <= 0.0f) {
            nv = true;                  // explicitly disable RNG
        }
        float d = 64.0f - wild;         // map back to divisor
        d = MathHelper.clamp(d, 1.0f, 64.0f);

        this.base       = MathHelper.clamp(b, EntitySizeUtil.HARD_MIN, EntitySizeUtil.HARD_MAX);
        this.sigmaDiv   = d;
        this.noVariation= nv;
    }

    // Convenience constructor for code defaults: base + INTERNAL divisor
    private SizeRange(float base, float sigmaDiv) {
        this.base       = MathHelper.clamp(base, EntitySizeUtil.HARD_MIN, EntitySizeUtil.HARD_MAX);
        this.sigmaDiv   = MathHelper.clamp(sigmaDiv, 1.0F, 64.0F);
        this.noVariation= false;
    }

    public float randomSizeFor(EntityLiving e) {
        java.util.UUID u = e.getPersistentID();
        long seed = u.getMostSignificantBits() ^ u.getLeastSignificantBits()
                  ^ (31L * e.world.provider.getDimension());
        java.util.Random r = new java.util.Random(seed);
        return randomSize(r);
    }

    public float randomSize(Random rand) {
        if (this.noVariation) return this.base;  // hard disable RNG

        final float min = EntitySizeUtil.HARD_MIN;   // 0.015625F
        final float max = EntitySizeUtil.HARD_MAX;   // 64.0F

        final double logMin   = Math.log(min);
        final double logMax   = Math.log(max);
        final double logRange = logMax - logMin;

        final double sigma = logRange / (double) this.sigmaDiv;
        if (sigma <= 0.0) return this.base;

        // Anchor at base: bias=0 ⇒ median=base ; bias=1 ⇒ mode=base
        final double mu = Math.log(this.base) + (Config.SIZE_ANCHOR_BIAS) * (sigma * sigma);

        for (int tries = 0; tries < 8; tries++) {
            double z   = rand.nextGaussian();
            double val = Math.exp(mu + sigma * z);
            if (val >= min && val <= max) return (float) val;
        }

        double val = Math.exp(mu + sigma * rand.nextGaussian());
        if (val < min) val = min;
        if (val > max) val = max;
        return (float) val;
    }
}


			    public static SizeRange getSizeRangeFor(EntityLiving e) {
			        if (e == null) return null;
			        Class<?> c = e.getClass();
			        SizeRange r = ENTITY_SIZES.get(c);
			        while (r == null && c != null && c != EntityLiving.class) {
			            c = c.getSuperclass();
			            r = ENTITY_SIZES.get(c);
			        }
if (r == null) {
    r = new SizeRange("1.0, 0.0"); // base + INTERNAL divisor
}

			        return r;
			    }

			}

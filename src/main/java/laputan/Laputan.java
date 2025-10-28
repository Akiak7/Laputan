package laputan;

import java.io.File;

import laputan.capabilities.DefaultSizeCapability;
import laputan.capabilities.ISizeCapability;
import laputan.capabilities.SizeCapabilityStorage;
import laputan.network.PacketHandler;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = Laputan.MODID, name = Laputan.NAME, version = Laputan.VERSION, dependencies = Laputan.DEPENDENCIES)
public class Laputan {
	
	public static final String MODID = "laputan";
	public static final String NAME = "Laputan";
	public static final String VERSION = "1.3";
	public static final String DEPENDENCIES = "";
	
	@Mod.Instance
	public static Laputan instance;
	
	public static Configuration config;
	
	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		CapabilityManager.INSTANCE.register(ISizeCapability.class, new SizeCapabilityStorage(), DefaultSizeCapability.class);
		PacketHandler.registerMessages();
		File directory = event.getModConfigurationDirectory();
        config = new Configuration(new File(directory.getPath(), "laputan.cfg"));
        Config.readConfig();
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {
		
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		Config.postInit();
		if (config.hasChanged()) {
            config.save();
        }
	}
	
}

package laputan.network;

import laputan.Laputan;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class PacketHandler {


	
	public static SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(Laputan.MODID);
	
	private static int ID = 0;
	
	public static void registerMessages() {
    	INSTANCE.registerMessage(MessageSizeChange.MessageHolder.class, MessageSizeChange.class, ID++, Side.CLIENT);
    	// pick the next free id
INSTANCE.registerMessage(
    laputan.network.MessageSizeBase.Handler.class,
    laputan.network.MessageSizeBase.class,
    ID++,
    net.minecraftforge.fml.relauncher.Side.CLIENT
);

    }

}

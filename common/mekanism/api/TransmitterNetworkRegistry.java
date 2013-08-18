package mekanism.api;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeSubscribe;
import net.minecraftforge.event.world.ChunkEvent;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;

public class TransmitterNetworkRegistry implements ITickHandler
{
	private static TransmitterNetworkRegistry INSTANCE = new TransmitterNetworkRegistry();
	private static boolean loaderRegistered = false;
	
	private HashSet<ITransmitterNetwork> networks = new HashSet<ITransmitterNetwork>();
	
	public TransmitterNetworkRegistry()
	{
		TickRegistry.registerTickHandler(this, Side.SERVER);
	}
	
	public static void initiate()
	{
		if(!loaderRegistered)
		{
			loaderRegistered = true;
			
			MinecraftForge.EVENT_BUS.register(getInstance());
		}
	}
	
	public static TransmitterNetworkRegistry getInstance()
	{
		return INSTANCE;
	}
		
	public void registerNetwork(ITransmitterNetwork network)
	{
		networks.add(network);
	}
	
	public void removeNetwork(ITransmitterNetwork network)
	{
		if(networks.contains(network))
		{
			networks.remove(network);
		}
	}
	
	public void pruneEmptyNetworks()
	{
		for(ITransmitterNetwork e : networks)
		{
			if(e.getSize() == 0)
			{
				removeNetwork(e);
			}
		}
	}
	
	@Override
	public void tickStart(EnumSet<TickType> type, Object... tickData)
	{
		return;
	}
	
	@Override
	public void tickEnd(EnumSet<TickType> type, Object... tickData)
	{
		Set<ITransmitterNetwork> iterNetworks = (Set<ITransmitterNetwork>)networks.clone();
		
		for(ITransmitterNetwork net : iterNetworks)
		{
			if(networks.contains(net))
			{
				net.tick();
			}
		}
	}

	@Override
	public EnumSet<TickType> ticks()
	{
		return EnumSet.of(TickType.SERVER);
	}

	@Override
	public String getLabel()
	{
		return "MekanismNetworks";
	}
	
	@Override
	public String toString() 
	{
		return "Network Registry:\n" + networks;
	}
	
	public static class NetworkLoader
	{
		@ForgeSubscribe
		public void onChunkLoad(ChunkEvent.Load event)
		{
			if(event.getChunk() != null)
			{
				for(Object obj : event.getChunk().chunkTileEntityMap.values())
				{
					if(obj instanceof TileEntity)
					{
						TileEntity tileEntity = (TileEntity)obj;
						
						if(tileEntity instanceof ITransmitter)
						{
							((ITransmitter)tileEntity).refreshNetwork();
						}
					}
				}
			}
		}
	}
}

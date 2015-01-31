package mekanism.common;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mekanism.api.Coord4D;
import mekanism.api.ListUtils;
import mekanism.api.energy.IStrictEnergyAcceptor;
import mekanism.api.transmitters.DynamicNetwork;
import mekanism.api.transmitters.IGridTransmitter;
import mekanism.api.transmitters.TransmissionType;
import mekanism.common.util.CableUtils;
import mekanism.common.util.MekanismUtils;

import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.Event;

import cofh.api.energy.IEnergyReceiver;
import ic2.api.energy.EnergyNet;
import ic2.api.energy.tile.IEnergySink;

public class EnergyNetwork extends DynamicNetwork<TileEntity, EnergyNetwork>
{
	private double lastPowerScale = 0;
	private double joulesTransmitted = 0;
	private double jouleBufferLastTick = 0;

	public double clientEnergyScale = 0;

	public double electricityStored;

	public EnergyNetwork(IGridTransmitter<EnergyNetwork>... varCables)
	{
		transmitters.addAll(Arrays.asList(varCables));
		register();
	}

	public EnergyNetwork(Collection<IGridTransmitter<EnergyNetwork>> collection)
	{
		transmitters.addAll(collection);
		register();
	}

	public EnergyNetwork(Set<EnergyNetwork> networks)
	{
		for(EnergyNetwork net : networks)
		{
			if(net != null)
			{
				if(net.jouleBufferLastTick > jouleBufferLastTick || net.clientEnergyScale > clientEnergyScale)
				{
					clientEnergyScale = net.clientEnergyScale;
					jouleBufferLastTick = net.jouleBufferLastTick;
					joulesTransmitted = net.joulesTransmitted;
					lastPowerScale = net.lastPowerScale;
				}

				electricityStored += net.electricityStored;

				addAllTransmitters(net.transmitters);
				net.deregister();
			}
		}

		register();
	}
	
	public static double round(double d)
	{
		return Math.round(d * 10000)/10000;
	}

    @Override
	protected void updateMeanCapacity()
	{
        int numCables = transmitters.size();
        double reciprocalSum = 0;

		synchronized(transmitters)
		{
			for(IGridTransmitter<EnergyNetwork> cable : transmitters)
			{
				reciprocalSum += 1.0 / (double)cable.getCapacity();
			}
		}

        meanCapacity = (double)numCables / reciprocalSum;            
	}
    
    @Override
    public void onNetworksCreated(List<EnergyNetwork> networks)
    {
    	if(FMLCommonHandler.instance().getEffectiveSide().isServer())
    	{
	    	double[] caps = new double[networks.size()];
	    	double cap = 0;
	    	
	    	for(EnergyNetwork network : networks)
	    	{
	    		double networkCapacity = network.getCapacity();
	    		caps[networks.indexOf(network)] = networkCapacity;
	    		cap += networkCapacity;
	    	}
	    	
	    	electricityStored = Math.min(cap, electricityStored);
	    	
	    	double[] percent = ListUtils.percent(caps);
	    	
	    	for(EnergyNetwork network : networks)
	    	{
	    		network.electricityStored = round(percent[networks.indexOf(network)]*electricityStored);
	    	}
    	}
    }
	
	public double getEnergyNeeded()
	{
		if(FMLCommonHandler.instance().getEffectiveSide().isClient())
		{
			return 0;
		}

		return getCapacity()-electricityStored;
	}

	public double tickEmit(double energyToSend)
	{
		if(FMLCommonHandler.instance().getEffectiveSide().isClient())
		{
			return 0;
		}

		double sent = 0;
		boolean tryAgain = false;
		int i = 0;

		do {
			double prev = sent;
			sent += doEmit(energyToSend-sent, tryAgain);

			tryAgain = energyToSend-sent > 0 && sent-prev > 0 && i < 100;

			i++;
		} while(tryAgain);

		joulesTransmitted = sent;
		return sent;
	}

	public double emit(double energyToSend, boolean doEmit)
	{
		double toUse = Math.min(getEnergyNeeded(), energyToSend);
		if(doEmit)
		{
			electricityStored += toUse;
		}
		return energyToSend-toUse;
	}

	/**
	 * @return sent
	 */
	public double doEmit(double energyToSend, boolean tryAgain)
	{
		double sent = 0;

		List availableAcceptors = Arrays.asList(getAcceptors().toArray());

		Collections.shuffle(availableAcceptors);

		if(!availableAcceptors.isEmpty())
		{
			int divider = availableAcceptors.size();
			double remaining = energyToSend % divider;
			double sending = (energyToSend-remaining)/divider;

			for(Object obj : availableAcceptors)
			{
				if(obj instanceof TileEntity)
				{
					TileEntity acceptor = (TileEntity)obj;
					double currentSending = sending+remaining;
					ForgeDirection side = acceptorDirections.get(acceptor);

					if(side == null)
					{
						continue;
					}

					remaining = 0;

					if(acceptor instanceof IStrictEnergyAcceptor)
					{
						sent += ((IStrictEnergyAcceptor)acceptor).transferEnergyToAcceptor(side.getOpposite(), currentSending);
					}
					else if(MekanismUtils.useRF() && acceptor instanceof IEnergyReceiver)
					{
						IEnergyReceiver handler = (IEnergyReceiver)acceptor;
						int used = handler.receiveEnergy(side.getOpposite(), (int) Math.round(currentSending * Mekanism.TO_TE), false);
						sent += used * Mekanism.FROM_TE;
					}
					else if(MekanismUtils.useIC2() && acceptor instanceof IEnergySink)
					{
						double toSend = Math.min(currentSending, EnergyNet.instance.getPowerFromTier(((IEnergySink)acceptor).getSinkTier())*Mekanism.FROM_IC2);
						toSend = Math.min(toSend, ((IEnergySink)acceptor).getDemandedEnergy()*Mekanism.FROM_IC2);
						sent += (toSend - (((IEnergySink)acceptor).injectEnergy(side.getOpposite(), toSend*Mekanism.TO_IC2, 0)*Mekanism.FROM_IC2));
					}
				}
			}
		}

		return sent;
	}

	@Override
	public Set<TileEntity> getAcceptors(Object... data)
	{
		Set<TileEntity> toReturn = new HashSet<TileEntity>();

		if(FMLCommonHandler.instance().getEffectiveSide().isClient())
		{
			return toReturn;
		}

		for(TileEntity acceptor : possibleAcceptors.values())
		{
			ForgeDirection side = acceptorDirections.get(acceptor);

			if(side == null)
			{
				continue;
			}

			if(acceptor instanceof IStrictEnergyAcceptor)
			{
				IStrictEnergyAcceptor handler = (IStrictEnergyAcceptor)acceptor;

				if(handler.canReceiveEnergy(side.getOpposite()))
				{
					if(handler.getMaxEnergy() - handler.getEnergy() > 0)
					{
						toReturn.add(acceptor);
					}
				}
			}
			else if(MekanismUtils.useRF() && acceptor instanceof IEnergyReceiver)
			{
				IEnergyReceiver handler = (IEnergyReceiver)acceptor;

				if(handler.canConnectEnergy(side.getOpposite()))
				{
					if(handler.receiveEnergy(side.getOpposite(), 1, true) > 0)
					{
						toReturn.add(acceptor);
					}
				}
			}
			else if(MekanismUtils.useIC2() && acceptor instanceof IEnergySink)
			{
				IEnergySink handler = (IEnergySink)acceptor;

				if(handler.acceptsEnergyFrom(null, side.getOpposite()))
				{
					double demanded = handler.getDemandedEnergy()*Mekanism.FROM_IC2;
					int tier = Math.min(handler.getSinkTier(), 8);
					double max = EnergyNet.instance.getPowerFromTier(tier)*Mekanism.FROM_IC2;
					
					if(Math.min(demanded, max) > 0)
					{
						toReturn.add(acceptor);
					}
				}
			}
		}

		return toReturn;
	}

	@Override
	public void refresh()
	{
		super.refresh();

		needsUpdate = true;
	}
	
	@Override
	public void refresh(IGridTransmitter<EnergyNetwork> transmitter)
	{
		TileEntity[] acceptors = CableUtils.getConnectedEnergyAcceptors(transmitter.getTile());
		
		clearAround(transmitter);

		for(TileEntity acceptor : acceptors)
		{
			ForgeDirection side = ForgeDirection.getOrientation(Arrays.asList(acceptors).indexOf(acceptor));

			if(side != null && acceptor != null && !(acceptor instanceof IGridTransmitter) && transmitter.canConnectToAcceptor(side, true))
			{
				possibleAcceptors.put(Coord4D.get(acceptor), acceptor);
				acceptorDirections.put(acceptor, ForgeDirection.getOrientation(Arrays.asList(acceptors).indexOf(acceptor)));
			}
		}
	}

	public static class EnergyTransferEvent extends Event
	{
		public final EnergyNetwork energyNetwork;

		public final double power;

		public EnergyTransferEvent(EnergyNetwork network, double currentPower)
		{
			energyNetwork = network;
			power = currentPower;
		}
	}

	@Override
	public String toString()
	{
		return "[EnergyNetwork] " + transmitters.size() + " transmitters, " + possibleAcceptors.size() + " acceptors.";
	}

	@Override
	public void onUpdate()
	{
		super.onUpdate();

		clearJoulesTransmitted();

		double currentPowerScale = getPowerScale();

		if(FMLCommonHandler.instance().getEffectiveSide().isServer())
		{
			if(Math.abs(currentPowerScale-lastPowerScale) > 0.01 || (currentPowerScale != lastPowerScale && (currentPowerScale == 0 || currentPowerScale == 1)))
			{
				needsUpdate = true;
			}

			if(needsUpdate)
			{
				MinecraftForge.EVENT_BUS.post(new EnergyTransferEvent(this, currentPowerScale));
				lastPowerScale = currentPowerScale;
				needsUpdate = false;
			}

			if(electricityStored > 0)
			{
				electricityStored -= tickEmit(electricityStored);
			}
		}
	}

	public double getPowerScale()
	{
		return Math.max(jouleBufferLastTick == 0 ? 0 : Math.min(Math.ceil(Math.log10(getPower())*2)/10, 1), getCapacity() == 0 ? 0 : electricityStored/getCapacity());
	}

	public void clearJoulesTransmitted()
	{
		jouleBufferLastTick = electricityStored;
		joulesTransmitted = 0;
	}

	public double getPower()
	{
		return jouleBufferLastTick * 20;
	}

	@Override
	protected EnergyNetwork create(IGridTransmitter<EnergyNetwork>... varTransmitters)
	{
		EnergyNetwork network = new EnergyNetwork(varTransmitters);
		network.clientEnergyScale = clientEnergyScale;
		network.jouleBufferLastTick = jouleBufferLastTick;
		network.joulesTransmitted = joulesTransmitted;
		network.lastPowerScale = lastPowerScale;
		network.electricityStored += electricityStored;
		return network;
	}

	@Override
	protected EnergyNetwork create(Collection<IGridTransmitter<EnergyNetwork>> collection)
	{
		EnergyNetwork network = new EnergyNetwork(collection);
		network.clientEnergyScale = clientEnergyScale;
		network.jouleBufferLastTick = jouleBufferLastTick;
		network.joulesTransmitted = joulesTransmitted;
		network.lastPowerScale = lastPowerScale;
		network.electricityStored += electricityStored;
		network.updateCapacity();
		return network;
	}

	@Override
	protected EnergyNetwork create(Set<EnergyNetwork> networks)
	{
		return new EnergyNetwork(networks);
	}

	@Override
	public TransmissionType getTransmissionType()
	{
		return TransmissionType.ENERGY;
	}

	@Override
	public String getNeededInfo()
	{
		return MekanismUtils.getEnergyDisplay(getEnergyNeeded());
	}

	@Override
	public String getStoredInfo()
	{
		return MekanismUtils.getEnergyDisplay(electricityStored);
	}

	@Override
	public String getFlowInfo()
	{
		return MekanismUtils.getEnergyDisplay(joulesTransmitted) + "/t";
	}
}

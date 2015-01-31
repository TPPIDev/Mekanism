package mekanism.common.tile;

import java.util.ArrayList;
import java.util.EnumSet;

import mekanism.api.Coord4D;
import mekanism.api.energy.ICableOutputter;
import mekanism.api.energy.IStrictEnergyAcceptor;
import mekanism.api.energy.IStrictEnergyStorage;
import mekanism.api.transmitters.IGridTransmitter;
import mekanism.common.ITileNetwork;
import mekanism.common.Mekanism;
import mekanism.common.util.MekanismUtils;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import cpw.mods.fml.common.Optional.Interface;
import cpw.mods.fml.common.Optional.InterfaceList;
import cpw.mods.fml.common.Optional.Method;

import io.netty.buffer.ByteBuf;

import cofh.api.energy.IEnergyHandler;
import ic2.api.energy.EnergyNet;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import ic2.api.energy.tile.IEnergyConductor;
import ic2.api.energy.tile.IEnergySink;
import ic2.api.energy.tile.IEnergySource;
import ic2.api.energy.tile.IEnergyTile;
import ic2.api.tile.IEnergyStorage;

@InterfaceList({
		@Interface(iface = "ic2.api.energy.tile.IEnergySink", modid = "IC2"),
		@Interface(iface = "ic2.api.energy.tile.IEnergySource", modid = "IC2"),
		@Interface(iface = "ic2.api.tile.IEnergyStorage", modid = "IC2"),
		@Interface(iface = "cofh.api.energy.IEnergyHandler", modid = "CoFHAPI|energy"),
})
public abstract class TileEntityElectricBlock extends TileEntityContainerBlock implements ITileNetwork, IStrictEnergyStorage, IEnergyHandler, IEnergySink, IEnergySource, IEnergyStorage, IStrictEnergyAcceptor, ICableOutputter
{
	/** How much energy is stored in this block. */
	public double electricityStored;

	/** Maximum amount of energy this machine can hold. */
	public double MAX_ELECTRICITY;

	/** Is this registered with IC2 */
	public boolean ic2Registered = false;

	/**
	 * The base of all blocks that deal with electricity. It has a facing state, initialized state,
	 * and a current amount of stored energy.
	 * @param name - full name of this block
	 * @param maxEnergy - how much energy this block can store
	 */
	public TileEntityElectricBlock(String name, double maxEnergy)
	{
		super(name);
		MAX_ELECTRICITY = maxEnergy;
	}

	@Method(modid = "IC2")
	public void register()
	{
		if(!worldObj.isRemote)
		{
			TileEntity registered = EnergyNet.instance.getTileEntity(worldObj, xCoord, yCoord, zCoord);
			if(registered != this)
			{
				if(registered instanceof IEnergyTile)
				{
					MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent((IEnergyTile)registered));
				}
				else if(registered == null)
				{
					MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
					ic2Registered = true;
				}
			}
		}
	}

	@Method(modid = "IC2")
	public void deregister()
	{
		if(!worldObj.isRemote)
		{
			TileEntity registered = EnergyNet.instance.getTileEntity(worldObj, xCoord, yCoord, zCoord);
			if(registered instanceof IEnergyTile)
			{
				MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent((IEnergyTile)registered));
			}
		}
	}

	@Override
	public void onUpdate()
	{
		if(!ic2Registered && MekanismUtils.useIC2())
			register();
	}

	public EnumSet<ForgeDirection> getOutputtingSides()
	{
		return EnumSet.noneOf(ForgeDirection.class);
	}

	protected EnumSet<ForgeDirection> getConsumingSides()
	{
		EnumSet set = EnumSet.allOf(ForgeDirection.class);
		set.remove(ForgeDirection.UNKNOWN);
		return set;
	}

	public double getMaxOutput()
	{
		return 0;
	}

	@Override
	public double getEnergy()
	{
		return electricityStored;
	}

	@Override
	public void setEnergy(double energy)
	{
		electricityStored = Math.max(Math.min(energy, getMaxEnergy()), 0);
		MekanismUtils.saveChunk(this);
	}

	@Override
	public double getMaxEnergy()
	{
		return MAX_ELECTRICITY;
	}

	@Override
	public void handlePacketData(ByteBuf dataStream)
	{
		super.handlePacketData(dataStream);
		setEnergy(dataStream.readDouble());
	}

	@Override
	public ArrayList getNetworkedData(ArrayList data)
	{
		super.getNetworkedData(data);
		data.add(getEnergy());
		return data;
	}

	@Override
	public void onChunkUnload()
	{
		if(MekanismUtils.useIC2())
			deregister();

		super.onChunkUnload();
	}

	@Override
	public void invalidate()
	{
		super.invalidate();

		if(MekanismUtils.useIC2())
			deregister();
	}

	@Override
	public void readFromNBT(NBTTagCompound nbtTags)
	{
		super.readFromNBT(nbtTags);

		electricityStored = nbtTags.getDouble("electricityStored");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbtTags)
	{
		super.writeToNBT(nbtTags);

		nbtTags.setDouble("electricityStored", getEnergy());
	}

	/**
	 * Gets the scaled energy level for the GUI.
	 * @param i - multiplier
	 * @return scaled energy
	 */
	public int getScaledEnergyLevel(int i)
	{
		return (int)(getEnergy()*i / getMaxEnergy());
	}

	@Override
	@Method(modid = "CoFHAPI|energy")
	public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate)
	{
		if(getConsumingSides().contains(from))
		{
			double toAdd = (int)Math.min(getMaxEnergy()-getEnergy(), maxReceive*Mekanism.FROM_TE);

			if(!simulate)
			{
				setEnergy(getEnergy() + toAdd);
			}

			return (int)Math.round(toAdd*Mekanism.TO_TE);
		}

		return 0;
	}

	@Override
	@Method(modid = "CoFHAPI|energy")
	public int extractEnergy(ForgeDirection from, int maxExtract, boolean simulate)
	{
		if(getOutputtingSides().contains(from))
		{
			double toSend = Math.min(getEnergy(), Math.min(getMaxOutput(), maxExtract*Mekanism.FROM_TE));

			if(!simulate)
			{
				setEnergy(getEnergy() - toSend);
			}

			return (int)Math.round(toSend*Mekanism.TO_TE);
		}

		return 0;
	}

	@Override
	@Method(modid = "CoFHAPI|energy")
	public boolean canConnectEnergy(ForgeDirection from)
	{
		return getConsumingSides().contains(from) || getOutputtingSides().contains(from);
	}

	@Override
	@Method(modid = "CoFHAPI|energy")
	public int getEnergyStored(ForgeDirection from)
	{
		return (int)Math.round(getEnergy()*Mekanism.TO_TE);
	}

	@Override
	@Method(modid = "CoFHAPI|energy")
	public int getMaxEnergyStored(ForgeDirection from)
	{
		return (int)Math.round(getMaxEnergy()*Mekanism.TO_TE);
	}

	@Override
	@Method(modid = "IC2")
	public int getSinkTier()
	{
		return 4;
	}

	@Override
	@Method(modid = "IC2")
	public int getSourceTier()
	{
		return 4;
	}

	@Override
	@Method(modid = "IC2")
	public void setStored(int energy)
	{
		setEnergy(energy*Mekanism.FROM_IC2);
	}

	@Override
	@Method(modid = "IC2")
	public int addEnergy(int amount)
	{
		setEnergy(getEnergy() + amount*Mekanism.FROM_IC2);
		return (int)Math.round(getEnergy()*Mekanism.TO_IC2);
	}

	@Override
	@Method(modid = "IC2")
	public boolean isTeleporterCompatible(ForgeDirection side)
	{
		return getOutputtingSides().contains(side);
	}

	@Override
	public boolean canOutputTo(ForgeDirection side)
	{
		return getOutputtingSides().contains(side);
	}

	@Override
	@Method(modid = "IC2")
	public boolean acceptsEnergyFrom(TileEntity emitter, ForgeDirection direction)
	{
		return getConsumingSides().contains(direction);
	}

	@Override
	@Method(modid = "IC2")
	public boolean emitsEnergyTo(TileEntity receiver, ForgeDirection direction)
	{
		return getOutputtingSides().contains(direction) && receiver instanceof IEnergyConductor;
	}

	@Override
	@Method(modid = "IC2")
	public int getStored()
	{
		return (int)Math.round(getEnergy() * Mekanism.TO_IC2);
	}

	@Override
	@Method(modid = "IC2")
	public int getCapacity()
	{
		return (int)Math.round(getMaxEnergy() * Mekanism.TO_IC2);
	}

	@Override
	@Method(modid = "IC2")
	public int getOutput()
	{
		return (int)Math.round(getMaxOutput()*Mekanism.TO_IC2);
	}

	@Override
	@Method(modid = "IC2")
	public double getDemandedEnergy()
	{
		return (getMaxEnergy() - getEnergy()) * Mekanism.TO_IC2;
	}

	@Override
	@Method(modid = "IC2")
	public double getOfferedEnergy()
	{
		return Math.min(getEnergy(), getMaxOutput()) * Mekanism.TO_IC2;
	}

	@Override
	public boolean canReceiveEnergy(ForgeDirection side)
	{
		return getConsumingSides().contains(side);
	}

	@Override
	@Method(modid = "IC2")
	public double getOutputEnergyUnitsPerTick()
	{
		return getMaxOutput() * Mekanism.TO_IC2;
	}

	@Override
	@Method(modid = "IC2")
	public double injectEnergy(ForgeDirection direction, double amount, double voltage)
	{
		if(Coord4D.get(this).getFromSide(direction).getTileEntity(worldObj) instanceof IGridTransmitter)
		{
			return amount;
		}

		return amount-transferEnergyToAcceptor(direction, amount*Mekanism.FROM_IC2)*Mekanism.TO_IC2;
	}

	@Override
	@Method(modid = "IC2")
	public void drawEnergy(double amount)
	{
		setEnergy(Math.max(getEnergy() - (amount * Mekanism.FROM_IC2), 0));
	}

	@Override
	public double transferEnergyToAcceptor(ForgeDirection side, double amount)
	{
		if(!(getConsumingSides().contains(side) || side == ForgeDirection.UNKNOWN))
		{
			return 0;
		}

		double toUse = Math.min(getMaxEnergy()-getEnergy(), amount);
		setEnergy(getEnergy() + toUse);

		return toUse;
	}
}

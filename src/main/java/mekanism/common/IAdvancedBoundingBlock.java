package mekanism.common;

import ic2.api.energy.tile.IEnergySink;
import mekanism.api.Coord4D;
import mekanism.api.IFilterAccess;
import mekanism.api.energy.IStrictEnergyAcceptor;
import mekanism.api.energy.IStrictEnergyStorage;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.common.Optional.Interface;
import cpw.mods.fml.common.Optional.InterfaceList;

import buildcraft.api.power.IPowerReceptor;
import cofh.api.energy.IEnergyHandler;
import dan200.computercraft.api.peripheral.IPeripheral;

@InterfaceList({
		@Interface(iface = "buildcraft.api.power.IPowerReceptor", modid = "BuildCraftAPI|power"),
		@Interface(iface = "ic2.api.energy.tile.IEnergySink", modid = "IC2API", striprefs = true),
		@Interface(iface = "cofh.api.energy.IEnergyHandler", modid = "CoFHAPI|energy"),
		@Interface(iface = "dan200.computercraft.api.peripheral.IPeripheral", modid = "ComputerCraft")
})
public interface IAdvancedBoundingBlock extends IBoundingBlock, ISidedInventory, IEnergySink, IStrictEnergyAcceptor, IPowerReceptor, IStrictEnergyStorage, IEnergyHandler, IPeripheral, IFilterAccess
{
	public int[] getBoundSlots(Coord4D location, int side);

	public boolean canBoundInsert(Coord4D location, int i, ItemStack itemstack);

	public boolean canBoundExtract(Coord4D location, int i, ItemStack itemstack, int j);

	public void onPower();

	public void onNoPower();
}

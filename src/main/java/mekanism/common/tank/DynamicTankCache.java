package mekanism.common.tank;

import java.util.HashSet;

import mekanism.api.Coord4D;
import mekanism.common.util.FluidContainerUtils.ContainerEditMode;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fluids.FluidStack;

public class DynamicTankCache
{
	public ItemStack[] inventory = new ItemStack[2];
	public FluidStack fluid;
	public ContainerEditMode editMode = ContainerEditMode.BOTH;
	
	public void apply(SynchronizedTankData data)
	{
		data.inventory = inventory;
		data.fluidStored = fluid;
		data.editMode = editMode;
	}
	
	public void sync(SynchronizedTankData data)
	{
		inventory = data.inventory;
		fluid = data.fluidStored;
		editMode = data.editMode;
	}
	
	public void load(NBTTagCompound nbtTags)
	{
		editMode = ContainerEditMode.values()[nbtTags.getInteger("editMode")];
		
		NBTTagList tagList = nbtTags.getTagList("Items", NBT.TAG_COMPOUND);
		inventory = new ItemStack[2];

		for(int tagCount = 0; tagCount < tagList.tagCount(); tagCount++)
		{
			NBTTagCompound tagCompound = (NBTTagCompound)tagList.getCompoundTagAt(tagCount);
			byte slotID = tagCompound.getByte("Slot");

			if(slotID >= 0 && slotID < 2)
			{
				inventory[slotID] = ItemStack.loadItemStackFromNBT(tagCompound);
			}
		}
		
		if(nbtTags.hasKey("cachedFluid"))
		{
			fluid = FluidStack.loadFluidStackFromNBT(nbtTags.getCompoundTag("cachedFluid"));
		}
	}
	
	public void save(NBTTagCompound nbtTags)
	{
		nbtTags.setInteger("editMode", editMode.ordinal());
		
		NBTTagList tagList = new NBTTagList();

		for(int slotCount = 0; slotCount < 2; slotCount++)
		{
			if(inventory[slotCount] != null)
			{
				NBTTagCompound tagCompound = new NBTTagCompound();
				tagCompound.setByte("Slot", (byte)slotCount);
				inventory[slotCount].writeToNBT(tagCompound);
				tagList.appendTag(tagCompound);
			}
		}

		nbtTags.setTag("Items", tagList);
		
		if(fluid != null)
		{
			nbtTags.setTag("cachedFluid", fluid.writeToNBT(new NBTTagCompound()));
		}
	}

	public HashSet<Coord4D> locations = new HashSet<Coord4D>();
}

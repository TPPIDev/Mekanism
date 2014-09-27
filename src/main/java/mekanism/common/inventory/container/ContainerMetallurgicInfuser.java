package mekanism.common.inventory.container;

import mekanism.api.infuse.InfuseRegistry;
import mekanism.api.infuse.InfusionInput;
import mekanism.common.inventory.slot.SlotEnergy.SlotDischarge;
import mekanism.common.inventory.slot.SlotMachineUpgrade;
import mekanism.common.inventory.slot.SlotOutput;
import mekanism.common.item.ItemMachineUpgrade;
import mekanism.common.recipe.RecipeHandler;
import mekanism.common.recipe.RecipeHandler.Recipe;
import mekanism.common.tile.TileEntityMetallurgicInfuser;
import mekanism.common.util.ChargeUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerMetallurgicInfuser extends Container
{
	private TileEntityMetallurgicInfuser tileEntity;

	public ContainerMetallurgicInfuser(InventoryPlayer inventory, TileEntityMetallurgicInfuser tentity)
	{
		tileEntity = tentity;
		addSlotToContainer(new SlotMachineUpgrade(tentity, 0, 180, 11));
		addSlotToContainer(new Slot(tentity, 1, 17, 35));
		addSlotToContainer(new Slot(tentity, 2, 51, 43));
		addSlotToContainer(new SlotOutput(tentity, 3, 109, 43));
		addSlotToContainer(new SlotDischarge(tentity, 4, 143, 35));
		int slotX;

		for(slotX = 0; slotX < 3; ++slotX)
		{
			for(int slotY = 0; slotY < 9; ++slotY)
			{
				addSlotToContainer(new Slot(inventory, slotY + slotX * 9 + 9, 8 + slotY * 18, 84 + slotX * 18));
			}
		}

		for(slotX = 0; slotX < 9; ++slotX)
		{
			addSlotToContainer(new Slot(inventory, slotX, 8 + slotX * 18, 142));
		}

		tileEntity.open(inventory.player);
		tileEntity.openInventory();
	}

	@Override
	public void onContainerClosed(EntityPlayer entityplayer)
	{
		super.onContainerClosed(entityplayer);

		tileEntity.close(entityplayer);
		tileEntity.closeInventory();
	}

	@Override
	public boolean canInteractWith(EntityPlayer entityplayer)
	{
		return tileEntity.isUseableByPlayer(entityplayer);
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int slotID)
	{
		ItemStack stack = null;
		Slot currentSlot = (Slot)inventorySlots.get(slotID);

		if(currentSlot != null && currentSlot.getHasStack())
		{
			ItemStack slotStack = currentSlot.getStack();
			stack = slotStack.copy();

			if(slotID != 0 && slotID != 1 && slotID != 2 && slotID != 3 && slotID != 4)
			{
				if(InfuseRegistry.getObject(slotStack) != null && (tileEntity.type == null || tileEntity.type == InfuseRegistry.getObject(slotStack).type))
				{
					if(!mergeItemStack(slotStack, 1, 2, false))
					{
						return null;
					}
				}
				else if(slotStack.getItem() instanceof ItemMachineUpgrade)
				{
					if(!mergeItemStack(slotStack, 0, 1, false))
					{
						return null;
					}
				}
				else if(ChargeUtils.canBeDischarged(slotStack))
				{
					if(!mergeItemStack(slotStack, 4, 5, false))
					{
						return null;
					}
				}
				else if(isInputItem(slotStack))
				{
					if(!mergeItemStack(slotStack, 2, 3, false))
					{
						return null;
					}
				}
				else {
					if(slotID >= 5 && slotID <= 31)
					{
						if(!mergeItemStack(slotStack, 32, inventorySlots.size(), false))
						{
							return null;
						}
					}
					else if(slotID > 31)
					{
						if(!mergeItemStack(slotStack, 5, 31, false))
						{
							return null;
						}
					}
					else {
						if(!mergeItemStack(slotStack, 5, inventorySlots.size(), true))
						{
							return null;
						}
					}
				}
			}
			else {
				if(!mergeItemStack(slotStack, 5, inventorySlots.size(), true))
				{
					return null;
				}
			}

			if(slotStack.stackSize == 0)
			{
				currentSlot.putStack((ItemStack)null);
			}
			else {
				currentSlot.onSlotChanged();
			}

			if(slotStack.stackSize == stack.stackSize)
			{
				return null;
			}

			currentSlot.onPickupFromSlot(player, slotStack);
		}

		return stack;
	}

	public boolean isInputItem(ItemStack itemStack)
	{
		if(tileEntity.type != null)
		{
			if(RecipeHandler.getMetallurgicInfuserOutput(InfusionInput.getInfusion(tileEntity.type, tileEntity.infuseStored, itemStack), false) != null)
			{
				return true;
			}
		}
		else {
			for(Object obj : Recipe.METALLURGIC_INFUSER.get().keySet())
			{
				InfusionInput input = (InfusionInput)obj;
				if(input.inputStack.isItemEqual(itemStack))
				{
					return true;
				}
			}
		}

		return false;
	}
}

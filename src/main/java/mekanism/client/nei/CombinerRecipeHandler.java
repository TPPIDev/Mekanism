package mekanism.client.nei;

import java.util.List;
import java.util.Set;

import mekanism.api.ListUtils;
import mekanism.api.gas.Gas;
import mekanism.client.gui.GuiCombiner;
import mekanism.client.gui.GuiProgress.ProgressBar;
import mekanism.common.recipe.RecipeHandler.Recipe;
import mekanism.common.util.MekanismUtils;

import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

public class CombinerRecipeHandler extends AdvancedMachineRecipeHandler
{
	@Override
	public String getRecipeName()
	{
		return MekanismUtils.localize("tile.MachineBlock.Combiner.name");
	}

	@Override
	public String getRecipeId()
	{
		return "mekanism.combiner";
	}

	@Override
	public String getOverlayIdentifier()
	{
		return "combiner";
	}

	@Override
	public Set getRecipes()
	{
		return Recipe.COMBINER.get().entrySet();
	}
	
	@Override
	public ProgressBar getProgressType()
	{
		return ProgressBar.STONE;
	}

	@Override
	public List<ItemStack> getFuelStacks(Gas gasType)
	{
		return ListUtils.asList(new ItemStack(Blocks.cobblestone));
	}

	@Override
	public Class getGuiClass()
	{
		return GuiCombiner.class;
	}
}

package mekanism.common.tile;

import java.util.Map;

import mekanism.common.Mekanism;
import mekanism.common.block.BlockMachine.MachineType;
import mekanism.common.recipe.RecipeHandler.Recipe;

public class TileEntityEnrichmentChamber extends TileEntityElectricMachine
{
	public TileEntityEnrichmentChamber()
	{
		super("Chamber.ogg", "EnrichmentChamber", Mekanism.enrichmentChamberUsage, 200, MachineType.ENRICHMENT_CHAMBER.baseEnergy);
	}

	@Override
	public Map getRecipes()
	{
		return Recipe.ENRICHMENT_CHAMBER.get();
	}

	@Override
	public float getVolumeMultiplier()
	{
		return 0.3F;
	}
}

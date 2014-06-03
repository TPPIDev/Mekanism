package mekanism.generators.common;

import java.io.DataOutputStream;
import java.io.IOException;

import mekanism.api.gas.FuelHandler;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasRegistry;
import mekanism.common.IModule;
import mekanism.common.Mekanism;
import mekanism.common.Version;
import mekanism.common.item.ItemMekanism;
import mekanism.common.recipe.MekanismRecipe;
import mekanism.generators.common.block.BlockGenerator;
import mekanism.generators.common.item.ItemBlockGenerator;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;

import buildcraft.api.fuels.IronEngineFuel;
import com.google.common.io.ByteArrayDataInput;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.registry.GameRegistry;

@Mod(modid = "MekanismGenerators", name = "MekanismGenerators", version = "6.1.2.TPPI", dependencies = "required-after:Mekanism")
@NetworkMod(clientSideRequired = true, serverSideRequired = false)
public class MekanismGenerators implements IModule
{
	@SidedProxy(clientSide = "mekanism.generators.client.GeneratorsClientProxy", serverSide = "mekanism.generators.common.GeneratorsCommonProxy")
	public static GeneratorsCommonProxy proxy;
	
	@Instance("MekanismGenerators")
	public static MekanismGenerators instance;
	
	/** MekanismGenerators version number */
	public static Version versionNumber = new Version(6, 0, 4);
	
	//Items
	public static Item SolarPanel;
	
	//Blocks
	public static Block Generator;
	
	//Block IDs
	public static int generatorID;
	
	//Generation Configuration
	public static double advancedSolarGeneration;
	public static double bioGeneration;
	public static double passiveHeatGen;
	public static double solarGeneration;
	public static double windGeneration;

	@EventHandler
	public void postInit(FMLPostInitializationEvent event)
	{
		for(String s : IronEngineFuel.fuels.keySet())
		{
			Fluid f = FluidRegistry.getFluid(s);
			if(!(f == null || GasRegistry.containsGas(s)))
			{
				GasRegistry.register(new Gas(f));
			}
		}

		IronEngineFuel.addFuel("ethene", (int)(240 * Mekanism.TO_BC), 40* FluidContainerRegistry.BUCKET_VOLUME);
	}
	
	@EventHandler
	public void init(FMLInitializationEvent event)
	{
		//Add this module to the core list
		Mekanism.modulesLoaded.add(this);
		
		//Set up the GUI handler
		NetworkRegistry.instance().registerGuiHandler(this, new GeneratorsGuiHandler());
		
		//Load the proxy
		proxy.loadConfiguration();
		proxy.registerSpecialTileEntities();
		proxy.registerRenderInformation();
		
		//Load this module
		addBlocks();
		addItems();
		addRecipes();
		
		//Finalization
		Mekanism.logger.info("[MekanismGenerators] Loaded module.");
	}
	
	public void addRecipes()
	{
		CraftingManager.getInstance().getRecipeList().add(new MekanismRecipe(new ItemStack(Generator, 1, 0), new Object[] {
			"III", "WOW", "CFC", Character.valueOf('I'), "ingotIron", Character.valueOf('C'), "ingotCopper", Character.valueOf('O'), "ingotOsmium", Character.valueOf('F'), Block.furnaceIdle, Character.valueOf('W'), Block.planks
		}));
		CraftingManager.getInstance().getRecipeList().add(new MekanismRecipe(new ItemStack(Generator, 1, 1), new Object[] {
			"SSS", "AIA", "PEP", Character.valueOf('S'), SolarPanel, Character.valueOf('A'), Mekanism.EnrichedAlloy, Character.valueOf('I'), Item.ingotIron, Character.valueOf('P'), "dustOsmium", Character.valueOf('E'), Mekanism.EnergyTablet.getUnchargedItem()
		}));
		CraftingManager.getInstance().getRecipeList().add(new MekanismRecipe(new ItemStack(Generator, 1, 5), new Object[] {
			"SES", "SES", "III", Character.valueOf('S'), new ItemStack(Generator, 1, 1), Character.valueOf('E'), Mekanism.EnrichedAlloy, Character.valueOf('I'), Item.ingotIron
		}));
		CraftingManager.getInstance().getRecipeList().add(new MekanismRecipe(new ItemStack(Generator, 1, 4), new Object[] {
			"RER", "BCB", "NEN", Character.valueOf('R'), Item.redstone, Character.valueOf('E'), Mekanism.EnrichedAlloy, Character.valueOf('B'), Mekanism.BioFuel, Character.valueOf('C'), "circuitBasic", Character.valueOf('N'), Item.ingotIron
		}));
		CraftingManager.getInstance().getRecipeList().add(new MekanismRecipe(new ItemStack(Generator, 1, 3), new Object[] {
			"PEP", "ICI", "PEP", Character.valueOf('P'), "ingotOsmium", Character.valueOf('E'), Mekanism.EnrichedAlloy, Character.valueOf('I'), new ItemStack(Mekanism.BasicBlock, 1, 8), Character.valueOf('C'), Mekanism.ElectrolyticCore
		}));
		CraftingManager.getInstance().getRecipeList().add(new MekanismRecipe(new ItemStack(SolarPanel), new Object[] {
			"GGG", "RAR", "PPP", Character.valueOf('G'), Block.thinGlass, Character.valueOf('R'), Item.redstone, Character.valueOf('A'), Mekanism.EnrichedAlloy, Character.valueOf('P'), "ingotOsmium"
		}));
		CraftingManager.getInstance().getRecipeList().add(new MekanismRecipe(new ItemStack(Generator, 1, 6), new Object[] {
			" O ", "OAO", "ECE", Character.valueOf('O'), "ingotOsmium", Character.valueOf('A'), Mekanism.EnrichedAlloy, Character.valueOf('E'), Mekanism.EnergyTablet.getUnchargedItem(), Character.valueOf('C'), "circuitBasic"
		}));

		FuelHandler.addGas(GasRegistry.getGas("ethene"), 40, Mekanism.FROM_H2 + bioGeneration * 80); //1mB hydrogen + 2*bioFuel/tick*200ticks/100mB * 20x efficiency bonus

	}
	
	public void addBlocks()
	{
		//Declarations
		Generator = new BlockGenerator(generatorID).setUnlocalizedName("Generator");
		
		Item.itemsList[generatorID] = new ItemBlockGenerator(generatorID - 256, Generator).setUnlocalizedName("Generator");
	}
	
	public void addItems()
	{
		//Declarations
		Mekanism.configuration.load();
		SolarPanel = new ItemMekanism(Mekanism.configuration.getItem("SolarPanel", 11300).getInt()).setUnlocalizedName("SolarPanel");
		Mekanism.configuration.save();
		
		//Registrations
		GameRegistry.registerItem(SolarPanel, "SolarPanel");
	}

	@Override
	public Version getVersion() 
	{
		return versionNumber;
	}

	@Override
	public String getName()
	{
		return "Generators";
	}
	
	@Override
	public void writeConfig(DataOutputStream dataStream) throws IOException
	{
		dataStream.writeDouble(advancedSolarGeneration);
		dataStream.writeDouble(bioGeneration);
		dataStream.writeDouble(passiveHeatGen);
		dataStream.writeDouble(solarGeneration);
		dataStream.writeDouble(windGeneration);
	}

	@Override
	public void readConfig(ByteArrayDataInput dataStream) throws IOException
	{
		advancedSolarGeneration = dataStream.readDouble();
		bioGeneration = dataStream.readDouble();
		passiveHeatGen = dataStream.readDouble();
		solarGeneration = dataStream.readDouble();
		windGeneration = dataStream.readDouble();
	}
}

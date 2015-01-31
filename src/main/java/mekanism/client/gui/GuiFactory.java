package mekanism.client.gui;

import java.util.List;

import mekanism.api.ListUtils;
import mekanism.api.gas.GasStack;
import mekanism.client.gui.GuiEnergyInfo.IInfoHandler;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.IFactory.RecipeType;
import mekanism.common.Tier.FactoryTier;
import mekanism.common.inventory.container.ContainerFactory;
import mekanism.common.tile.TileEntityFactory;
import mekanism.common.util.MekanismUtils;

import net.minecraft.entity.player.InventoryPlayer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import org.lwjgl.opengl.GL11;

@SideOnly(Side.CLIENT)
public class GuiFactory extends GuiMekanism
{
	public TileEntityFactory tileEntity;

	public GuiFactory(InventoryPlayer inventory, TileEntityFactory tentity)
	{
		super(tentity, new ContainerFactory(inventory, tentity));
		tileEntity = tentity;

		ySize += 11;

		guiElements.add(new GuiRedstoneControl(this, tileEntity, tileEntity.tier.guiLocation));
		guiElements.add(new GuiUpgradeManagement(this, tileEntity, tileEntity.tier.guiLocation));
		guiElements.add(new GuiRecipeType(this, tileEntity, tileEntity.tier.guiLocation));
		guiElements.add(new GuiConfigurationTab(this, tileEntity, tileEntity.tier.guiLocation));
		guiElements.add(new GuiSortingTab(this, tileEntity, tileEntity.tier.guiLocation));
		guiElements.add(new GuiEnergyInfo(new IInfoHandler() {
			@Override
			public List<String> getInfo()
			{
				String multiplier = MekanismUtils.getEnergyDisplay(MekanismUtils.getEnergyPerTick(tileEntity, tileEntity.ENERGY_PER_TICK));
				return ListUtils.asList("Using: " + multiplier + "/t", "Needed: " + MekanismUtils.getEnergyDisplay(tileEntity.getMaxEnergy()-tileEntity.getEnergy()));
			}
		}, this, tileEntity.tier.guiLocation));
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY)
	{
		int xAxis = (mouseX - (width - xSize) / 2);
		int yAxis = (mouseY - (height - ySize) / 2);

		fontRendererObj.drawString(tileEntity.getInventoryName(), 48, 4, 0x404040);
		fontRendererObj.drawString(MekanismUtils.localize("container.inventory"), 8, (ySize - 93) + 2, 0x404040);
		fontRendererObj.drawString(RecipeType.values()[tileEntity.recipeType].getName(), 104, (ySize - 93) + 2, 0x404040);

		if(xAxis >= 165 && xAxis <= 169 && yAxis >= 17 && yAxis <= 69)
		{
			drawCreativeTabHoveringText(MekanismUtils.getEnergyDisplay(tileEntity.getEnergy()), xAxis, yAxis);
		}

		if(xAxis >= 8 && xAxis <= 168 && yAxis >= 78 && yAxis <= 83)
		{
			drawCreativeTabHoveringText(tileEntity.gasTank.getGas() != null ? tileEntity.gasTank.getGas().getGas().getLocalizedName() + ": " + tileEntity.gasTank.getStored() : MekanismUtils.localize("gui.none"), xAxis, yAxis);
		}

		super.drawGuiContainerForegroundLayer(mouseX, mouseY);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTick, int mouseX, int mouseY)
	{
		super.drawGuiContainerBackgroundLayer(partialTick, mouseX, mouseY);

		mc.renderEngine.bindTexture(tileEntity.tier.guiLocation);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		int guiWidth = (width - xSize) / 2;
		int guiHeight = (height - ySize) / 2;
		drawTexturedModalRect(guiWidth, guiHeight, 0, 0, xSize, ySize);

		int xAxis = mouseX - guiWidth;
		int yAxis = mouseY - guiHeight;

		int displayInt;

		displayInt = tileEntity.getScaledEnergyLevel(52);
		drawTexturedModalRect(guiWidth + 165, guiHeight + 17 + 52 - displayInt, 176, 52 - displayInt, 4, displayInt);

		if(tileEntity.tier == FactoryTier.BASIC)
		{
			for(int i = 0; i < tileEntity.tier.processes; i++)
			{
				int xPos = 59 + (i*38);

				displayInt = tileEntity.getScaledProgress(20, i);
				drawTexturedModalRect(guiWidth + xPos, guiHeight + 33, 176, 52, 8, displayInt);
			}
		}
		else if(tileEntity.tier == FactoryTier.ADVANCED)
		{
			for(int i = 0; i < tileEntity.tier.processes; i++)
			{
				int xPos = 39 + (i*26);

				displayInt = tileEntity.getScaledProgress(20, i);
				drawTexturedModalRect(guiWidth + xPos, guiHeight + 33, 176, 52, 8, displayInt);
			}
		}
		else if(tileEntity.tier == FactoryTier.ELITE)
		{
			for(int i = 0; i < tileEntity.tier.processes; i++)
			{
				int xPos = 33 + (i*19);

				displayInt = tileEntity.getScaledProgress(20, i);
				drawTexturedModalRect(guiWidth + xPos, guiHeight + 33, 176, 52, 8, displayInt);
			}
		}

		if(tileEntity.getScaledGasLevel(160) > 0)
		{
			displayGauge(8, 78, tileEntity.getScaledGasLevel(160), 5, tileEntity.gasTank.getGas());
		}
	}

	public void displayGauge(int xPos, int yPos, int sizeX, int sizeY, GasStack gas)
	{
		if(gas == null)
		{
			return;
		}

		int guiWidth = (width - xSize) / 2;
		int guiHeight = (height - ySize) / 2;

		mc.renderEngine.bindTexture(MekanismRenderer.getBlocksTexture());
		drawTexturedModelRectFromIcon(guiWidth + xPos, guiHeight + yPos, gas.getGas().getIcon(), sizeX, sizeY);
	}
}

package mekanism.client.gui;

import codechicken.lib.vec.Rectangle4i;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;

public abstract class GuiGauge<T> extends GuiElement
{
	protected int xLocation;
	protected int yLocation;

	protected int width;
	protected int height;

	protected int number;
	protected boolean dummy;
	
	protected T dummyType;

	public GuiGauge(Type type, IGuiWrapper gui, ResourceLocation def, int x, int y)
	{
		super(MekanismUtils.getResource(ResourceType.GUI_ELEMENT, type.textureLocation), gui, def);

		xLocation = x;
		yLocation = y;

		width = type.width;
		height = type.height;
		number = type.number;
	}

	public abstract int getScaledLevel();

	public abstract IIcon getIcon();

	public abstract String getTooltipText();

	@Override
	public void renderBackground(int xAxis, int yAxis, int guiWidth, int guiHeight)
	{
		mc.renderEngine.bindTexture(RESOURCE);

		guiObj.drawTexturedRect(guiWidth + xLocation, guiHeight + yLocation, 0, 0, width, height);
		
		if(!dummy)
		{
			renderScale(xAxis, yAxis, guiWidth, guiHeight);
		}

		mc.renderEngine.bindTexture(defaultLocation);
	}
	
	public void renderScale(int xAxis, int yAxis, int guiWidth, int guiHeight)
	{
		if(getScaledLevel() == 0 || getIcon() == null)
		{
			guiObj.drawTexturedRect(guiWidth + xLocation, guiHeight + yLocation, width, 0, width, height);
			return;
		}
		
		int scale = getScaledLevel();
		int start = 0;
		
		while(scale > 0)
		{
			int renderRemaining = 0;

			if(scale > 16)
			{
				renderRemaining = 16;
				scale -= 16;
			}
			else {
				renderRemaining = scale;
				scale = 0;
			}

			mc.renderEngine.bindTexture(MekanismRenderer.getBlocksTexture());

			for(int i = 0; i < number; i++)
			{
				guiObj.drawTexturedRectFromIcon(guiWidth + xLocation + 16*i + 1, guiHeight + yLocation + height - renderRemaining - start - 1, getIcon(), 16, renderRemaining);
			}

			start+=16;

			if(renderRemaining == 0 || scale == 0)
			{
				break;
			}
		}

		mc.renderEngine.bindTexture(RESOURCE);
		guiObj.drawTexturedRect(guiWidth + xLocation, guiHeight + yLocation, width, 0, width, height);
	}

	@Override
	public void renderForeground(int xAxis, int yAxis)
	{
		if(xAxis >= xLocation + 1 && xAxis <= xLocation + width - 1 && yAxis >= yLocation + 1 && yAxis <= yLocation + height - 1)
		{
			guiObj.displayTooltip(getTooltipText(), xAxis, yAxis);
		}
	}

	@Override
	public void preMouseClicked(int xAxis, int yAxis, int button)
	{

	}

	@Override
	public void mouseClicked(int xAxis, int yAxis, int button)
	{

	}

	public void setDummyType(T type)
	{
		dummyType = type;
	}
	
	@Override
	public Rectangle4i getBounds(int guiWidth, int guiHeight)
	{
		return new Rectangle4i(guiWidth + xLocation, guiHeight + yLocation, width, height);
	}
	
	public static enum Type
	{
		STANDARD(18, 60, 1, "GuiGaugeStandard.png"),
		WIDE(66, 50, 4, "GuiGaugeWide.png"),
		SMALL(18, 30, 1, "GuiGaugeSmall.png");

		public int width;
		public int height;
		public int number;
		public String textureLocation;

		private Type(int w, int h, int n, String t)
		{
			width = w;
			height = h;
			number = n;
			textureLocation = t;
		}
	}
}

package mekanism.client.render;

import java.util.HashMap;
import java.util.Map;

import mekanism.api.Coord4D;
import mekanism.client.model.ModelTransporterBox;
import mekanism.client.render.MekanismRenderer.DisplayInteger;
import mekanism.client.render.MekanismRenderer.Model3D;
import mekanism.common.item.ItemConfigurator;
import mekanism.common.multipart.PartDiversionTransporter;
import mekanism.common.multipart.PartLogisticalTransporter;
import mekanism.common.multipart.PartMechanicalPipe;
import mekanism.common.multipart.PartPressurizedTube;
import mekanism.common.multipart.PartSidedPipe;
import mekanism.common.multipart.PartSidedPipe.ConnectionType;
import mekanism.common.multipart.PartTransmitter;
import mekanism.common.multipart.PartUniversalCable;
import mekanism.common.multipart.TransmitterType;
import mekanism.common.multipart.TransmitterType.Size;
import mekanism.common.transporter.TransporterStack;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import mekanism.common.util.TransporterUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;

import org.lwjgl.opengl.GL11;

import codechicken.lib.colour.Colour;
import codechicken.lib.colour.ColourRGBA;
import codechicken.lib.lighting.LightModel;
import codechicken.lib.lighting.LightModel.Light;
import codechicken.lib.render.CCModel;
import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.ColourMultiplier;
import codechicken.lib.render.TextureUtils;
import codechicken.lib.render.TextureUtils.IIconSelfRegister;
import codechicken.lib.render.uv.IconTransformation;
import codechicken.lib.vec.Translation;
import codechicken.lib.vec.Vector3;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderPartTransmitter implements IIconSelfRegister
{
	public static RenderPartTransmitter INSTANCE;

	public static Map<String, CCModel> small_models;
	public static Map<String, CCModel> large_models;
	public static Map<String, CCModel> contents_models;

	private static final int stages = 100;
	private static final double height = 0.45;
	private static final double offset = 0.015;

	private ModelTransporterBox modelBox = new ModelTransporterBox();

	private HashMap<ForgeDirection, HashMap<Fluid, DisplayInteger[]>> cachedLiquids = new HashMap<ForgeDirection, HashMap<Fluid, DisplayInteger[]>>();
	private HashMap<ForgeDirection, HashMap<Integer, DisplayInteger>> cachedOverlays = new HashMap<ForgeDirection, HashMap<Integer, DisplayInteger>>();

	private Minecraft mc = Minecraft.getMinecraft();

	private EntityItem entityItem = new EntityItem(null);
	private RenderItem renderer = (RenderItem)RenderManager.instance.getEntityClassRenderObject(EntityItem.class);

	public static RenderPartTransmitter getInstance()
	{
		return INSTANCE;
	}

	public static void init()
	{
		INSTANCE = new RenderPartTransmitter();
		TextureUtils.addIconRegistrar(INSTANCE);

		small_models = CCModel.parseObjModels(MekanismUtils.getResource(ResourceType.MODEL, "transmitter_small.obj"), 7, null);

		for(Map.Entry<String, CCModel> e : small_models.entrySet())
		{
			e.setValue(e.getValue().twoFacedCopy().apply(new Translation(Vector3.center)).shrinkUVs(0.0005));
			e.getValue().computeLighting(LightModel.standardLightModel);
		}

		large_models = CCModel.parseObjModels(MekanismUtils.getResource(ResourceType.MODEL, "transmitter_large.obj"), 7, null);

		for(Map.Entry<String, CCModel> e : large_models.entrySet())
		{
			e.setValue(e.getValue().twoFacedCopy().apply(new Translation(Vector3.center)).shrinkUVs(0.0005));
			e.getValue().computeLighting(LightModel.standardLightModel);
		}

		contents_models = CCModel.parseObjModels(MekanismUtils.getResource(ResourceType.MODEL, "transmitter_contents.obj"), 7, null);
		LightModel interiorLightModel = new LightModel()
				.setAmbient(new Vector3(0.6, 0.6, 0.6))
				.addLight(new Light(new Vector3(0.3, 1, -0.7))
						.setDiffuse(new Vector3(0.6, 0.6, 0.6)))
				.addLight(new Light(new Vector3(-0.3, 1, 0.7))
						.setDiffuse(new Vector3(0.6, 0.6, 0.6)));

		for(CCModel c : contents_models.values())
		{
			c.apply(new Translation(Vector3.center));
			c.computeLighting(interiorLightModel);
			c.shrinkUVs(0.0005);
		}
	}

	private void push()
	{
		GL11.glPushMatrix();
		GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glDisable(GL11.GL_LIGHTING);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
	}

	private void pop()
	{
		GL11.glPopAttrib();
		GL11.glPopMatrix();
	}

	public void renderItem(TransmitterType type)
	{
		CCRenderState.reset();
		CCRenderState.startDrawing();
		
		for(ForgeDirection side : ForgeDirection.VALID_DIRECTIONS)
		{
			renderSide(side, type);
		}
		
		CCRenderState.draw();
	}

	public void renderContents(PartLogisticalTransporter transporter, float partialTick, Vector3 vec)
	{
		GL11.glPushMatrix();

		entityItem.age = 0;
		entityItem.hoverStart = 0;

		entityItem.setPosition(transporter.x() + 0.5, transporter.y() + 0.5, transporter.z() + 0.5);
		entityItem.worldObj = transporter.world();

		for(TransporterStack stack : transporter.transit)
		{
			if(stack != null)
			{
				GL11.glPushMatrix();
				entityItem.setEntityItemStack(stack.itemStack);

				float[] pos = TransporterUtils.getStackPosition(transporter, stack, partialTick*PartLogisticalTransporter.SPEED);

				GL11.glTranslated(vec.x + pos[0], vec.y + pos[1] - entityItem.yOffset, vec.z + pos[2]);
				GL11.glScalef(0.75F, 0.75F, 0.75F);

				renderer.doRender(entityItem, 0, 0, 0, 0, 0);
				GL11.glPopMatrix();

				if(stack.color != null)
				{
					CCRenderState.changeTexture(MekanismUtils.getResource(ResourceType.RENDER, "TransporterBox.png"));
					GL11.glPushMatrix();
					MekanismRenderer.glowOn();
					GL11.glDisable(GL11.GL_CULL_FACE);
					GL11.glColor4f(stack.color.getColor(0), stack.color.getColor(1), stack.color.getColor(2), 1.0F);
					GL11.glTranslatef((float)(vec.x + pos[0]), (float)(vec.y + pos[1] - entityItem.yOffset - ((stack.itemStack.getItem() instanceof ItemBlock) ? 0.1 : 0)), (float)(vec.z + pos[2]));
					modelBox.render(0.0625F);
					MekanismRenderer.glowOff();
					GL11.glPopMatrix();
				}
			}
		}

		if(transporter instanceof PartDiversionTransporter)
		{
			EntityPlayer player = mc.thePlayer;
			World world = mc.thePlayer.worldObj;
			ItemStack itemStack = player.getCurrentEquippedItem();
			MovingObjectPosition pos = player.rayTrace(8.0D, 1.0F);

			if(pos != null && itemStack != null && itemStack.getItem() instanceof ItemConfigurator)
			{
				int xPos = MathHelper.floor_double(pos.blockX);
				int yPos = MathHelper.floor_double(pos.blockY);
				int zPos = MathHelper.floor_double(pos.blockZ);

				Coord4D obj = new Coord4D(xPos, yPos, zPos, transporter.world().provider.dimensionId);

				if(obj.equals(Coord4D.get(transporter.tile())))
				{
					int mode = ((PartDiversionTransporter)transporter).modes[pos.sideHit];
					ForgeDirection side = ForgeDirection.getOrientation(pos.sideHit);

					pushTransporter();

					GL11.glColor4f(1.0F, 1.0F, 1.0F, 0.8F);

					CCRenderState.changeTexture(mode == 0 ? MekanismRenderer.getItemsTexture() : MekanismRenderer.getBlocksTexture());
					GL11.glTranslatef((float)vec.x, (float)vec.y, (float)vec.z);
					GL11.glScalef(0.5F, 0.5F, 0.5F);
					GL11.glTranslatef(0.5F, 0.5F, 0.5F);

					int display = getOverlayDisplay(world, side, mode).display;
					GL11.glCallList(display);

					popTransporter();
				}
			}
		}

		GL11.glPopMatrix();
	}

	public void renderContents(PartUniversalCable cable, Vector3 pos)
	{
		if(cable.currentPower == 0)
		{
			return;
		}

		GL11.glPushMatrix();
		CCRenderState.reset();
		CCRenderState.useNormals = true;
		CCRenderState.startDrawing();
		GL11.glTranslated(pos.x, pos.y, pos.z);

		for(ForgeDirection side : ForgeDirection.VALID_DIRECTIONS)
		{
			renderEnergySide(side, cable);
		}

		MekanismRenderer.glowOn();
		MekanismRenderer.cullFrontFace();

		CCRenderState.draw();

		MekanismRenderer.disableCullFace();
		MekanismRenderer.glowOff();

		GL11.glPopMatrix();
	}

	public void renderContents(PartMechanicalPipe pipe, Vector3 pos)
	{
		float targetScale = pipe.getTransmitterNetwork().fluidScale;

		if(Math.abs(pipe.currentScale - targetScale) > 0.01)
		{
			pipe.currentScale = (12 * pipe.currentScale + targetScale) / 13;
		}
		else {
			pipe.currentScale = targetScale;
		}

		Fluid fluid = pipe.getTransmitterNetwork().refFluid;
		float scale = pipe.currentScale;

		if(scale > 0.01 && fluid != null)
		{
			push();

			MekanismRenderer.glowOn(fluid.getLuminosity());

			CCRenderState.changeTexture(MekanismRenderer.getBlocksTexture());
			GL11.glTranslated(pos.x, pos.y, pos.z);

			boolean gas = fluid.isGaseous();

			for(ForgeDirection side : ForgeDirection.VALID_DIRECTIONS)
			{
				if(pipe.getConnectionType(side) == ConnectionType.NORMAL)
				{
					DisplayInteger[] displayLists = getListAndRender(side, fluid);

					if(displayLists != null)
					{
						if(!gas)
						{
							displayLists[Math.max(3, (int)((float)scale*(stages-1)))].render();
						}
						else {
							GL11.glColor4f(1F, 1F, 1F, scale);
							displayLists[stages-1].render();
						}
					}
				}
				else if(pipe.getConnectionType(side) != ConnectionType.NONE) 
				{
					GL11.glCullFace(GL11.GL_FRONT);
					CCRenderState.startDrawing();
					renderFluidInOut(side, pipe);
					CCRenderState.draw();
					GL11.glCullFace(GL11.GL_BACK);
				}
			}

			DisplayInteger[] displayLists = getListAndRender(ForgeDirection.UNKNOWN, fluid);

			if(displayLists != null)
			{
				if(!gas)
				{
					displayLists[Math.max(3, (int)((float)scale*(stages-1)))].render();
				}
				else {
					GL11.glColor4f(1F, 1F, 1F, scale);
					displayLists[stages-1].render();
				}
			}

			MekanismRenderer.glowOff();

			pop();
		}

	}

	private DisplayInteger[] getListAndRender(ForgeDirection side, Fluid fluid)
	{
		if(side == null || fluid == null || fluid.getIcon() == null)
		{
			return null;
		}

		if(cachedLiquids.containsKey(side) && cachedLiquids.get(side).containsKey(fluid))
		{
			return cachedLiquids.get(side).get(fluid);
		}

		Model3D toReturn = new Model3D();
		toReturn.baseBlock = Blocks.water;
		toReturn.setTexture(fluid.getIcon());

		toReturn.setSideRender(side, false);
		toReturn.setSideRender(side.getOpposite(), false);

		DisplayInteger[] displays = new DisplayInteger[stages];

		if(cachedLiquids.containsKey(side))
		{
			cachedLiquids.get(side).put(fluid, displays);
		}
		else {
			HashMap<Fluid, DisplayInteger[]> map = new HashMap<Fluid, DisplayInteger[]>();
			map.put(fluid, displays);
			cachedLiquids.put(side, map);
		}

		MekanismRenderer.colorFluid(fluid);

		for(int i = 0; i < stages; i++)
		{
			displays[i] = DisplayInteger.createAndStart();

			switch(side)
			{
				case UNKNOWN:
				{
					toReturn.minX = 0.25 + offset;
					toReturn.minY = 0.25 + offset;
					toReturn.minZ = 0.25 + offset;

					toReturn.maxX = 0.75 - offset;
					toReturn.maxY = 0.25 + offset + ((float)i / (float)stages)*height;
					toReturn.maxZ = 0.75 - offset;
					break;
				}
				case DOWN:
				{
					toReturn.minX = 0.5 - (((float)i / (float)stages)*height)/2;
					toReturn.minY = 0.0;
					toReturn.minZ = 0.5 - (((float)i / (float)stages)*height)/2;

					toReturn.maxX = 0.5 + (((float)i / (float)stages)*height)/2;
					toReturn.maxY = 0.25 + offset;
					toReturn.maxZ = 0.5 + (((float)i / (float)stages)*height)/2;
					break;
				}
				case UP:
				{
					toReturn.minX = 0.5 - (((float)i / (float)stages)*height)/2;
					toReturn.minY = 0.25 - offset + ((float)i / (float)stages)*height;
					toReturn.minZ = 0.5 - (((float)i / (float)stages)*height)/2;

					toReturn.maxX = 0.5 + (((float)i / (float)stages)*height)/2;
					toReturn.maxY = 1.0;
					toReturn.maxZ = 0.5 + (((float)i / (float)stages)*height)/2;
					break;
				}
				case NORTH:
				{
					toReturn.minX = 0.25 + offset;
					toReturn.minY = 0.25 + offset;
					toReturn.minZ = 0.0;

					toReturn.maxX = 0.75 - offset;
					toReturn.maxY = 0.25 + offset + ((float)i / (float)stages)*height;
					toReturn.maxZ = 0.25 + offset;
					break;
				}
				case SOUTH:
				{
					toReturn.minX = 0.25 + offset;
					toReturn.minY = 0.25 + offset;
					toReturn.minZ = 0.75 - offset;

					toReturn.maxX = 0.75 - offset;
					toReturn.maxY = 0.25 + offset + ((float)i / (float)stages)*height;
					toReturn.maxZ = 1.0;
					break;
				}
				case WEST:
				{
					toReturn.minX = 0.0;
					toReturn.minY = 0.25 + offset;
					toReturn.minZ = 0.25 + offset;

					toReturn.maxX = 0.25 + offset;
					toReturn.maxY = 0.25 + offset + ((float)i / (float)stages)*height;
					toReturn.maxZ = 0.75 - offset;
					break;
				}
				case EAST:
				{
					toReturn.minX = 0.75 - offset;
					toReturn.minY = 0.25 + offset;
					toReturn.minZ = 0.25 + offset;

					toReturn.maxX = 1.0;
					toReturn.maxY = 0.25 + offset + ((float)i / (float)stages)*height;
					toReturn.maxZ = 0.75 - offset;
					break;
				}
			}

			MekanismRenderer.renderObject(toReturn);
			displays[i].endList();
		}

		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);

		return displays;
	}

	public void renderContents(PartPressurizedTube tube, Vector3 pos)
	{
		if(tube.getTransmitterNetwork().refGas == null || tube.getTransmitterNetwork().gasScale == 0)
		{
			return;
		}

		GL11.glPushMatrix();
		CCRenderState.reset();
		CCRenderState.useNormals = true;
		CCRenderState.startDrawing();
		GL11.glTranslated(pos.x, pos.y, pos.z);

		for(ForgeDirection side : ForgeDirection.VALID_DIRECTIONS)
		{
			renderGasSide(side, tube);
		}

		MekanismRenderer.glowOn(0);
		MekanismRenderer.cullFrontFace();

		CCRenderState.draw();

		MekanismRenderer.disableCullFace();
		MekanismRenderer.glowOff();
		GL11.glPopMatrix();
	}

	public void renderStatic(PartSidedPipe transmitter)
	{
		CCRenderState.reset();
		CCRenderState.hasColour = true;
		CCRenderState.setBrightness(transmitter.world(), transmitter.x(), transmitter.y(), transmitter.z());

		for(ForgeDirection side : ForgeDirection.VALID_DIRECTIONS)
		{
			renderSide(side, transmitter);
		}
	}

	public void renderSide(ForgeDirection side, PartSidedPipe transmitter)
	{
		boolean connected = PartTransmitter.connectionMapContainsSide(transmitter.getAllCurrentConnections(), side);
		IIcon renderIcon = transmitter.getIconForSide(side);

		Colour c = null;

		if(transmitter.getRenderColor() != null)
		{
			c = new ColourRGBA(transmitter.getRenderColor().getColor(0), transmitter.getRenderColor().getColor(1), transmitter.getRenderColor().getColor(2), 1);
		}

		renderPart(renderIcon, transmitter.getModelForSide(side, false), transmitter.x(), transmitter.y(), transmitter.z(), c);
	}

	public void renderSide(ForgeDirection side, TransmitterType type)
	{
		boolean out = side == ForgeDirection.UP || side == ForgeDirection.DOWN;

		IIcon renderIcon = out ? type.getSideIcon() : type.getCenterIcon();

		renderPart(renderIcon, getItemModel(side, type), 0, 0, 0, null);
	}

	public void renderEnergySide(ForgeDirection side, PartUniversalCable cable)
	{
		CCRenderState.changeTexture(MekanismRenderer.getBlocksTexture());
		renderTransparency(MekanismRenderer.energyIcon, cable.getModelForSide(side, true), new ColourRGBA(1.0, 1.0, 1.0, cable.currentPower));
	}

	public void renderFluidInOut(ForgeDirection side, PartMechanicalPipe pipe)
	{
		CCRenderState.changeTexture(MekanismRenderer.getBlocksTexture());
		renderTransparency(pipe.getTransmitterNetwork().refFluid.getIcon(), pipe.getModelForSide(side, true), new ColourRGBA(1.0, 1.0, 1.0, pipe.currentScale));
	}

	public void renderGasSide(ForgeDirection side, PartPressurizedTube tube)
	{
		CCRenderState.changeTexture(MekanismRenderer.getBlocksTexture());
		renderTransparency(tube.getTransmitterNetwork().refGas.getIcon(), tube.getModelForSide(side, true), new ColourRGBA(1.0, 1.0, 1.0, tube.currentScale));
	}

	public void renderPart(IIcon icon, CCModel cc, double x, double y, double z, Colour color)
	{
		if(color != null)
		{
			cc.render(new Translation(x, y, z), new IconTransformation(icon), new ColourMultiplier(color.rgba()));
		}
		else {
			cc.render(new Translation(x, y, z), new IconTransformation(icon));
		}
	}

	public void renderTransparency(IIcon icon, CCModel cc, Colour color)
	{
		if(color != null)
		{
			cc.render(new IconTransformation(icon), new ColourMultiplier(color.rgba()));
		}
		else {
			cc.render(new IconTransformation(icon));
		}
	}

	public CCModel getItemModel(ForgeDirection side, TransmitterType type)
	{
		String name = side.name().toLowerCase();
		boolean out = side == ForgeDirection.UP || side == ForgeDirection.DOWN;
		name += out ? "NORMAL" : "NONE";
		
		if(type.getSize() == Size.SMALL)
		{
			return small_models.get(name);
		}
		else {
			return large_models.get(name);
		}
	}

	@Override
	public void registerIcons(IIconRegister register)
	{
		PartUniversalCable.registerIcons(register);
		PartMechanicalPipe.registerIcons(register);
		PartPressurizedTube.registerIcons(register);
		PartLogisticalTransporter.registerIcons(register);
	}

	@Override
	public int atlasIndex()
	{
		return 0;
	}

	private void popTransporter()
	{
		GL11.glPopAttrib();
		MekanismRenderer.glowOff();
		MekanismRenderer.blendOff();
		GL11.glPopMatrix();
	}

	private void pushTransporter()
	{
		GL11.glPushMatrix();
		GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
		GL11.glEnable(GL11.GL_CULL_FACE);
		GL11.glDisable(GL11.GL_LIGHTING);
		MekanismRenderer.glowOn();
		MekanismRenderer.blendOn();
	}

	private DisplayInteger getOverlayDisplay(World world, ForgeDirection side, int mode)
	{
		if(cachedOverlays.containsKey(side) && cachedOverlays.get(side).containsKey(mode))
		{
			return cachedOverlays.get(side).get(mode);
		}

		IIcon icon = null;

		switch(mode)
		{
			case 0:
				icon = Items.gunpowder.getIcon(new ItemStack(Items.gunpowder), 0);
				break;
			case 1:
				icon = Blocks.redstone_torch.getIcon(0, 0);
				break;
			case 2:
				icon = Blocks.redstone_torch.getIcon(0, 0);
				break;
		}

		Model3D toReturn = new Model3D();
		toReturn.baseBlock = Blocks.stone;
		toReturn.setTexture(icon);

		DisplayInteger display = DisplayInteger.createAndStart();

		if(cachedOverlays.containsKey(side))
		{
			cachedOverlays.get(side).put(mode, display);
		}
		else {
			HashMap<Integer, DisplayInteger> map = new HashMap<Integer, DisplayInteger>();
			map.put(mode, display);
			cachedOverlays.put(side, map);
		}

		switch(side)
		{
			case DOWN:
			{
				toReturn.minY = -0.01;
				toReturn.maxY = 0;

				toReturn.minX = 0;
				toReturn.minZ = 0;
				toReturn.maxX = 1;
				toReturn.maxZ = 1;
				break;
			}
			case UP:
			{
				toReturn.minY = 1;
				toReturn.maxY = 1.01;

				toReturn.minX = 0;
				toReturn.minZ = 0;
				toReturn.maxX = 1;
				toReturn.maxZ = 1;
				break;
			}
			case NORTH:
			{
				toReturn.minZ = -0.01;
				toReturn.maxZ = 0;

				toReturn.minX = 0;
				toReturn.minY = 0;
				toReturn.maxX = 1;
				toReturn.maxY = 1;
				break;
			}
			case SOUTH:
			{
				toReturn.minZ = 1;
				toReturn.maxZ = 1.01;

				toReturn.minX = 0;
				toReturn.minY = 0;
				toReturn.maxX = 1;
				toReturn.maxY = 1;
				break;
			}
			case WEST:
			{
				toReturn.minX = -0.01;
				toReturn.maxX = 0;

				toReturn.minY = 0;
				toReturn.minZ = 0;
				toReturn.maxY = 1;
				toReturn.maxZ = 1;
				break;
			}
			case EAST:
			{
				toReturn.minX = 1;
				toReturn.maxX = 1.01;

				toReturn.minY = 0;
				toReturn.minZ = 0;
				toReturn.maxY = 1;
				toReturn.maxZ = 1;
				break;
			}
			default:
			{
				break;
			}
		}

		MekanismRenderer.renderObject(toReturn);
		display.endList();

		return display;
	}
}

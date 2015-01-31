package mekanism.common.multipart;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import mekanism.api.Coord4D;
import mekanism.api.EnumColor;
import mekanism.api.IConfigurable;
import mekanism.api.transmitters.IBlockableConnection;
import mekanism.api.transmitters.ITransmitter;
import mekanism.api.transmitters.TransmissionType;
import mekanism.client.render.RenderPartTransmitter;
import mekanism.common.ITileNetwork;
import mekanism.common.Mekanism;
import mekanism.common.Tier;
import mekanism.common.multipart.TransmitterType.Size;
import mekanism.common.util.MekanismUtils;

import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.common.util.ForgeDirection;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import io.netty.buffer.ByteBuf;

import codechicken.lib.data.MCDataInput;
import codechicken.lib.data.MCDataOutput;
import codechicken.lib.raytracer.ExtendedMOP;
import codechicken.lib.raytracer.IndexedCuboid6;
import codechicken.lib.raytracer.RayTracer;
import codechicken.lib.render.CCModel;
import codechicken.lib.vec.Cuboid6;
import codechicken.lib.vec.Vector3;
import codechicken.microblock.ISidedHollowConnect;
import codechicken.multipart.INeighborTileChange;
import codechicken.multipart.IconHitEffects;
import codechicken.multipart.JIconHitEffects;
import codechicken.multipart.JNormalOcclusion;
import codechicken.multipart.NormalOcclusionTest;
import codechicken.multipart.PartMap;
import codechicken.multipart.TMultiPart;
import codechicken.multipart.TSlottedPart;

public abstract class PartSidedPipe extends TMultiPart implements TSlottedPart, JNormalOcclusion, ISidedHollowConnect, JIconHitEffects, ITileNetwork, IBlockableConnection, IConfigurable, ITransmitter, INeighborTileChange
{
	public static IndexedCuboid6[] smallSides = new IndexedCuboid6[7];
	public static IndexedCuboid6[] largeSides = new IndexedCuboid6[7];

	public int delayTicks;

	public ForgeDirection testingSide = null;

	public byte currentAcceptorConnections = 0x00;
	public byte currentTransmitterConnections = 0x00;

	public boolean sendDesc = false;
	public boolean redstonePowered = false;

	public boolean redstoneReactive = true;

	public ConnectionType[] connectionTypes = {ConnectionType.NORMAL, ConnectionType.NORMAL, ConnectionType.NORMAL, ConnectionType.NORMAL, ConnectionType.NORMAL, ConnectionType.NORMAL};

	static
	{
		smallSides[0] = new IndexedCuboid6(0, new Cuboid6(0.3, 0.0, 0.3, 0.7, 0.3, 0.7));
		smallSides[1] = new IndexedCuboid6(1, new Cuboid6(0.3, 0.7, 0.3, 0.7, 1.0, 0.7));
		smallSides[2] = new IndexedCuboid6(2, new Cuboid6(0.3, 0.3, 0.0, 0.7, 0.7, 0.3));
		smallSides[3] = new IndexedCuboid6(3, new Cuboid6(0.3, 0.3, 0.7, 0.7, 0.7, 1.0));
		smallSides[4] = new IndexedCuboid6(4, new Cuboid6(0.0, 0.3, 0.3, 0.3, 0.7, 0.7));
		smallSides[5] = new IndexedCuboid6(5, new Cuboid6(0.7, 0.3, 0.3, 1.0, 0.7, 0.7));
		smallSides[6] = new IndexedCuboid6(6, new Cuboid6(0.3, 0.3, 0.3, 0.7, 0.7, 0.7));

		largeSides[0] = new IndexedCuboid6(0, new Cuboid6(0.25, 0.0, 0.25, 0.75, 0.25, 0.75));
		largeSides[1] = new IndexedCuboid6(1, new Cuboid6(0.25, 0.75, 0.25, 0.75, 1.0, 0.75));
		largeSides[2] = new IndexedCuboid6(2, new Cuboid6(0.25, 0.25, 0.0, 0.75, 0.75, 0.25));
		largeSides[3] = new IndexedCuboid6(3, new Cuboid6(0.25, 0.25, 0.75, 0.75, 0.75, 1.0));
		largeSides[4] = new IndexedCuboid6(4, new Cuboid6(0.0, 0.25, 0.25, 0.25, 0.75, 0.75));
		largeSides[5] = new IndexedCuboid6(5, new Cuboid6(0.75, 0.25, 0.25, 1.0, 0.75, 0.75));
		largeSides[6] = new IndexedCuboid6(6, new Cuboid6(0.25, 0.25, 0.25, 0.75, 0.75, 0.75));
	}

	public static TMultiPart getPartType(TransmitterType type)
	{
		switch(type)
		{
			case UNIVERSAL_CABLE_BASIC:
				return new PartUniversalCable(Tier.CableTier.BASIC);
			case UNIVERSAL_CABLE_ADVANCED:
				return new PartUniversalCable(Tier.CableTier.ADVANCED);
			case UNIVERSAL_CABLE_ELITE:
				return new PartUniversalCable(Tier.CableTier.ELITE);
			case UNIVERSAL_CABLE_ULTIMATE:
				return new PartUniversalCable(Tier.CableTier.ULTIMATE);
			case MECHANICAL_PIPE_BASIC:
				return new PartMechanicalPipe(Tier.PipeTier.BASIC);
			case MECHANICAL_PIPE_ADVANCED:
				return new PartMechanicalPipe(Tier.PipeTier.ADVANCED);
			case MECHANICAL_PIPE_ELITE:
				return new PartMechanicalPipe(Tier.PipeTier.ELITE);
			case MECHANICAL_PIPE_ULTIMATE:
				return new PartMechanicalPipe(Tier.PipeTier.ULTIMATE);
			case PRESSURIZED_TUBE:
				return new PartPressurizedTube();
			case LOGISTICAL_TRANSPORTER:
				return new PartLogisticalTransporter();
			case RESTRICTIVE_TRANSPORTER:
				return new PartRestrictiveTransporter();
			case DIVERSION_TRANSPORTER:
				return new PartDiversionTransporter();
			default:
				return null;
		}
	}

	public static boolean connectionMapContainsSide(byte connections, ForgeDirection side)
	{
		byte tester = (byte)(1 << side.ordinal());
		return (connections & tester) > 0;
	}

	public abstract IIcon getCenterIcon();

	public abstract IIcon getSideIcon();

	@Override
	public void update()
	{
		if(world().isRemote)
		{
			if(delayTicks == 5)
			{
				delayTicks = 6; /* don't refresh again */
				refreshConnections();
			}
			else if(delayTicks < 5)
			{
				delayTicks++;
			}
		}

		if(sendDesc && !world().isRemote)
		{
			sendDescUpdate();
			sendDesc = false;
		}
	}

	public IIcon getIconForSide(ForgeDirection side)
	{
		ConnectionType type = getConnectionType(side);

		if(type == ConnectionType.NONE)
		{
			return getCenterIcon();
		}
		else {
			return getSideIcon();
		}
	}

	public byte getPossibleTransmitterConnections()
	{
		byte connections = 0x00;

		if(redstoneReactive && world().isBlockIndirectlyGettingPowered(x(), y(), z()))
		{
			return connections;
		}

		for(ForgeDirection side : ForgeDirection.VALID_DIRECTIONS)
		{
			if(canConnectMutual(side))
			{
				TileEntity tileEntity = Coord4D.get(tile()).getFromSide(side).getTileEntity(world());

				if(TransmissionType.checkTransmissionType(tileEntity, getTransmitter().getTransmission()))
				{
					connections |= 1 << side.ordinal();
				}
			}
		}

		return connections;
	}

	public byte getPossibleAcceptorConnections()
	{
		byte connections = 0x00;

		if(redstoneReactive && world().isBlockIndirectlyGettingPowered(x(), y(), z()))
		{
			return connections;
		}

		for(ForgeDirection side : ForgeDirection.VALID_DIRECTIONS)
		{
			if(canConnectMutual(side))
			{
				TileEntity tileEntity = Coord4D.get(tile()).getFromSide(side).getTileEntity(world());

				if(isValidAcceptor(tileEntity, side))
				{
					connections |= 1 << side.ordinal();
				}
			}
		}

		return connections;
	}

	public byte getAllCurrentConnections()
	{
		return (byte)(currentTransmitterConnections | currentAcceptorConnections);
	}

	@Override
	public boolean occlusionTest(TMultiPart other)
	{
		return NormalOcclusionTest.apply(this, other);
	}

	@Override
	public Iterable<IndexedCuboid6> getSubParts()
	{
		Set<IndexedCuboid6> subParts = new HashSet<IndexedCuboid6>();

		if(tile() != null)
		{
			for(ForgeDirection side : ForgeDirection.VALID_DIRECTIONS)
			{
				int ord = side.ordinal();
				byte connections = getAllCurrentConnections();

				if(connectionMapContainsSide(connections, side) || side == testingSide)
				{
					subParts.add(getTransmitter().getSize() == Size.SMALL ? smallSides[ord] : largeSides[ord]);
				}
			}
		}

		subParts.add(getTransmitter().getSize() == Size.SMALL ? smallSides[6] : largeSides[6]);

		return subParts;
	}

	public abstract TransmitterType getTransmitter();

	@Override
	public Iterable<Cuboid6> getCollisionBoxes()
	{
		Set<Cuboid6> collisionBoxes = new HashSet<Cuboid6>();
		collisionBoxes.addAll((Collection<? extends Cuboid6>)getSubParts());

		return collisionBoxes;
	}

	@Override
	public Iterable<Cuboid6> getOcclusionBoxes()
	{
		return getCollisionBoxes();
	}

	@Override
	public int getSlotMask()
	{
		return PartMap.CENTER.mask;
	}

	@Override
	public IIcon getBreakingIcon(Object subPart, int side)
	{
		return getCenterIcon();
	}

	@Override
	public IIcon getBrokenIcon(int side)
	{
		return getCenterIcon();
	}

	@Override
	public Cuboid6 getBounds()
	{
		return getTransmitter().getSize() == Size.SMALL ? smallSides[6] : largeSides[6];
	}

	@Override
	public int getHollowSize(int side)
	{
		ForgeDirection direction = ForgeDirection.getOrientation(side);

		if(connectionMapContainsSide(getAllCurrentConnections(), direction) || direction == testingSide)
		{
			return getTransmitter().getSize().centerSize+1;
		}
		
		return 0;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean renderStatic(Vector3 pos, int pass)
	{
		if(pass == 1)
		{
			RenderPartTransmitter.getInstance().renderStatic(this);
			return true;
		}
		
		return false;
	}

	@Override
	public void addHitEffects(MovingObjectPosition hit, EffectRenderer effectRenderer)
	{
		IconHitEffects.addHitEffects(this, hit, effectRenderer);
	}

	@Override
	public void addDestroyEffects(MovingObjectPosition mop, EffectRenderer effectRenderer)
	{
		IconHitEffects.addDestroyEffects(this, effectRenderer, false);
	}

	public abstract boolean isValidAcceptor(TileEntity tile, ForgeDirection side);

	@Override
	public boolean canConnectMutual(ForgeDirection side)
	{
		if(!canConnect(side)) return false;

		TileEntity tile = Coord4D.get(tile()).getFromSide(side).getTileEntity(world());
		return (!(tile instanceof IBlockableConnection) || ((IBlockableConnection)tile).canConnect(side.getOpposite()));
	}

	@Override
	public boolean canConnect(ForgeDirection side)
	{
		if(redstoneReactive && world().isBlockIndirectlyGettingPowered(x(), y(), z()))
		{
			return false;
		}

		testingSide = side;
		boolean unblocked = tile().canReplacePart(this, this);
		testingSide = null;
		
		return unblocked;
	}

	@Override
	public void readDesc(MCDataInput packet)
	{
		currentTransmitterConnections = packet.readByte();
		currentAcceptorConnections = packet.readByte();

		for(int i = 0; i < 6; i++)
		{
			connectionTypes[i] = ConnectionType.values()[packet.readInt()];
		}

		if(tile() != null)
		{
			tile().internalPartChange(this);
			tile().markRender();
		}
	}

	@Override
	public void writeDesc(MCDataOutput packet)
	{
		packet.writeByte(currentTransmitterConnections);
		packet.writeByte(currentAcceptorConnections);

		for(int i = 0; i < 6; i++)
		{
			packet.writeInt(connectionTypes[i].ordinal());
		}
	}

	@Override
	public void load(NBTTagCompound nbtTags)
	{
		super.load(nbtTags);

		redstoneReactive = nbtTags.getBoolean("redstoneReactive");

		for(int i = 0; i < 6; i++)
		{
			connectionTypes[i] = ConnectionType.values()[nbtTags.getInteger("connection" + i)];
		}
	}

	@Override
	public void save(NBTTagCompound nbtTags)
	{
		super.save(nbtTags);

		nbtTags.setBoolean("redstoneReactive", redstoneReactive);

		for(int i = 0; i < 6; i++)
		{
			nbtTags.setInteger("connection" + i, connectionTypes[i].ordinal());
		}
	}

	@Override
	public boolean activate(EntityPlayer player, MovingObjectPosition part, ItemStack item)
	{
		if(item == null)
		{
			return false;
		}

		if(MekanismUtils.hasUsableWrench(player, x(), y(), z()) && player.isSneaking())
		{
			if(!world().isRemote)
			{
				tile().dropItems(getDrops());
				tile().remPart(this);
			}

			return true;
		}

		return false;
	}

	@Override
	public Iterable<ItemStack> getDrops()
	{
		return Collections.singletonList(pickItem(null));
	}

	@Override
	public ItemStack pickItem(MovingObjectPosition hit)
	{
		return new ItemStack(Mekanism.PartTransmitter, 1, getTransmitter().ordinal());
	}

	@Override
	public boolean doesTick()
	{
		return true;
	}

	protected void onRedstoneSplit() {}

	protected void onRedstoneJoin() {}

	protected void onRefresh() {}

	public void redstoneRefresh()
	{
		boolean nowPowered = redstoneReactive && world().isBlockIndirectlyGettingPowered(x(), y(), z());

		if(nowPowered != redstonePowered)
		{
			refreshConnections();
			redstonePowered = nowPowered;
		}
	}

	public void refreshConnections()
	{
		byte possibleTransmitters = getPossibleTransmitterConnections();
		byte possibleAcceptors = getPossibleAcceptorConnections();

		if(possibleTransmitters != currentTransmitterConnections)
		{
			boolean nowPowered = redstoneReactive && world().isBlockIndirectlyGettingPowered(x(), y(), z());

			if(nowPowered != redstonePowered)
			{
				redstonePowered = nowPowered;

				if(nowPowered)
				{
					onRedstoneSplit();
				}
				else
				{
					onRedstoneJoin();
				}

				tile().notifyTileChange();
			}
		}

		if(!world().isRemote)
		{
			if(getAllCurrentConnections() != (possibleTransmitters | possibleAcceptors))
			{
				sendDesc = true;
			}
			currentTransmitterConnections = possibleTransmitters;
			currentAcceptorConnections = possibleAcceptors;
		}

		onRefresh();
	}

	protected void onModeChange(ForgeDirection side)
	{
		refreshConnections();
	}

	@Override
	public void onAdded()
	{
		super.onAdded();
		redstonePowered = redstoneReactive && world().isBlockIndirectlyGettingPowered(x(), y(), z());
		refreshConnections();
	}

	@Override
	public void onChunkLoad()
	{
		super.onChunkLoad();
		redstonePowered = redstoneReactive && world().isBlockIndirectlyGettingPowered(x(), y(), z());
		refreshConnections();
	}
	
	@Override
	public void onNeighborTileChanged(int side, boolean weak) 
	{
		refreshConnections();
	}

	@Override
	public void onNeighborChanged()
	{
		redstoneRefresh();
	}

	@Override
	public void onPartChanged(TMultiPart part)
	{
		super.onPartChanged(part);
		refreshConnections();
	}

	@Override
	public void handlePacketData(ByteBuf dataStream) throws Exception {}

	@Override
	public ArrayList getNetworkedData(ArrayList data)
	{
		return data;
	}

	public ConnectionType getConnectionType(ForgeDirection side)
	{
		if(!connectionMapContainsSide(getAllCurrentConnections(), side))
		{
			return ConnectionType.NONE;
		}
		else if(connectionMapContainsSide(currentTransmitterConnections, side))
		{
			return ConnectionType.NORMAL;
		}

		return connectionTypes[side.ordinal()];
	}

	public List<ForgeDirection> getConnections(ConnectionType type)
	{
		List<ForgeDirection> sides = new ArrayList<ForgeDirection>();

		for(ForgeDirection side : ForgeDirection.VALID_DIRECTIONS)
		{
			if(getConnectionType(side) == type)
			{
				sides.add(side);
			}
		}

		return sides;
	}

	public CCModel getModelForSide(ForgeDirection side, boolean internal)
	{
		String sideName = side.name().toLowerCase();
		String typeName = getConnectionType(side).name().toUpperCase();
		String name = sideName + typeName;

		if(internal)
		{
			return RenderPartTransmitter.contents_models.get(name);
		}
		else {
			if(getTransmitter().getSize() == Size.LARGE)
			{
				return RenderPartTransmitter.large_models.get(name);
			}
			else {
				return RenderPartTransmitter.small_models.get(name);
			}
		}
	}

	@Override
	public boolean onSneakRightClick(EntityPlayer player, int side)
	{
		ExtendedMOP hit = (ExtendedMOP)RayTracer.retraceBlock(world(), player, x(), y(), z());

		if(hit == null)
		{
			return false;
		}
		else if(hit.subHit < 6)
		{
			connectionTypes[hit.subHit] = connectionTypes[hit.subHit].next();
			sendDesc = true;

			onModeChange(ForgeDirection.getOrientation(side));
			player.addChatMessage(new ChatComponentText("Connection type changed to " + connectionTypes[hit.subHit].toString()));

			return true;
		}
		else {
			return onConfigure(player, hit.subHit, side);
		}
	}

	protected boolean onConfigure(EntityPlayer player, int part, int side)
	{
		return false;
	}

	public EnumColor getRenderColor()
	{
		return null;
	}

	@Override
	public boolean onRightClick(EntityPlayer player, int side)
	{
		redstoneReactive ^= true;
		refreshConnections();
		tile().notifyPartChange(this);

		player.addChatMessage(new ChatComponentText(EnumColor.DARK_BLUE + "[Mekanism]" + EnumColor.GREY + " Redstone sensitivity turned " + EnumColor.INDIGO + (redstoneReactive ? "on." : "off.")));
		return true;
	}

	public boolean canConnectToAcceptor(ForgeDirection side, boolean ignoreActive)
	{
		if(!isValidAcceptor(Coord4D.get(tile()).getFromSide(side).getTileEntity(world()), side) || !connectionMapContainsSide(currentAcceptorConnections, side))
		{
			return false;
		}

		if(!ignoreActive)
		{
			return getConnectionType(side) == ConnectionType.NORMAL || getConnectionType(side) == ConnectionType.PUSH;
		}
		else {
			return connectionTypes[side.ordinal()] == ConnectionType.NORMAL || connectionTypes[side.ordinal()] == ConnectionType.PUSH;
		}
	}

	public static enum ConnectionType
	{
		NORMAL,
		PUSH,
		PULL,
		NONE;

		public ConnectionType next()
		{
			if(ordinal() == values().length-1)
			{
				return NORMAL;
			}

			return values()[ordinal()+1];
		}
	}

	@Override
	public boolean weakTileChanges() 
	{
		return false;
	}
}

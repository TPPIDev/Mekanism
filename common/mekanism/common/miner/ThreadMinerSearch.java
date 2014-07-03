package mekanism.common.miner;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import mekanism.api.Coord4D;
import mekanism.api.ItemInfo;
import mekanism.common.IBoundingBlock;
import mekanism.common.Mekanism;
import mekanism.common.Mekanism.ItemType;
import mekanism.common.tile.TileEntityDigitalMiner;
import mekanism.common.util.MekanismUtils;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

public class ThreadMinerSearch extends Thread
{
	public TileEntityDigitalMiner tileEntity;

	public State state = State.IDLE;

	public BitSet oresToMine = new BitSet();

	public Map<ItemInfo, Boolean> acceptedItems = new HashMap<ItemInfo, Boolean>();

	public int found = 0;

	public ThreadMinerSearch(TileEntityDigitalMiner tile)
	{
		tileEntity = tile;
	}

	@Override
	public void run()
	{
		state = State.SEARCHING;

		if(!tileEntity.inverse && tileEntity.filters.isEmpty())
		{
			state = State.FINISHED;
			return;
		}

		Coord4D coord = tileEntity.getStartingCoord();
		int diameter = tileEntity.getDiameter();
		int size = tileEntity.getTotalSize();
		ItemInfo info = new ItemInfo(0,0);

		for(int i = 0; i < size; i++)
		{
			int x = coord.xCoord+i%diameter;
			int z = coord.zCoord+(i/diameter)%diameter;
			int y = coord.yCoord+(i/diameter/diameter);

			if(tileEntity.isInvalid())
			{
				return;
			}

			if(tileEntity.xCoord == x && tileEntity.yCoord == y && tileEntity.zCoord == z)
			{
				continue;
			}

			if(!tileEntity.worldObj.getChunkProvider().chunkExists(x >> 4, z >> 4))
			{
				continue;
			}

			TileEntity bte;
			if ((bte = tileEntity.worldObj.getBlockTileEntity(x,  y,  z)) != null && bte instanceof IBoundingBlock) 
			{
				continue;
			}

			info.id = tileEntity.worldObj.getBlockId(x, y, z);
			info.meta = tileEntity.worldObj.getBlockMetadata(x, y, z);
			Block block = Block.blocksList[info.id];

			if(info.id != 0 && block.getBlockHardness(tileEntity.worldObj, x, y, z) >= 0 && !Mekanism.digiMinerBlacklist.contains(new ItemType(info.id, info.meta)));
			{
				boolean canFilter = false;

				if(acceptedItems.containsKey(info))
				{
					canFilter = acceptedItems.get(info);
				}
				else {
					ItemStack stack = new ItemStack(info.id, 1, info.meta);

					if(tileEntity.replaceStack != null && tileEntity.replaceStack.isItemEqual(stack))
					{
						continue;
					}

					boolean hasFilter = false;

					for(MinerFilter filter : tileEntity.filters)
					{
						if(filter.canFilter(stack))
						{
							hasFilter = true;
						}
					}

					canFilter = tileEntity.inverse ? !hasFilter : hasFilter;
					acceptedItems.put(info, canFilter);
				}

				if(canFilter)
				{
					oresToMine.set(i);
					found++;
				}
			}
		}

		state = State.FINISHED;
		tileEntity.oresToMine = oresToMine;
		MekanismUtils.saveChunk(tileEntity);
	}

	public void reset()
	{
		state = State.IDLE;
	}

	public static enum State
	{
		IDLE("Not ready"),
		SEARCHING("Searching"),
		PAUSED("Paused"),
		FINISHED("Ready");

		public String desc;

		private State(String s)
		{
			desc = s;
		}
	}
}

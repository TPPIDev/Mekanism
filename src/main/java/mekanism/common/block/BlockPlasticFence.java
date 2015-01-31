package mekanism.common.block;

import mekanism.common.Mekanism;

import net.minecraft.block.BlockFence;
import net.minecraft.block.material.Material;

public class BlockPlasticFence extends BlockFence
{
	public BlockPlasticFence()
	{
		super("mekanism:PlasticFence", Material.clay);
		setCreativeTab(Mekanism.tabMekanism);
	}
}

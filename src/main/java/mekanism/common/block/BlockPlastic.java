package mekanism.common.block;

import java.util.List;

import mekanism.api.EnumColor;
import mekanism.client.ClientProxy;
import mekanism.common.Mekanism;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockPlastic extends Block
{
	public BlockPlastic()
	{
		super(Material.wood);
		setHardness(5F);
		setResistance(10F);
		setCreativeTab(Mekanism.tabMekanism);
		if(this == Mekanism.BlockSlickHDPE)
		{
			slipperiness = 0.98F;
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerBlockIcons(IIconRegister register)
	{
		if(this == Mekanism.BlockHDPE)
		{
			blockIcon = register.registerIcon("mekanism:PlasticBlock");
		}
		else if(this == Mekanism.BlockSlickHDPE)
		{
			blockIcon = register.registerIcon("mekanism:SlickPlasticBlock");
		}
		else if(this == Mekanism.BlockGlowHDPE)
		{
			blockIcon = register.registerIcon("mekanism:GlowPlasticBlock");
		}
		else if(this == Mekanism.BlockReinforcedHDPE)
		{
			blockIcon = register.registerIcon("mekanism:ReinforcedPlasticBlock");
		}
		else if(this == Mekanism.BlockRoadHDPE)
		{
			blockIcon = register.registerIcon("mekanism:RoadPlasticBlock");
		}
	}

	@Override
	public void onEntityWalking(World world, int x, int y, int z, Entity e)
	{
		if(this == Mekanism.BlockRoadHDPE)
		{
			double boost = 1.6;

			double a = Math.atan2(e.motionX, e.motionZ);
			e.motionX += Math.sin(a) * boost * slipperiness;
			e.motionZ += Math.cos(a) * boost * slipperiness;
		}
	}

	@Override
	public int damageDropped(int i)
	{
		return i;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void getSubBlocks(Item item, CreativeTabs creativetabs, List list)
	{
		for(int i = 0; i < EnumColor.DYES.length; i++)
		{
			list.add(new ItemStack(item, 1, i));
		}
	}

	@Override
	public int colorMultiplier(IBlockAccess world, int x, int y, int z)
	{
		return getRenderColor(world.getBlockMetadata(x, y, z));
	}

	@Override
	public int getRenderColor(int meta)
	{
		EnumColor colour = EnumColor.DYES[meta];
		return (int)(colour.getColor(0)*255) << 16 | (int)(colour.getColor(1)*255) << 8 | (int)(colour.getColor(2)*255);

	}

	@Override
	public int getLightValue(IBlockAccess world, int x, int y, int z)
	{
		if(this == Mekanism.BlockGlowHDPE)
		{
			return 10;
		}

		return 0;
	}

	public boolean recolourBlock(World world, int x, int y, int z, ForgeDirection side, int colour)
	{
		int meta = world.getBlockMetadata(x, y, z);
		if (meta != (15 - colour))
		{
			world.setBlockMetadataWithNotify(x, y, z, 15-colour, 3);
			return true;
		}
		return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getRenderType()
	{
		return ClientProxy.PLASTIC_RENDER_ID;
	}
}

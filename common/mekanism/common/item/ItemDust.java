package mekanism.common.item;

import java.util.List;

import mekanism.common.Mekanism;
import net.minecraft.client.renderer.texture.IconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;

public class ItemDust extends ItemMekanism
{
	public Icon[] icons = new Icon[256];

	public static String[] en_USNames = {"Iron", "Gold", "Osmium",
										"Obsidian", "Diamond", "Steel",
										"Copper", "Tin", "Silver",
										"Lead", "Sulfur"};

	public ItemDust(int id)
	{
		super(id);
		setHasSubtypes(true);
		setCreativeTab(Mekanism.tabMekanism);
	}

	@Override
	public void registerIcons(IconRegister register)
	{
		for(int i = 0; i <= 10; i++)
		{
			icons[i] = register.registerIcon("mekanism:" + en_USNames[i] + "Dust");
		}
	}

	@Override
	public Icon getIconFromDamage(int meta)
	{
		return icons[meta];
	}

	@Override
	public void getSubItems(int id, CreativeTabs tabs, List itemList)
	{
		for(int counter = 0; counter <= 10; ++counter)
		{
			itemList.add(new ItemStack(this, 1, counter));
		}
	}

	@Override
	public String getUnlocalizedName(ItemStack item)
	{
		return "item." + en_USNames[item.getItemDamage()].toLowerCase() + "Dust";
	}
}
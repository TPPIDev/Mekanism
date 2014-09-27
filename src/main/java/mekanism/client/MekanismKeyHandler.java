package mekanism.client;

import mekanism.common.util.MekanismUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.Type;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class MekanismKeyHandler extends MekKeyHandler
{
	public static final String keybindCategory = "Mekanism";
	public static KeyBinding modeSwitchKey = new KeyBinding("Mekanism " + MekanismUtils.localize("key.mode"), Keyboard.KEY_M, keybindCategory);
	public static KeyBinding voiceKey = new KeyBinding("Mekanism " + MekanismUtils.localize("key.voice"), Keyboard.KEY_U, keybindCategory);
	public static KeyBinding sneakKey = Minecraft.getMinecraft().gameSettings.keyBindSneak;
	public static KeyBinding jumpKey = Minecraft.getMinecraft().gameSettings.keyBindJump;

	public MekanismKeyHandler()
	{
		super(new KeyBinding[] {modeSwitchKey, voiceKey}, new boolean[] {false, false});
		
		ClientRegistry.registerKeyBinding(modeSwitchKey);
		ClientRegistry.registerKeyBinding(voiceKey);
		
		FMLCommonHandler.instance().bus().register(this);
	}
	
	@SubscribeEvent
	public void onTick(ClientTickEvent event)
	{
		if(event.side == Side.CLIENT)
		{
			if(event.phase == Phase.START)
			{
				keyTick(event.type, false);
			}
			else if(event.phase == Phase.END)
			{
				keyTick(event.type, true);
			}
		}
	}

	@Override
	public void keyDown(Type types, KeyBinding kb, boolean tickEnd, boolean isRepeat) {}

	@Override
	public void keyUp(Type types, KeyBinding kb, boolean tickEnd) {}
}

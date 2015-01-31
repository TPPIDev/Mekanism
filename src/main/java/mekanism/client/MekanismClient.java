package mekanism.client;

import mekanism.api.MekanismAPI;
import mekanism.api.MekanismAPI.BoxBlacklistEvent;
import mekanism.client.sound.SoundHandler;
import mekanism.client.voice.VoiceClient;
import mekanism.common.Mekanism;
import mekanism.common.network.PacketKey.KeyMessage;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class MekanismClient extends Mekanism
{
	@SideOnly(Side.CLIENT)
	/** The main SoundHandler instance that is used by all audio sources */
	public static SoundHandler audioHandler;

	public static VoiceClient voiceClient;

	//General Configuration
	public static boolean enableSounds = true;
	public static boolean fancyUniversalCableRender = true;
	public static boolean holidays = true;
	public static double baseSoundVolume = 1;

	public static long ticksPassed = 0;

	public static void updateKey(KeyBinding key, int type)
	{
		boolean down = Minecraft.getMinecraft().currentScreen == null ? key.getIsKeyPressed() : false;

		if(down != keyMap.has(Minecraft.getMinecraft().thePlayer, type))
		{
			Mekanism.packetHandler.sendToServer(new KeyMessage(type, down));
			keyMap.update(Minecraft.getMinecraft().thePlayer, type, down);
		}
	}

	public static void reset()
	{
		if(Mekanism.voiceServerEnabled)
		{
			if(MekanismClient.voiceClient != null)
			{
				MekanismClient.voiceClient.disconnect();
				MekanismClient.voiceClient = null;
			}
		}

		ClientTickHandler.tickingSet.clear();
		Mekanism.proxy.unloadSoundHandler();

		MekanismAPI.getBoxIgnore().clear();
		MinecraftForge.EVENT_BUS.post(new BoxBlacklistEvent());

		Mekanism.jetpackOn.clear();
		Mekanism.gasmaskOn.clear();
		Mekanism.activeVibrators.clear();

		Mekanism.proxy.loadConfiguration();

		Mekanism.logger.info("Reloaded config.");
	}
}

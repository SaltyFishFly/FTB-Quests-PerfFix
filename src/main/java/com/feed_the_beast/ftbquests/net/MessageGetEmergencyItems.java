package com.feed_the_beast.ftbquests.net;

import com.feed_the_beast.ftblib.lib.config.ConfigItemStack;
import com.feed_the_beast.ftblib.lib.net.MessageToServer;
import com.feed_the_beast.ftblib.lib.net.NetworkWrapper;
import com.feed_the_beast.ftbquests.quest.ServerQuestFile;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.items.ItemHandlerHelper;

/**
 * @author LatvianModder
 */
public class MessageGetEmergencyItems extends MessageToServer
{
	@Override
	public NetworkWrapper getWrapper()
	{
		return FTBQuestsNetHandler.GENERAL;
	}

	@Override
	public void onMessage(EntityPlayerMP player)
	{
		//TODO: Verify on server side

		for (ConfigItemStack value : ServerQuestFile.INSTANCE.emergencyItems)
		{
			ItemHandlerHelper.giveItemToPlayer(player, value.getStack().copy());
		}
	}
}
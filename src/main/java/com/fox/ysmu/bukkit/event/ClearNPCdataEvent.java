package com.fox.ysmu.bukkit.event;

import com.fox.ysmu.bukkit.client.NPCData;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ClearNPCdataEvent { // 客户端
    @SubscribeEvent
    public static void onPlayerLeave(PlayerLoggedOutEvent event) {
        NPCData.clear();
    }
}

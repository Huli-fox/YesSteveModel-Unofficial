package com.elfmcys.yesstevemodel.event;

import com.elfmcys.yesstevemodel.command.RootCommand;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public final class CommandRegistry {

    @SubscribeEvent
    public static void onRegisterCommand(RegisterCommandsEvent event) {
        RootCommand.registerCommands(event.getDispatcher());
    }
}

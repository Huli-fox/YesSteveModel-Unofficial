package com.elfmcys.yesstevemodel.command;

import com.elfmcys.yesstevemodel.command.subcommands.ModelCommand;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class RootCommand {

    private static final String ROOT_NAME = "ysm";

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(ROOT_NAME);
        root.then(ModelCommand.register());
        dispatcher.register(root);
    }
}

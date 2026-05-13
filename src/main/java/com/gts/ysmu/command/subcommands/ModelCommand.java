package com.gts.ysmu.command.subcommands;

import com.gts.ysmu.model.ServerModelManager;
import com.gts.ysmu.util.YSMMessageFormatter;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.apache.commons.lang3.time.StopWatch;

import java.util.concurrent.TimeUnit;

public class ModelCommand {

    private static final String MODEL_NAME = "model";

    private static final String LITERAL_RELOAD = "reload";

    public static LiteralArgumentBuilder<CommandSourceStack> register() {
        LiteralArgumentBuilder<CommandSourceStack> model = Commands.literal(MODEL_NAME).requires(commandSourceStack -> YSMMessageFormatter.hasCommandPermission(commandSourceStack, 2));
        model.then(Commands.literal(LITERAL_RELOAD).executes(ModelCommand::reloadAllPack));
        return model;
    }

    private static int reloadAllPack(CommandContext<CommandSourceStack> context) {
        StopWatch watch = StopWatch.createStarted();
        if (!ServerModelManager.loadModels(result -> {
            if (result.getErrorMessage() != null) {
                YSMMessageFormatter.sendServerMessage(context.getSource(), YSMMessageFormatter.withPrefix(result.getErrorMessage()), true);
            }
            if (result.isSuccess()) {
                YSMMessageFormatter.sendServerMessage(context.getSource(), Component.translatable("message.yes_steve_model.model.reload.complete", Double.valueOf(watch.getTime(TimeUnit.MICROSECONDS) / 1000.0d)), true);
                watch.reset();
                watch.start();
            }
        }, data -> {
            watch.stop();
            if (!data.isEnabled()) {
                YSMMessageFormatter.sendServerMessage(context.getSource(), YSMMessageFormatter.withPrefix(data.getDisplayComponent()), true);
                return;
            }
            if (!data.getUuidComponentMap().isEmpty()) {
                for (Component component : data.getUuidComponentMap().values()) {
                    YSMMessageFormatter.sendServerMessage(context.getSource(), YSMMessageFormatter.withPrefix(component), true);
                }
                if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
                    YSMMessageFormatter.sendServerMessage(context.getSource(), Component.translatable("message.yes_steve_model.model.sync.complete", Double.valueOf(watch.getTime(TimeUnit.MICROSECONDS) / 1000.0d)), true);
                }
            }
        })) {
            context.getSource().sendFailure(Component.translatable("message.yes_steve_model.model.reload.in_progress"));
            return Command.SINGLE_SUCCESS;
        }
        context.getSource().sendSuccess(() -> Component.translatable("message.yes_steve_model.model.reload.start"), true);
        return Command.SINGLE_SUCCESS;
    }
}

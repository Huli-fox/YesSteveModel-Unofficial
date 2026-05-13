package com.gts.ysmu.util.log;

import net.minecraft.network.chat.Component;

public interface ILogger {
    void logFormatted(String str, Object... objArr);

    void logComponent(Component component);
}

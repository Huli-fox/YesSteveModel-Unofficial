package com.gts.ysmu.audio;

import net.minecraft.client.sounds.AudioStream;

public interface IAudioStreamSupport extends AudioStream {
    boolean isClosed();
}

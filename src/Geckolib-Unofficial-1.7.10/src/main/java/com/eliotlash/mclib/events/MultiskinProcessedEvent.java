package com.eliotlash.mclib.events;

import com.eliotlash.mclib.utils.resources.MultiResourceLocation;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import java.awt.image.BufferedImage;

@SideOnly(Side.CLIENT)
public class MultiskinProcessedEvent extends Event {
    public MultiResourceLocation location;
    public BufferedImage image;

    public MultiskinProcessedEvent(MultiResourceLocation location, BufferedImage image) {
        this.location = location;
        this.image = image;
    }
}

package com.fox.ysmu.compat;

import cpw.mods.fml.common.Loader;
import ganymedes01.etfuturum.api.elytra.IElytraPlayer;
import ganymedes01.etfuturum.elytra.IClientElytraPlayer;
import ganymedes01.etfuturum.spectator.SpectatorMode;
import ganymedes01.etfuturum.items.equipment.ItemArmorElytra;
import net.minecraft.entity.player.EntityPlayer;

public class EtfuturumCompat {
    private static final boolean ETFUTURUM_LOADED = Loader.isModLoaded("etfuturum");

    public static boolean isEtfuturumLoadedLoaded() {
        return ETFUTURUM_LOADED;
    }

    public static boolean isFallFlying(EntityPlayer entityPlayer) {
        if (ETFUTURUM_LOADED) {
            return ((IElytraPlayer)entityPlayer).etfu$isElytraFlying();
        }
        return false;
    }

    public static boolean isSpectator(EntityPlayer entityPlayer) {
        if (ETFUTURUM_LOADED) {
            return SpectatorMode.isSpectator(entityPlayer);
        }
        return false;
    }

    public static boolean hasElytra(EntityPlayer entityPlayer) {
        if (ETFUTURUM_LOADED) {
            return ItemArmorElytra.getElytra(entityPlayer) != null;
        }
        return false;
    }

    public static double getElytraRot(EntityPlayer entityPlayer, String xyz) {
        if (ETFUTURUM_LOADED) {
            if (entityPlayer instanceof IClientElytraPlayer cep) {
                switch (xyz) {
                    case "x" -> {
                        return Math.toDegrees(cep.getRotateElytraX());
                    }
                    case "y" -> {
                        return Math.toDegrees(cep.getRotateElytraY());
                    }
                    case "z" -> {
                        return Math.toDegrees(cep.getRotateElytraZ());
                    }
                }
            }
            return 0;
        }
        return 0;
    }
}

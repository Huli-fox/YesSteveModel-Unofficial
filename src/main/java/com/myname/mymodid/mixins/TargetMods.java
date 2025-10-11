package com.myname.mymodid.mixins;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.ITargetMod;
import com.gtnewhorizon.gtnhmixins.builders.TargetModBuilder;

public enum TargetMods implements ITargetMod {

    // Read the java doc of ITargetMod and TargetModBuilder for further information

    // Add to this enum information about the mods you need to identify during runtime

    COFHCORE("cofh.asm.LoadingPlugin", "CoFHCore"),
    OPTIFINE("optifine.OptiFineForgeTweaker", "Optifine");

    private final TargetModBuilder builder;

    TargetMods(String coreModClass, String modId) {
        this.builder = new TargetModBuilder().setCoreModClass(coreModClass)
            .setModId(modId);
    }

    @Nonnull
    @Override
    public TargetModBuilder getBuilder() {
        return builder;
    }
}

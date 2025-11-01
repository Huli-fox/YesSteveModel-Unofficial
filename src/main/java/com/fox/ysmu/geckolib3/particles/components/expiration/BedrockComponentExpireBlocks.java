package com.fox.ysmu.geckolib3.particles.components.expiration;

import com.fox.ysmu.geckolib3.core.molang.MolangException;
import com.fox.ysmu.geckolib3.core.molang.MolangParser;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import cpw.mods.fml.common.registry.GameData;
import com.fox.ysmu.geckolib3.util.BlockPos;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import com.fox.ysmu.geckolib3.particles.components.BedrockComponentBase;
import com.fox.ysmu.geckolib3.particles.emitter.BedrockEmitter;
import com.fox.ysmu.geckolib3.particles.emitter.BedrockParticle;

import javax.vecmath.Vector3d;
import java.util.ArrayList;
import java.util.List;

public abstract class BedrockComponentExpireBlocks extends BedrockComponentBase {
    public List<Block> blocks = new ArrayList<Block>();

    private BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    @Override
    public BedrockComponentBase fromJson(JsonElement element, MolangParser parser) throws MolangException {
        if (element.isJsonArray()) {
            for (JsonElement value : element.getAsJsonArray()) {
                Block block = GameData.getBlockRegistry().getObject(value.getAsString());

                if (block != null) {
                    this.blocks.add(block);
                }
            }
        }

        return super.fromJson(element, parser);
    }

    @Override
    public JsonElement toJson() {
        JsonArray array = new JsonArray();

        for (Block block : this.blocks) {
            String rl = GameData.getBlockRegistry().getNameForObject(block);

            if (rl != null) {
                array.add(new JsonPrimitive(rl));
            }
        }

        return array;
    }

    public Block getBlock(BedrockEmitter emitter, BedrockParticle particle) {
        if (emitter.world == null) {
            return Blocks.air;
        }

        Vector3d position = particle.getGlobalPosition(emitter);

        this.pos.set((int) position.x, (int) position.y, (int) position.z);

        return emitter.world.getBlock(pos.getX(), pos.getY(), pos.getZ());
    }
}

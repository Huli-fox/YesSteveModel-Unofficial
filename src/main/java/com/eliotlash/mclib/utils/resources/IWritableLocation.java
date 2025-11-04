package com.eliotlash.mclib.utils.resources;

import net.minecraft.nbt.NBTBase;

import com.eliotlash.mclib.utils.ICopy;
import com.google.gson.JsonElement;

public interface IWritableLocation<T> extends ICopy<T> {

    public void fromNbt(NBTBase nbt) throws Exception;

    public void fromJson(JsonElement element) throws Exception;

    public NBTBase writeNbt();

    public JsonElement writeJson();
}

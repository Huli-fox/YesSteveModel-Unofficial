package com.fox.ysmu.geckolib3.geo.raw.pojo;

import com.fox.ysmu.util.Keep;
import com.google.gson.*;
import com.google.gson.annotations.JsonAdapter;

import java.io.Serializable;
import java.lang.reflect.Type;

@JsonAdapter(LocatorValue.Serializer.class)
public class LocatorValue implements Serializable {
    public LocatorClass locatorClassValue;
    public double[] doubleArrayValue;

    protected static class Serializer implements JsonSerializer<LocatorValue>, JsonDeserializer<LocatorValue> {
        @Override
        @Keep
        public LocatorValue deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            LocatorValue result = new LocatorValue();
            if (json.isJsonArray()) {
                result.doubleArrayValue = context.deserialize(json, double[].class);
            } else if (json.isJsonObject()) {
                result.locatorClassValue = context.deserialize(json, LocatorClass.class);
            }
            return result;
        }

        @Override
        @Keep
        public JsonElement serialize(LocatorValue src, Type typeOfSrc, JsonSerializationContext context) {
            return src.locatorClassValue != null ?
                    context.serialize(src.locatorClassValue) :
                    context.serialize(src.doubleArrayValue);
        }
    }
}

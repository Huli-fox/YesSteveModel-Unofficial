// To use this code, add the following Maven dependency to your project:
//
//
//     com.fasterxml.jackson.core     : jackson-databind          : 2.9.0
//     com.fasterxml.jackson.datatype : jackson-datatype-jsr310   : 2.9.0
//
// Import this package:
//
//     import com.fox.ysmu.geckolib.file.geo.Converter;
//
// Then you can deserialize a JSON string with
//
//     GeoModel data = Converter.fromJsonString(jsonString);

package com.fox.ysmu.geckolib3.geo.raw.pojo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Converter {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    public static RawGeoModel fromJsonString(String json) {
        return GSON.fromJson(json, RawGeoModel.class);
    }
}

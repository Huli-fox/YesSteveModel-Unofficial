package software.bernie.geckolib3.file;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import software.bernie.geckolib3.geo.exception.GeoModelException;
import software.bernie.geckolib3.geo.raw.pojo.Converter;
import software.bernie.geckolib3.geo.raw.pojo.FormatVersion;
import software.bernie.geckolib3.geo.raw.pojo.RawGeoModel;
import software.bernie.geckolib3.geo.raw.tree.RawGeometryTree;
import software.bernie.geckolib3.geo.render.GeoBuilder;
import software.bernie.geckolib3.geo.render.built.GeoModel;

import java.io.File;

public class GeoModelLoader {
    private static GeoModelLoader instance;

    public static GeoModelLoader getInstance() {
        if (instance == null) {
            instance = new GeoModelLoader();
        }
        return instance;
    }

    public GeoModel loadModel(IResourceManager resourceManager, ResourceLocation location) {
        try {
            // Deserialize from json into basic json objects, bones are still stored as a
            // flat list
            RawGeoModel rawModel = Converter
                .fromJsonString(AnimationFileLoader.getResourceAsString(location, resourceManager));
            if (rawModel.getFormatVersion() != FormatVersion.VERSION_1_12_0) {
                throw new GeoModelException(location, "Wrong geometry json version, expected 1.12.0");
            }

            // Parse the flat list of bones into a raw hierarchical tree of "BoneGroup"s
            RawGeometryTree rawGeometryTree = RawGeometryTree.parseHierarchy(rawModel, location);

            // Build the quads and cubes from the raw tree into a built and ready to be
            // rendered GeoModel
            return GeoBuilder.getGeoBuilder(location.getResourceDomain()).constructGeoModel(rawGeometryTree);
        } catch (Exception e) {
            System.err.println("[GeckoLib] " + String.format("Error parsing %S", location) + e);
            throw (new RuntimeException(e));
        }
    }

    public GeoModel loadModelFromFile(File file, ResourceLocation location) {
        try {
            // Deserialize from json into basic json objects, bones are still stored as a
            // flat list
            RawGeoModel rawModel = Converter
                .fromJsonString(AnimationFileLoader.getResourceAsString(file, location));
            if (rawModel.getFormatVersion() != FormatVersion.VERSION_1_12_0) {
                throw new GeoModelException(location, "Wrong geometry json version, expected 1.12.0");
            }

            // Parse the flat list of bones into a raw hierarchical tree of "BoneGroup"s
            RawGeometryTree rawGeometryTree = RawGeometryTree.parseHierarchy(rawModel, location);

            // Build the quads and cubes from the raw tree into a built and ready to be
            // rendered GeoModel
            return GeoBuilder.getGeoBuilder(location.getResourceDomain()).constructGeoModel(rawGeometryTree);
        } catch (Exception e) {
            System.err.println("[GeckoLib] " + String.format("Error parsing %S", location) + e);
            throw (new RuntimeException(e));
        }
    }
}

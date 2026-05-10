package com.fox.ysmu.model.resource;

import org.apache.commons.lang3.StringUtils;

public final class YsmControllerResources {

    public static final String ANIMATION_MAP_PREFIX = "__ysm_controller__";

    private YsmControllerResources() {}

    public static boolean isControllerResource(String name) {
        return StringUtils.startsWith(name, ANIMATION_MAP_PREFIX);
    }

    public static String resourceName(String sourceName, int index) {
        String safeName = StringUtils.defaultIfBlank(sourceName, "controller_" + index)
            .replace('\\', '_')
            .replace('/', '_');
        return ANIMATION_MAP_PREFIX + safeName;
    }
}

package com.fox.ysmu.client.animation.controller;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.util.ResourceLocation;

import org.apache.commons.lang3.StringUtils;

import com.fox.ysmu.client.animation.controller.OpenYsmControllerDefinitions.AnimationEntry;
import com.fox.ysmu.client.animation.controller.OpenYsmControllerDefinitions.Controller;
import com.fox.ysmu.client.animation.controller.OpenYsmControllerDefinitions.ControllerSet;
import com.fox.ysmu.client.animation.controller.OpenYsmControllerDefinitions.State;
import com.fox.ysmu.client.animation.controller.OpenYsmControllerDefinitions.Transition;
import com.fox.ysmu.ysmu;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class OpenYsmAnimationControllerRegistry {

    private static final Map<ResourceLocation, ControllerSet> CONTROLLERS = new ConcurrentHashMap<>();
    private static final Set<String> WARNED = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private OpenYsmAnimationControllerRegistry() {}

    public static void register(ResourceLocation animationId, Iterable<byte[]> controllerFiles) {
        if (animationId == null || controllerFiles == null) {
            return;
        }
        ControllerSet set = new ControllerSet();
        int fileCount = 0;
        for (byte[] bytes : controllerFiles) {
            if (bytes == null || bytes.length == 0) {
                continue;
            }
            fileCount++;
            try {
                parseControllerFile(set, bytes);
            } catch (Exception e) {
                ysmu.LOG.warn("Failed to parse OpenYSM animation controller for {}", animationId, e);
            }
        }
        if (set.controllers.isEmpty()) {
            CONTROLLERS.remove(animationId);
            return;
        }
        CONTROLLERS.put(animationId, set);
        ysmu.LOG.info(
            "YSM client registered OpenYSM controllers for {}: files={}, controllers={}",
            animationId,
            fileCount,
            set.controllers.size());
    }

    static ControllerSet get(ResourceLocation animationId) {
        return CONTROLLERS.get(animationId);
    }

    public static void clear() {
        CONTROLLERS.clear();
        WARNED.clear();
        OpenYsmPlayerControllerRuntime.clear();
    }

    static void warnOnce(String key, String message) {
        if (WARNED.add(key)) {
            ysmu.LOG.warn(message);
        }
    }

    private static void parseControllerFile(ControllerSet set, byte[] bytes) {
        JsonElement element = new JsonParser().parse(new String(bytes, StandardCharsets.UTF_8));
        if (element == null || !element.isJsonObject()) {
            return;
        }
        JsonObject root = element.getAsJsonObject();
        if (!hasObject(root, "animation_controllers")) {
            return;
        }
        JsonObject controllers = root.getAsJsonObject("animation_controllers");
        for (Map.Entry<String, JsonElement> entry : controllers.entrySet()) {
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            Controller controller = parseController(entry.getKey(), entry.getValue().getAsJsonObject());
            if (!controller.states.isEmpty()) {
                set.controllers.put(controller.name, controller);
            }
        }
    }

    private static Controller parseController(String name, JsonObject json) {
        Controller controller = new Controller();
        controller.name = name;
        controller.initialState = getString(json, "initial_state", "");
        if (hasObject(json, "states")) {
            JsonObject states = json.getAsJsonObject("states");
            for (Map.Entry<String, JsonElement> entry : states.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                State state = parseState(entry.getKey(), entry.getValue().getAsJsonObject());
                controller.states.put(state.name, state);
            }
        }
        return controller;
    }

    private static State parseState(String name, JsonObject json) {
        State state = new State();
        state.name = name;
        parseAnimations(state, json.get("animations"));
        parseTransitions(state, json.get("transitions"));
        parseStringArray(state.onEntry, json.get("on_entry"));
        parseStringArray(state.onExit, json.get("on_exit"));
        parseSoundEffects(state, json.get("sound_effects"));
        parseBlendTransition(state, json.get("blend_transition"));
        state.blendViaShortestPath = getBoolean(json, "blend_via_shortest_path", false);
        return state;
    }

    private static void parseAnimations(State state, JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement entry : array) {
                parseAnimationEntry(state, entry);
            }
        } else {
            parseAnimationEntry(state, element);
        }
    }

    private static void parseAnimationEntry(State state, JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonPrimitive()) {
            state.animations.add(new AnimationEntry(element.getAsString(), ""));
        } else if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                state.animations.add(new AnimationEntry(entry.getKey(), getElementString(entry.getValue())));
            }
        }
    }

    private static void parseTransitions(State state, JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (!element.isJsonArray()) {
            return;
        }
        for (JsonElement entry : element.getAsJsonArray()) {
            if (!entry.isJsonObject()) {
                continue;
            }
            for (Map.Entry<String, JsonElement> transition : entry.getAsJsonObject().entrySet()) {
                state.transitions.add(new Transition(transition.getKey(), getElementString(transition.getValue())));
            }
        }
    }

    private static void parseStringArray(List<String> target, JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonArray()) {
            for (JsonElement entry : element.getAsJsonArray()) {
                if (!entry.isJsonNull()) {
                    target.add(entry.getAsString());
                }
            }
        } else {
            target.add(getElementString(element));
        }
    }

    private static void parseSoundEffects(State state, JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonArray()) {
            for (JsonElement entry : element.getAsJsonArray()) {
                addSoundEffect(state, entry);
            }
        } else {
            addSoundEffect(state, element);
        }
    }

    private static void addSoundEffect(State state, JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonObject()) {
            String effect = getString(element.getAsJsonObject(), "effect", "");
            if (StringUtils.isNotBlank(effect)) {
                state.soundEffects.add(effect);
            }
        } else {
            state.soundEffects.add(getElementString(element));
        }
    }

    private static void parseBlendTransition(State state, JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            state.blendTransitionTicks = Math.max(0f, element.getAsFloat() * 20f);
        } else if (element.isJsonObject()) {
            float maxSeconds = 0f;
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                try {
                    maxSeconds = Math.max(maxSeconds, Float.parseFloat(entry.getKey()));
                } catch (NumberFormatException ignored) {}
            }
            state.blendTransitionTicks = maxSeconds * 20f;
        }
    }

    private static boolean hasObject(JsonObject json, String name) {
        return json.has(name) && json.get(name).isJsonObject();
    }

    private static String getString(JsonObject json, String name, String defaultValue) {
        return json.has(name) && !json.get(name).isJsonNull() ? json.get(name).getAsString() : defaultValue;
    }

    private static boolean getBoolean(JsonObject json, String name, boolean defaultValue) {
        return json.has(name) && !json.get(name).isJsonNull() ? json.get(name).getAsBoolean() : defaultValue;
    }

    private static String getElementString(JsonElement element) {
        return element == null || element.isJsonNull() ? "" : element.getAsString();
    }

}

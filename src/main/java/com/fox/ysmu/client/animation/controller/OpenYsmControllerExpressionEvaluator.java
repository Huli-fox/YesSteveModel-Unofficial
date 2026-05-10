package com.fox.ysmu.client.animation.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;

import org.apache.commons.lang3.StringUtils;

import com.fox.ysmu.client.animation.RemotePlayerMotionStates;
import com.fox.ysmu.client.animation.condition.InnerClassify;
import com.fox.ysmu.compat.BackhandCompat;

import software.bernie.geckolib3.core.builder.Animation;
import software.bernie.geckolib3.core.event.predicate.AnimationEvent;

final class OpenYsmControllerExpressionEvaluator {

    private static final double TRUE = 1.0d;
    private static final double FALSE = 0.0d;

    private final String expression;
    private final Context context;
    private int index;

    private OpenYsmControllerExpressionEvaluator(String expression, Context context) {
        this.expression = expression == null ? "" : expression;
        this.context = context;
    }

    static boolean evaluateBoolean(String expression, Context context) {
        if (StringUtils.isBlank(expression)) {
            return true;
        }
        return truthy(evaluateNumber(expression, context));
    }

    static double evaluateNumber(String expression, Context context) {
        try {
            OpenYsmControllerExpressionEvaluator evaluator = new OpenYsmControllerExpressionEvaluator(expression, context);
            return evaluator.parseExpression();
        } catch (RuntimeException e) {
            OpenYsmAnimationControllerRegistry.warnOnce(
                "expr:" + expression,
                "Failed to evaluate OpenYSM controller expression: " + expression + " (" + e.getMessage() + ")");
            return FALSE;
        }
    }

    static void executeStatements(List<String> statements, Context context) {
        for (String statement : statements) {
            executeStatement(statement, context);
        }
    }

    private static void executeStatement(String statements, Context context) {
        if (StringUtils.isBlank(statements)) {
            return;
        }
        String[] split = statements.split(";");
        for (String statement : split) {
            String trimmed = statement.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int equals = findAssignmentOperator(trimmed);
            if (equals <= 0) {
                evaluateNumber(trimmed, context);
                continue;
            }
            String target = normalizeVariableName(trimmed.substring(0, equals).trim());
            String valueExpression = trimmed.substring(equals + 1).trim();
            if (target.startsWith("v.")) {
                context.state.variables.put(target.substring(2), evaluateNumber(valueExpression, context));
            }
        }
    }

    private double parseExpression() {
        return parseOr();
    }

    private double parseOr() {
        double value = parseAnd();
        while (true) {
            skipWhitespace();
            if (match("||")) {
                double right = parseAnd();
                value = truthy(value) || truthy(right) ? TRUE : FALSE;
            } else {
                return value;
            }
        }
    }

    private double parseAnd() {
        double value = parseEquality();
        while (true) {
            skipWhitespace();
            if (match("&&")) {
                double right = parseEquality();
                value = truthy(value) && truthy(right) ? TRUE : FALSE;
            } else {
                return value;
            }
        }
    }

    private double parseEquality() {
        double value = parseComparison();
        while (true) {
            skipWhitespace();
            if (match("==")) {
                value = nearlyEqual(value, parseComparison()) ? TRUE : FALSE;
            } else if (match("!=")) {
                value = !nearlyEqual(value, parseComparison()) ? TRUE : FALSE;
            } else {
                return value;
            }
        }
    }

    private double parseComparison() {
        double value = parseAdditive();
        while (true) {
            skipWhitespace();
            if (match(">=")) {
                value = value >= parseAdditive() ? TRUE : FALSE;
            } else if (match("<=")) {
                value = value <= parseAdditive() ? TRUE : FALSE;
            } else if (match(">")) {
                value = value > parseAdditive() ? TRUE : FALSE;
            } else if (match("<")) {
                value = value < parseAdditive() ? TRUE : FALSE;
            } else {
                return value;
            }
        }
    }

    private double parseAdditive() {
        double value = parseMultiplicative();
        while (true) {
            skipWhitespace();
            if (match("+")) {
                value += parseMultiplicative();
            } else if (match("-")) {
                value -= parseMultiplicative();
            } else {
                return value;
            }
        }
    }

    private double parseMultiplicative() {
        double value = parseUnary();
        while (true) {
            skipWhitespace();
            if (match("*")) {
                value *= parseUnary();
            } else if (match("/")) {
                double divisor = parseUnary();
                value = divisor == 0.0d ? 0.0d : value / divisor;
            } else if (match("%")) {
                double divisor = parseUnary();
                value = divisor == 0.0d ? 0.0d : value % divisor;
            } else {
                return value;
            }
        }
    }

    private double parseUnary() {
        skipWhitespace();
        if (match("!")) {
            return truthy(parseUnary()) ? FALSE : TRUE;
        }
        if (match("-")) {
            return -parseUnary();
        }
        return parsePrimary();
    }

    private double parsePrimary() {
        skipWhitespace();
        if (index >= expression.length()) {
            return FALSE;
        }
        char c = expression.charAt(index);
        if (c == '(') {
            index++;
            double value = parseExpression();
            match(")");
            return value;
        }
        if (c == '\'' || c == '"') {
            readQuotedString();
            return FALSE;
        }
        if (Character.isDigit(c) || (c == '.' && index + 1 < expression.length()
            && Character.isDigit(expression.charAt(index + 1)))) {
            return parseNumber();
        }
        String identifier = parseIdentifier();
        if (identifier.isEmpty()) {
            index++;
            return FALSE;
        }
        skipWhitespace();
        if (match("(")) {
            List<Argument> arguments = parseArguments();
            return context.functionValue(identifier, arguments);
        }
        return context.variableValue(identifier);
    }

    private List<Argument> parseArguments() {
        List<Argument> arguments = new ArrayList<>();
        while (true) {
            skipWhitespace();
            if (match(")")) {
                return arguments;
            }
            if (index >= expression.length()) {
                return arguments;
            }
            char c = expression.charAt(index);
            if (c == '\'' || c == '"') {
                arguments.add(Argument.string(readQuotedString()));
            } else {
                String raw = readRawArgument();
                arguments.add(Argument.number(evaluateNumber(raw, context)));
            }
            skipWhitespace();
            if (match(",")) {
                continue;
            }
            match(")");
            return arguments;
        }
    }

    private String readRawArgument() {
        int start = index;
        int depth = 0;
        boolean quoted = false;
        char quote = 0;
        while (index < expression.length()) {
            char c = expression.charAt(index);
            if (quoted) {
                if (c == quote) {
                    quoted = false;
                }
            } else if (c == '\'' || c == '"') {
                quoted = true;
                quote = c;
            } else if (c == '(') {
                depth++;
            } else if (c == ')') {
                if (depth == 0) {
                    break;
                }
                depth--;
            } else if (c == ',' && depth == 0) {
                break;
            }
            index++;
        }
        return expression.substring(start, index).trim();
    }

    private String readQuotedString() {
        char quote = expression.charAt(index++);
        StringBuilder out = new StringBuilder();
        while (index < expression.length()) {
            char c = expression.charAt(index++);
            if (c == quote) {
                break;
            }
            if (c == '\\' && index < expression.length()) {
                out.append(expression.charAt(index++));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private double parseNumber() {
        int start = index;
        while (index < expression.length()) {
            char c = expression.charAt(index);
            if (!Character.isDigit(c) && c != '.') {
                break;
            }
            index++;
        }
        return Double.parseDouble(expression.substring(start, index));
    }

    private String parseIdentifier() {
        int start = index;
        while (index < expression.length()) {
            char c = expression.charAt(index);
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '.' && c != '$' && c != ':') {
                break;
            }
            index++;
        }
        return expression.substring(start, index);
    }

    private boolean match(String token) {
        skipWhitespace();
        if (expression.startsWith(token, index)) {
            index += token.length();
            return true;
        }
        return false;
    }

    private void skipWhitespace() {
        while (index < expression.length() && Character.isWhitespace(expression.charAt(index))) {
            index++;
        }
    }

    private static boolean truthy(double value) {
        return Math.abs(value) > 0.000001d;
    }

    private static boolean nearlyEqual(double left, double right) {
        return Math.abs(left - right) < 0.000001d;
    }

    private static String normalizeVariableName(String variable) {
        if (variable.startsWith("variable.")) {
            return "v." + variable.substring("variable.".length());
        }
        return variable;
    }

    private static int findAssignmentOperator(String statement) {
        for (int i = 0; i < statement.length(); i++) {
            if (statement.charAt(i) != '=') {
                continue;
            }
            char before = i > 0 ? statement.charAt(i - 1) : 0;
            char after = i + 1 < statement.length() ? statement.charAt(i + 1) : 0;
            if (before != '=' && before != '!' && before != '<' && before != '>' && after != '=') {
                return i;
            }
        }
        return -1;
    }

    static final class Context {
        private final AnimationEvent<?> event;
        private final EntityPlayer player;
        private final OpenYsmPlayerControllerRuntime.RuntimeState state;

        Context(AnimationEvent<?> event, EntityPlayer player, OpenYsmPlayerControllerRuntime.RuntimeState state) {
            this.event = event;
            this.player = player;
            this.state = state;
        }

        double variableValue(String name) {
            String normalized = normalizeVariableName(name);
            if ("true".equals(normalized)) {
                return TRUE;
            }
            if ("false".equals(normalized)) {
                return FALSE;
            }
            if (normalized.startsWith("q.")) {
                normalized = "query." + normalized.substring(2);
            }
            if (normalized.startsWith("v.")) {
                return localVariableValue(normalized.substring(2));
            }
            if (normalized.startsWith("query.")) {
                return queryValue(normalized.substring("query.".length()));
            }
            if (normalized.startsWith("ctrl.")) {
                return ctrlValue(normalized.substring("ctrl.".length()));
            }
            if (normalized.startsWith("ysm.")) {
                return ysmValue(normalized.substring("ysm.".length()));
            }
            OpenYsmAnimationControllerRegistry.warnOnce(
                "var:" + normalized,
                "Unsupported OpenYSM controller variable: " + normalized);
            return FALSE;
        }

        double functionValue(String name, List<Argument> arguments) {
            if ("ctrl.hold".equals(name)) {
                return handMatch(arguments, false, false);
            }
            if ("ctrl.use".equals(name)) {
                return handMatch(arguments, true, false);
            }
            if ("ctrl.swing".equals(name)) {
                return handMatch(arguments, false, true);
            }
            if ("ctrl.ride".equals(name)) {
                return player.isRiding() ? TRUE : FALSE;
            }
            OpenYsmAnimationControllerRegistry.warnOnce(
                "func:" + name,
                "Unsupported OpenYSM controller function: " + name);
            return FALSE;
        }

        private double localVariableValue(String name) {
            if ("jump".equals(name)) {
                return isJumping() ? TRUE : FALSE;
            }
            Double value = state.variables.get(name);
            return value == null ? FALSE : value;
        }

        private double queryValue(String name) {
            if ("anim_time".equals(name)) {
                return Math.max(0.0d, event.getAnimationTick() - state.enteredTick) / 20.0d;
            }
            if ("life_time".equals(name)) {
                return event.getAnimationTick() / 20.0d;
            }
            if ("all_animations_finished".equals(name) || "any_animation_finished".equals(name)) {
                return allAnimationsFinished() ? TRUE : FALSE;
            }
            if ("ground_speed".equals(name)) {
                return horizontalSpeed();
            }
            if ("vertical_speed".equals(name)) {
                return (player.posY - player.prevPosY) * 20.0d;
            }
            if ("modified_distance_moved".equals(name)) {
                return player.distanceWalkedModified;
            }
            if ("walk_distance".equals(name)) {
                return player.distanceWalkedOnStepModified;
            }
            if ("is_on_ground".equals(name)) {
                return isOnGround() ? TRUE : FALSE;
            }
            if ("is_sneaking".equals(name)) {
                return isOnGround() && player.isSneaking() ? TRUE : FALSE;
            }
            if ("is_sprinting".equals(name)) {
                return player.isSprinting() ? TRUE : FALSE;
            }
            if ("is_swimming".equals(name) || "is_in_water".equals(name)) {
                return player.isInWater() ? TRUE : FALSE;
            }
            if ("is_in_water_or_rain".equals(name)) {
                return player.isWet() ? TRUE : FALSE;
            }
            if ("is_using_item".equals(name)) {
                return player.isUsingItem() ? TRUE : FALSE;
            }
            if ("is_jumping".equals(name)) {
                return isJumping() ? TRUE : FALSE;
            }
            if ("is_riding".equals(name)) {
                return player.isRiding() ? TRUE : FALSE;
            }
            if ("is_sleeping".equals(name)) {
                return player.isPlayerSleeping() ? TRUE : FALSE;
            }
            if ("is_on_fire".equals(name)) {
                return player.isBurning() ? TRUE : FALSE;
            }
            if ("is_playing_dead".equals(name)) {
                return player.isDead ? TRUE : FALSE;
            }
            if ("is_eating".equals(name)) {
                return player.getItemInUse() != null && player.getItemInUse().getItemUseAction() == EnumAction.eat
                    ? TRUE
                    : FALSE;
            }
            if ("health".equals(name)) {
                return player.getHealth();
            }
            if ("max_health".equals(name)) {
                return player.getMaxHealth();
            }
            if ("hurt_time".equals(name)) {
                return player.hurtTime;
            }
            if ("time_of_day".equals(name) || "time_stamp".equals(name)) {
                Minecraft mc = Minecraft.getMinecraft();
                return mc.theWorld == null ? 0.0d : mc.theWorld.getWorldTime();
            }
            OpenYsmAnimationControllerRegistry.warnOnce(
                "query:" + name,
                "Unsupported OpenYSM controller query: query." + name);
            return FALSE;
        }

        private double ctrlValue(String name) {
            return isControllerState(name) ? TRUE : FALSE;
        }

        private double ysmValue(String name) {
            if ("is_fishing".equals(name)) {
                return player.fishEntity != null ? TRUE : FALSE;
            }
            if ("swinging".equals(name)) {
                return player.isSwingInProgress ? TRUE : FALSE;
            }
            if ("swing_time".equals(name)) {
                return player.swingProgressInt;
            }
            if ("swinging_arm".equals(name)) {
                return BackhandCompat.swingingArm(player) ? 0.0d : 1.0d;
            }
            if ("is_passenger".equals(name)) {
                return player.isRiding() ? TRUE : FALSE;
            }
            if ("is_sleep".equals(name)) {
                return player.isPlayerSleeping() ? TRUE : FALSE;
            }
            if ("is_sneak".equals(name)) {
                return isOnGround() && player.isSneaking() ? TRUE : FALSE;
            }
            if ("on_ladder".equals(name)) {
                return player.isOnLadder() ? TRUE : FALSE;
            }
            if ("is_riptide".equals(name)) {
                return FALSE;
            }
            if ("has_mainhand".equals(name)) {
                return player.getHeldItem() != null ? TRUE : FALSE;
            }
            if ("has_offhand".equals(name)) {
                return BackhandCompat.getOffhandItem(player) != null ? TRUE : FALSE;
            }
            if ("mainhand_charged_crossbow".equals(name) || "offhand_charged_crossbow".equals(name)) {
                return FALSE;
            }
            if ("armor_value".equals(name)) {
                return player.getTotalArmorValue();
            }
            if ("hurt_time".equals(name)) {
                return player.hurtTime;
            }
            if ("food_level".equals(name)) {
                return player.getFoodStats().getFoodLevel();
            }
            OpenYsmAnimationControllerRegistry.warnOnce(
                "ysm:" + name,
                "Unsupported OpenYSM controller ysm variable: ysm." + name);
            return FALSE;
        }

        private boolean isControllerState(String name) {
            if ("idle".equals(name)) {
                return !hasNonIdleControllerState();
            }
            return isControllerStateDirect(name);
        }

        private boolean hasNonIdleControllerState() {
            return isControllerStateDirect("death")
                || isControllerStateDirect("sleep")
                || isControllerStateDirect("swim")
                || isControllerStateDirect("climb")
                || isControllerStateDirect("climbing")
                || isControllerStateDirect("ladder_up")
                || isControllerStateDirect("ladder_stillness")
                || isControllerStateDirect("ladder_down")
                || isControllerStateDirect("fly")
                || isControllerStateDirect("swim_stand")
                || isControllerStateDirect("attacked")
                || isControllerStateDirect("jump")
                || isControllerStateDirect("sneak")
                || isControllerStateDirect("sneaking")
                || isControllerStateDirect("run")
                || isControllerStateDirect("walk");
        }

        private boolean isControllerStateDirect(String name) {
            if ("death".equals(name)) {
                return player.isDead;
            }
            if ("sleep".equals(name)) {
                return player.isPlayerSleeping();
            }
            if ("swim".equals(name)) {
                return player.isInWater() && Math.abs(event.getLimbSwingAmount()) > 0.05f;
            }
            if ("climb".equals(name) || "climbing".equals(name)) {
                return player.isOnLadder();
            }
            if ("ladder_up".equals(name)) {
                return player.isOnLadder() && motionYState(0.1d) > 0;
            }
            if ("ladder_stillness".equals(name)) {
                return player.isOnLadder() && motionYState(0.1d) == 0;
            }
            if ("ladder_down".equals(name)) {
                return player.isOnLadder() && motionYState(0.1d) < 0;
            }
            if ("ride_pig".equals(name)) {
                return player.ridingEntity instanceof EntityPig;
            }
            if ("boat".equals(name)) {
                return player.ridingEntity instanceof EntityBoat;
            }
            if ("ride".equals(name) || "sit".equals(name)) {
                return player.isRiding();
            }
            if ("fly".equals(name)) {
                return isFlying();
            }
            if ("swim_stand".equals(name)) {
                return player.isInWater();
            }
            if ("attacked".equals(name)) {
                return player.hurtTime > 0;
            }
            if ("jump".equals(name)) {
                return isJumping();
            }
            if ("sneak".equals(name)) {
                return isOnGround() && player.isSneaking() && Math.abs(event.getLimbSwingAmount()) > 0.05f;
            }
            if ("sneaking".equals(name)) {
                return isOnGround() && player.isSneaking();
            }
            if ("run".equals(name)) {
                return isOnGround() && player.isSprinting();
            }
            if ("walk".equals(name)) {
                return isOnGround() && event.getLimbSwingAmount() > 0.05f;
            }
            return false;
        }

        private double handMatch(List<Argument> arguments, boolean requireUse, boolean requireSwing) {
            String hand = arguments.size() > 0 ? arguments.get(0).asString() : "mainhand";
            String matcher = arguments.size() > 1 ? arguments.get(1).asString() : "";
            boolean mainHand = !"offhand".equals(hand);
            if (!mainHand && !BackhandCompat.isBackhandLoaded()) {
                return FALSE;
            }
            if (requireUse && (!player.isUsingItem() || BackhandCompat.getUsedItemHand(player) != mainHand)) {
                return FALSE;
            }
            if (requireSwing && (!player.isSwingInProgress || BackhandCompat.swingingArm(player) != mainHand)) {
                return FALSE;
            }
            ItemStack stack = BackhandCompat.getItemInHand(player, mainHand);
            return itemMatches(stack, matcher) ? TRUE : FALSE;
        }

        private boolean itemMatches(ItemStack stack, String matcher) {
            if (StringUtils.isBlank(matcher)) {
                return stack != null;
            }
            if ("empty".equals(matcher)) {
                return stack == null;
            }
            if (stack == null || stack.getItem() == null) {
                return false;
            }
            String id = itemId(stack);
            if (matcher.startsWith("$")) {
                return id.equals(matcher.substring(1).toLowerCase(Locale.ROOT));
            }
            if (matcher.startsWith("#")) {
                return false;
            }
            String category = matcher.startsWith(":") ? matcher.substring(1) : matcher;
            return itemCategoryMatches(stack, id, category.toLowerCase(Locale.ROOT));
        }

        private String itemId(ItemStack stack) {
            Object rawName = Item.itemRegistry.getNameForObject(stack.getItem());
            return rawName == null ? "" : rawName.toString().toLowerCase(Locale.ROOT);
        }

        private boolean itemCategoryMatches(ItemStack stack, String id, String category) {
            String itemType = InnerClassify.getItemType(stack);
            if (category.equals(itemType)) {
                return true;
            }
            if ("trident".equals(category) && "spear".equals(itemType)) {
                return true;
            }
            if ("spear".equals(category) || "trident".equals(category)) {
                return id.contains("spear") || id.contains("trident");
            }
            if (isKnownItemCategory(category)) {
                return false;
            }
            return id.contains(category);
        }

        private boolean isKnownItemCategory(String category) {
            return "sword".equals(category)
                || "axe".equals(category)
                || "pickaxe".equals(category)
                || "shovel".equals(category)
                || "hoe".equals(category)
                || "bow".equals(category)
                || "crossbow".equals(category)
                || "shield".equals(category)
                || "spear".equals(category)
                || "trident".equals(category)
                || "fishing_rod".equals(category)
                || "throwable_potion".equals(category);
        }

        private boolean allAnimationsFinished() {
            Animation current = event.getController() == null ? null : event.getController().getCurrentAnimation();
            if (current == null || current.animationLength == null || current.animationLength <= 0.0d) {
                return false;
            }
            return event.getAnimationTick() - state.enteredTick >= current.animationLength;
        }

        private boolean isOnGround() {
            if (player == Minecraft.getMinecraft().thePlayer) {
                return player.onGround;
            }
            return RemotePlayerMotionStates.isOnGround(player);
        }

        private boolean isFlying() {
            if (player == Minecraft.getMinecraft().thePlayer) {
                return player.capabilities.isFlying;
            }
            return RemotePlayerMotionStates.isFlying(player);
        }

        private boolean isJumping() {
            return !isFlying() && !player.isRiding() && !isOnGround() && !player.isInWater()
                && motionYState(0.0d) != 0;
        }

        private double horizontalSpeed() {
            double x = player.posX - player.prevPosX;
            double z = player.posZ - player.prevPosZ;
            return MathHelper.sqrt_double(x * x + z * z) * 20.0d;
        }

        private int motionYState(double threshold) {
            double motionY = player == Minecraft.getMinecraft().thePlayer ? player.motionY
                : (player.posY - player.prevPosY) * 2.0d;
            if (motionY > threshold) {
                return 1;
            }
            if (motionY < -threshold) {
                return -1;
            }
            return 0;
        }
    }

    static final class Argument {
        private final String stringValue;
        private final double numberValue;
        private final boolean string;

        private Argument(String stringValue, double numberValue, boolean string) {
            this.stringValue = stringValue;
            this.numberValue = numberValue;
            this.string = string;
        }

        static Argument string(String value) {
            return new Argument(value, 0.0d, true);
        }

        static Argument number(double value) {
            return new Argument("", value, false);
        }

        String asString() {
            return string ? stringValue : Double.toString(numberValue);
        }
    }
}

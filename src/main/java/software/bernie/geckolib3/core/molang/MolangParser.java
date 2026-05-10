package software.bernie.geckolib3.core.molang;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleSupplier;

import com.eliotlash.mclib.math.Constant;
import com.eliotlash.mclib.math.IValue;
import com.eliotlash.mclib.math.MathBuilder;
import com.eliotlash.mclib.math.Variable;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import software.bernie.geckolib3.core.molang.expressions.MolangAssignment;
import software.bernie.geckolib3.core.molang.expressions.MolangExpression;
import software.bernie.geckolib3.core.molang.expressions.MolangMultiStatement;
import software.bernie.geckolib3.core.molang.expressions.MolangValue;
import software.bernie.geckolib3.core.molang.functions.BonePosition;
import software.bernie.geckolib3.core.molang.functions.BoneRotation;
import software.bernie.geckolib3.core.molang.functions.BoneScale;
import software.bernie.geckolib3.core.molang.functions.CosDegrees;
import software.bernie.geckolib3.core.molang.functions.FirstOrder;
import software.bernie.geckolib3.core.molang.functions.SecondOrder;
import software.bernie.geckolib3.core.molang.functions.SinDegrees;

/**
 * MoLang 解析器
 * <a href="https://bedrock.dev/docs/1.19.0.0/1.19.30.23/Molang#Math%20Functions">Wiki</a>
 */
public class MolangParser extends MathBuilder {

    public static final Map<String, LazyVariable> VARIABLES = new Object2ObjectOpenHashMap<>();
    public static final MolangExpression ZERO = new MolangValue(null, new Constant(0));
    public static final MolangExpression ONE = new MolangValue(null, new Constant(1));
    public static final String RETURN = "return ";

    public MolangParser() {
        super();
        // 将函数重新映射为 MoLang 标准名
        doCoreRemaps();
    }

    private void doCoreRemaps() {
        // 将 sin 和 cos 改成角度参数
        this.functions.put("cos", CosDegrees.class);
        this.functions.put("sin", SinDegrees.class);
        this.functions.put("ysm.first_order", FirstOrder.class);
        this.functions.put("ysm.second_order", SecondOrder.class);
        this.functions.put("ysm.bone_rot_x", BoneRotation.class);
        this.functions.put("ysm.bone_rot_y", BoneRotation.class);
        this.functions.put("ysm.bone_rot_z", BoneRotation.class);
        this.functions.put("ysm.bone_pos_x", BonePosition.class);
        this.functions.put("ysm.bone_pos_y", BonePosition.class);
        this.functions.put("ysm.bone_pos_z", BonePosition.class);
        this.functions.put("ysm.bone_position_x", BonePosition.class);
        this.functions.put("ysm.bone_position_y", BonePosition.class);
        this.functions.put("ysm.bone_position_z", BonePosition.class);
        this.functions.put("ysm.bone_scale_x", BoneScale.class);
        this.functions.put("ysm.bone_scale_y", BoneScale.class);
        this.functions.put("ysm.bone_scale_z", BoneScale.class);

        remap("abs", "math.abs");
        remap("acos", "math.acos");
        remap("asin", "math.asin");
        remap("atan", "math.atan");
        remap("atan2", "math.atan2");
        remap("ceil", "math.ceil");
        remap("clamp", "math.clamp");
        remap("cos", "math.cos");
        remap("die_roll", "math.die_roll");
        remap("die_roll_integer", "math.die_roll_integer");
        remap("exp", "math.exp");
        remap("floor", "math.floor");
        remap("hermite_blend", "math.hermite_blend");
        remap("lerp", "math.lerp");
        remap("lerprotate", "math.lerprotate");
        remap("ln", "math.ln");
        remap("max", "math.max");
        remap("min", "math.min");
        remap("mod", "math.mod");
        remap("pi", "math.pi");
        remap("pow", "math.pow");
        remap("random", "math.random");
        remap("random_integer", "math.random_integer");
        remap("round", "math.round");
        remap("sin", "math.sin");
        remap("sqrt", "math.sqrt");
        remap("trunc", "math.trunc");
    }

    @Override

    public void register(Variable variable) {
        if (!(variable instanceof LazyVariable)) {
            variable = LazyVariable.from(variable);
        }
        String name = normalizeVariableName(variable.getName());
        if (name.startsWith("v.") && !(variable instanceof ScopedMolangVariable)) {
            Variable fallback = variable;
            variable = new ScopedMolangVariable(name, fallback::get);
        }
        VARIABLES.put(name, (LazyVariable) variable);
    }

    /**
     * 重映射方法
     */
    public void remap(String old, String newName) {
        this.functions.put(newName, this.functions.remove(old));
    }

    @Deprecated
    public void setValue(String name, double value) {
        setValue(name, () -> value);
    }

    public void setValue(String name, DoubleSupplier value) {
        LazyVariable variable = getVariable(name);
        if (variable != null) {
            variable.set(value);
        }
    }

    @Override

    protected LazyVariable getVariable(String name) {
        name = normalizeVariableName(name);
        return VARIABLES.computeIfAbsent(name, MolangParser::newVariable);
    }

    public LazyVariable getVariable(String name, MolangMultiStatement currentStatement) {
        name = normalizeVariableName(name);
        LazyVariable variable;
        if (currentStatement != null) {
            variable = currentStatement.locals.get(name);
            if (variable != null) {
                return variable;
            }
        }
        return getVariable(name);
    }

    private static String normalizeVariableName(String name) {
        if (name.startsWith("q.")) {
            return "query." + name.substring(2);
        }
        if (name.startsWith("variable.")) {
            return "v." + name.substring("variable.".length());
        }
        return name;
    }

    private static LazyVariable newVariable(String key) {
        return key.startsWith("v.") ? new ScopedMolangVariable(key, 0) : new LazyVariable(key, 0);
    }

    public MolangExpression parseJson(JsonElement element) throws MolangException {
        if (!element.isJsonPrimitive()) {
            return ZERO;
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isNumber()) {
            return new MolangValue(this, new Constant(primitive.getAsDouble()));
        }
        if (primitive.isString()) {
            String string = primitive.getAsString();
            try {
                return new MolangValue(this, new Constant(Double.parseDouble(string)));
            } catch (NumberFormatException ex) {
                return parseExpression(string);
            }
        }
        return ZERO;
    }

    /**
     * 解析一个 MoLang 表达式
     */
    public MolangExpression parseExpression(String expression) throws MolangException {
        MolangMultiStatement result = null;
        for (String split : splitStatements(lowerCaseOutsideStrings(expression).trim())) {
            String trimmed = split.trim();
            if (!trimmed.isEmpty()) {
                if (result == null) {
                    result = new MolangMultiStatement(this);
                }
                result.expressions.add(parseOneLine(trimmed, result));
            }
        }
        if (result == null) {
            throw new MolangException("Molang expression cannot be blank!");
        }
        return result;
    }

    public static List<String> splitStatements(String expression) throws MolangException {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder(expression.length());
        boolean quoted = false;
        char quote = 0;
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (quoted) {
                current.append(c);
                if (c == '\\' && i + 1 < expression.length()) {
                    current.append(expression.charAt(++i));
                } else if (c == quote) {
                    quoted = false;
                }
            } else if (c == '\'' || c == '"') {
                quoted = true;
                quote = c;
                current.append(c);
            } else if (c == ';') {
                statements.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (quoted) {
            throw new MolangException("Unclosed string literal in Molang expression!");
        }
        statements.add(current.toString());
        return statements;
    }

    @Override
    public String[] breakdown(String expression) throws Exception {
        return super.breakdown(rewriteOpenYsmExpression(expression));
    }

    /**
     * 解析单个 MoLang 表达式
     */
    protected MolangExpression parseOneLine(String expression, MolangMultiStatement currentStatement)
        throws MolangException {
        if (expression.startsWith(RETURN)) {
            try {
                return new MolangValue(this, parse(expression.substring(RETURN.length()))).addReturn();
            } catch (Exception e) {
                throw new MolangException("Couldn't parse return '" + expression + "' expression!");
            }
        }

        try {
            // 将表达式拆分
            List<Object> symbols = breakdownChars(this.breakdown(expression));
            // 如果是赋值表达式
            if (symbols.size() >= 3 && (symbols.get(0) instanceof String name)
                && isVariable(symbols.get(0))
                && symbols.get(1)
                    .equals("=")) {
                symbols = symbols.subList(2, symbols.size());
                name = normalizeVariableName(name);
                LazyVariable variable;
                if (!name.startsWith("v.")
                    && !VARIABLES.containsKey(name)
                    && !currentStatement.locals.containsKey(name)) {
                    currentStatement.locals.put(name, (variable = new LazyVariable(name, 0)));
                } else {
                    variable = getVariable(name, currentStatement);
                }
                return new MolangAssignment(this, variable, parseSymbolsMolang(symbols));
            }
            // 如果是其他表达式
            return new MolangValue(this, parseSymbolsMolang(symbols));
        } catch (Exception e) {
            throw new MolangException("Couldn't parse '" + expression + "' expression!");
        }
    }

    /**
     * 将 parseSymbols 方法包装，并抛出 MolangException
     */
    private IValue parseSymbolsMolang(List<Object> symbols) throws MolangException {
        try {
            return this.parseSymbols(symbols);
        } catch (Exception e) {
            e.printStackTrace();
            throw new MolangException("Couldn't parse an expression!");
        }
    }

    /**
     * 拓展此方法，从而让 {@link #breakdownChars(String[])} 能够解析等号
     * 这样就能更加轻松解析赋值表达式
     */
    @Override

    protected boolean isOperator(String s) {
        return super.isOperator(s) || s.equals("=");
    }

    private static String lowerCaseOutsideStrings(String expression) {
        StringBuilder out = new StringBuilder(expression.length());
        boolean quoted = false;
        char quote = 0;
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (quoted) {
                out.append(c);
                if (c == '\\' && i + 1 < expression.length()) {
                    out.append(expression.charAt(++i));
                } else if (c == quote) {
                    quoted = false;
                }
            } else if (c == '\'' || c == '"') {
                quoted = true;
                quote = c;
                out.append(c);
            } else {
                out.append(Character.toLowerCase(c));
            }
        }
        return out.toString();
    }

    private static String rewriteOpenYsmExpression(String expression) throws MolangException {
        String rewritten = replaceStringLiterals(expression);
        rewritten = rewriteVectorFunction(rewritten, "ysm.bone_rot", "ysm.bone_rot");
        rewritten = rewriteVectorFunction(rewritten, "ysm.bone_pos", "ysm.bone_pos");
        rewritten = rewriteVectorFunction(rewritten, "ysm.bone_position", "ysm.bone_position");
        rewritten = rewriteVectorFunction(rewritten, "ysm.bone_scale", "ysm.bone_scale");
        return rewritten;
    }

    private static String replaceStringLiterals(String expression) throws MolangException {
        StringBuilder out = new StringBuilder(expression.length());
        boolean quoted = false;
        char quote = 0;
        StringBuilder literal = new StringBuilder();
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (quoted) {
                if (c == '\\' && i + 1 < expression.length()) {
                    literal.append(expression.charAt(++i));
                } else if (c == quote) {
                    quoted = false;
                    out.append(MolangStringPool.intern(literal.toString()));
                    literal.setLength(0);
                } else {
                    literal.append(c);
                }
            } else if (c == '\'' || c == '"') {
                quoted = true;
                quote = c;
            } else {
                out.append(c);
            }
        }
        if (quoted) {
            throw new MolangException("Unclosed string literal in Molang expression!");
        }
        return out.toString();
    }

    private static String rewriteVectorFunction(String expression, String sourceFunction, String targetPrefix) {
        String out = expression;
        for (char axis : new char[] { 'x', 'y', 'z' }) {
            String suffix = ")." + axis;
            int searchFrom = 0;
            while (true) {
                int functionStart = out.indexOf(sourceFunction + "(", searchFrom);
                if (functionStart < 0) {
                    break;
                }
                int argsStart = functionStart + sourceFunction.length() + 1;
                int argsEnd = findMatchingParen(out, argsStart - 1);
                if (argsEnd < 0 || !out.startsWith(suffix, argsEnd)) {
                    searchFrom = argsStart;
                    continue;
                }
                String args = out.substring(argsStart, argsEnd);
                String replacement = targetPrefix + "_" + axis + "(" + args + ")";
                out = out.substring(0, functionStart) + replacement + out.substring(argsEnd + suffix.length());
                searchFrom = functionStart + replacement.length();
            }
        }
        return out;
    }

    private static int findMatchingParen(String expression, int openIndex) {
        int depth = 0;
        for (int i = openIndex; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
}

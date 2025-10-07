//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.eliotlash.molang.expressions;

import com.eliotlash.mclib.math.Constant;
import com.eliotlash.mclib.math.IValue;
import com.eliotlash.mclib.math.Operation;
import com.eliotlash.molang.MolangParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import software.bernie.geckolib3.molang.MolangRegistrar;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public abstract class MolangExpression implements IValue, Externalizable {
    public MolangParser context;

    public MolangExpression() {
    }

    public static boolean isZero(MolangExpression expression) {
        return isConstant(expression, 0.0);
    }

    public static boolean isOne(MolangExpression expression) {
        return isConstant(expression, 1.0);
    }

    public static boolean isConstant(MolangExpression expression, double x) {
        if (!(expression instanceof MolangValue)) {
            return false;
        } else {
            MolangValue value = (MolangValue) expression;
            return value.value instanceof Constant && Operation.equals(value.value.get(), x);
        }
    }

    public static boolean isExpressionConstant(MolangExpression expression) {
        if (expression instanceof MolangValue) {
            MolangValue value = (MolangValue) expression;
            return value.value instanceof Constant;
        } else {
            return false;
        }
    }

    public MolangExpression(MolangParser context) {
        this.context = context;
    }

    public JsonElement toJson() {
        return new JsonPrimitive(this.toString());
    }

    public void writeExternal(ObjectOutput out) throws IOException {

    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        context = MolangRegistrar.getParser();
    }
}

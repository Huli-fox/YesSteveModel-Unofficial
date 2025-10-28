package com.fox.ysmu.geckolib3.core.molang.expressions;

import com.fox.ysmu.geckolib3.core.molang.MolangParser;
import com.fox.ysmu.mclib.math.IValue;
import com.fox.ysmu.mclib.math.Variable;
import com.fox.ysmu.util.Keep;

public class MolangAssignment extends MolangExpression {
    public Variable variable;
    public IValue expression;

    public MolangAssignment(MolangParser context, Variable variable, IValue expression) {
        super(context);
        this.variable = variable;
        this.expression = expression;
    }

    @Override
    @Keep
    public double get() {
        double value = this.expression.get();
        this.variable.set(value);
        return value;
    }

    @Override
    @Keep
    public String toString() {
        return this.variable.getName() + " = " + this.expression.toString();
    }
}

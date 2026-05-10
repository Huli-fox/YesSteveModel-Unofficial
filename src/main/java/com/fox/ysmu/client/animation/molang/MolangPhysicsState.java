package com.fox.ysmu.client.animation.molang;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class MolangPhysicsState {

    private static final String FIRST_ORDER_PREFIX = "first:";
    private static final String SECOND_ORDER_PREFIX = "second:";

    private final Map<String, IPhysicsValue> values = new ConcurrentHashMap<>();
    private double lastRenderTicks;

    void update(double renderTicks) {
        if (lastRenderTicks > 0.0D && renderTicks > lastRenderTicks) {
            float timeStep = (float) ((renderTicks - lastRenderTicks) / 20.0D);
            lastRenderTicks = renderTicks;
            if (timeStep > 0.0F) {
                for (IPhysicsValue value : values.values()) {
                    value.update(timeStep);
                }
            }
        } else if (lastRenderTicks <= 0.0D) {
            lastRenderTicks = renderTicks;
        }
    }

    double firstOrder(int nameId, double input, double response) {
        String key = FIRST_ORDER_PREFIX + nameId;
        IPhysicsValue value = values.get(key);
        if (!(value instanceof FirstOrderPhysics)) {
            value = new FirstOrderPhysics(input, response);
            values.put(key, value);
            return input;
        }
        value.setArgs(input, response, 0.0D, 0.0D);
        return value.getValue();
    }

    double secondOrder(int nameId, double input, double frequency, double coefficient, double response) {
        String key = SECOND_ORDER_PREFIX + nameId;
        IPhysicsValue value = values.get(key);
        if (!(value instanceof SecondOrderPhysics)) {
            value = new SecondOrderPhysics(input, frequency, coefficient, response);
            values.put(key, value);
            return input;
        }
        value.setArgs(input, frequency, coefficient, response);
        return value.getValue();
    }

    private interface IPhysicsValue {

        void update(float timeStep);

        void setArgs(double arg0, double arg1, double arg2, double arg3);

        double getValue();
    }

    private static final class FirstOrderPhysics implements IPhysicsValue {

        private double input;
        private double response;
        private double lastSimulation;

        private FirstOrderPhysics(double input, double response) {
            this.input = input;
            this.response = response;
        }

        @Override
        public void update(float timeStep) {
            lastSimulation = ((1.0D - (timeStep / response)) * lastSimulation) + ((timeStep / response) * input);
        }

        @Override
        public void setArgs(double input, double response, double arg2, double arg3) {
            this.input = input;
            this.response = response;
        }

        @Override
        public double getValue() {
            return lastSimulation;
        }
    }

    private static final class SecondOrderPhysics implements IPhysicsValue {

        private double inputFunction;
        private double lastSimulation;
        private double lastSimulationDot;
        private double input;
        private double frequency;
        private double coefficient;
        private double response;

        private SecondOrderPhysics(double input, double frequency, double coefficient, double response) {
            this.input = input;
            this.frequency = frequency;
            this.coefficient = coefficient;
            this.response = response;
        }

        @Override
        public void update(float timeStep) {
            if (timeStep <= 0.0F) {
                return;
            }
            double safeFrequency = Math.max(0.0001D, clamp(frequency, 0.0D, 5.0D));
            double safeCoefficient = clamp(coefficient, 0.0D, 1.0D);
            double k1 = safeCoefficient / Math.PI / safeFrequency;
            double k2 = 1.0D / (2.0D * Math.PI * safeFrequency) / (2.0D * Math.PI * safeFrequency);
            double k3 = response * safeCoefficient / 2.0D / Math.PI / safeFrequency;

            double inputFunctionDot = (input - inputFunction) / timeStep;
            inputFunction = input;

            double maxTimeStep = Math.sqrt(4.0D * k2 + k1 * k1) - k1;
            if (maxTimeStep <= 0.000001D || Double.isNaN(maxTimeStep) || Double.isInfinite(maxTimeStep)) {
                maxTimeStep = timeStep;
            }
            int cycleTime = (int) Math.max(1.0D, Math.ceil(timeStep / maxTimeStep));
            double step = timeStep / cycleTime;

            double simulation = lastSimulation;
            double simulationDot = lastSimulationDot;
            for (; cycleTime > 0; cycleTime--) {
                simulation = simulation + step * simulationDot;
                simulationDot = simulationDot
                    + step * (k3 * inputFunctionDot + input - simulation - k1 * simulationDot) / k2;
            }
            lastSimulation = simulation;
            lastSimulationDot = simulationDot;
        }

        @Override
        public void setArgs(double input, double frequency, double coefficient, double response) {
            this.input = input;
            this.frequency = frequency;
            this.coefficient = coefficient;
            this.response = response;
        }

        @Override
        public double getValue() {
            return lastSimulation;
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

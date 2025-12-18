package dev.loratech.guard.api;

import java.util.List;

public class ModerationRequest {

    private final Object input;
    private final String model;
    private final double threshold;

    public ModerationRequest(String input, String model, double threshold) {
        this.input = input;
        this.model = model;
        this.threshold = threshold;
    }

    public ModerationRequest(List<String> inputs, String model, double threshold) {
        this.input = inputs;
        this.model = model;
        this.threshold = threshold;
    }

    public Object getInput() {
        return input;
    }

    public String getModel() {
        return model;
    }

    public double getThreshold() {
        return threshold;
    }
}

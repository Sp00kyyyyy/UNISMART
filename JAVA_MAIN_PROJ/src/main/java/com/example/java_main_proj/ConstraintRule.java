package com.example.java_main_proj;

public class ConstraintRule {
    private final String name;
    private final String description;
    private final String type;
    private final int weight;

    public ConstraintRule(String name, String description, String type, int weight) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.weight = weight;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public int getWeight() {
        return weight;
    }

    public boolean isHardConstraint() {
        return "HARD".equalsIgnoreCase(type);
    }
}

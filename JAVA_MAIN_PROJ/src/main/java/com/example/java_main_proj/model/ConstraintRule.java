package com.example.java_main_proj.model;

/**
 * מייצג אילוץ או משקל החלטה כפי שהוגדר במסד הנתונים.
 */
public class ConstraintRule {
    private final String name;
    private final String description;
    private final String type;
    private final int weight;

    /**
     * יוצר כלל אילוץ או כלל משקל כפי שהוא נשמר במסד.
     *
     * @param name שם האילוץ
     * @param description תיאור מילולי של האילוץ
     * @param type סוג האילוץ, למשל קשיח או רך
     * @param weight משקל האילוץ בחישוב
     */
    public ConstraintRule(String name, String description, String type, int weight) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.weight = weight;
    }

    /**
     * @return שם האילוץ
     */
    public String getName() {
        return name;
    }

    /**
     * @return התיאור המילולי של האילוץ
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return סוג האילוץ
     */
    public String getType() {
        return type;
    }

    /**
     * @return משקל האילוץ
     */
    public int getWeight() {
        return weight;
    }

    /**
     * מאפשר להבחין בין אילוץ קשיח לבין העדפה רכה.
     *
     * @return {@code true} אם סוג האילוץ הוא קשיח
     */
    public boolean isHardConstraint() {
        return "HARD".equalsIgnoreCase(type);
    }
}

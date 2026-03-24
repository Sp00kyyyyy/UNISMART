package com.example.java_main_proj.model;

/**
 * העדפת קורס של סטודנט, יחד עם דירוג העדיפות שלה.
 */
public class CoursePreference {
    private final int courseId;
    private final int preferenceRank;

    /**
     * יוצר העדפת קורס אחת עבור סטודנט.
     *
     * @param courseId מזהה הקורס המועדף
     * @param preferenceRank דירוג ההעדפה
     */
    public CoursePreference(int courseId, int preferenceRank) {
        this.courseId = courseId;
        this.preferenceRank = preferenceRank;
    }

    /**
     * מחזיר את מזהה הקורס המועדף.
     *
     * @return מזהה הקורס המועדף
     */
    public int getCourseId() {
        return courseId;
    }

    /**
     * מחזיר את דירוג ההעדפה של הקורס.
     *
     * @return דירוג ההעדפה של הקורס
     */
    public int getPreferenceRank() {
        return preferenceRank;
    }
}

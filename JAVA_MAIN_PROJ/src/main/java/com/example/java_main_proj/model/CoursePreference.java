package com.example.java_main_proj.model;

public class CoursePreference {
    private final int courseId;
    private final int preferenceRank;

    public CoursePreference(int courseId, int preferenceRank) {
        this.courseId = courseId;
        this.preferenceRank = preferenceRank;
    }

    public int getCourseId() {
        return courseId;
    }

    public int getPreferenceRank() {
        return preferenceRank;
    }
}

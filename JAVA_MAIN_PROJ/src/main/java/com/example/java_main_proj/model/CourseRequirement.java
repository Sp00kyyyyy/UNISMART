package com.example.java_main_proj.model;

public class CourseRequirement {
    private final int courseId;
    private final String track;
    private final int year;
    private final boolean mandatory;

    public CourseRequirement(int courseId, String track, int year, boolean mandatory) {
        this.courseId = courseId;
        this.track = track;
        this.year = year;
        this.mandatory = mandatory;
    }

    public int getCourseId() {
        return courseId;
    }

    public String getTrack() {
        return track;
    }

    public int getYear() {
        return year;
    }

    public boolean isMandatory() {
        return mandatory;
    }
}

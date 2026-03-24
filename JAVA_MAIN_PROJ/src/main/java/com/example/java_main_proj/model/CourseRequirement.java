package com.example.java_main_proj.model;

/**
 * כלל שמגדיר האם קורס הוא חובה עבור מסלול ושנה מסוימים.
 */
public class CourseRequirement {
    private final int courseId;
    private final String track;
    private final int year;
    private final boolean mandatory;

    /**
     * יוצר כלל חובה עבור קורס, מסלול ושנה.
     *
     * @param courseId מזהה הקורס
     * @param track המסלול האקדמי
     * @param year שנת הלימוד
     * @param mandatory האם הקורס חובה
     */
    public CourseRequirement(int courseId, String track, int year, boolean mandatory) {
        this.courseId = courseId;
        this.track = track;
        this.year = year;
        this.mandatory = mandatory;
    }

    /**
     * מחזיר את מזהה הקורס שעליו חל הכלל.
     *
     * @return מזהה הקורס שעליו חל הכלל
     */
    public int getCourseId() {
        return courseId;
    }

    /**
     * מחזיר את המסלול האקדמי של הכלל.
     *
     * @return המסלול האקדמי שאליו הכלל שייך
     */
    public String getTrack() {
        return track;
    }

    /**
     * מחזיר את שנת הלימוד של הכלל.
     *
     * @return שנת הלימוד שעליה חל הכלל
     */
    public int getYear() {
        return year;
    }

    /**
     * מחזיר האם הקורס מוגדר כקורס חובה.
     *
     * @return {@code true} אם הקורס הוא קורס חובה
     */
    public boolean isMandatory() {
        return mandatory;
    }
}

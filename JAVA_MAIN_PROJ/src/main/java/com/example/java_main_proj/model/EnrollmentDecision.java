package com.example.java_main_proj.model;

/**
 * החלטת שיבוץ בודדת לפני שמירתה למסד הנתונים.
 */
public class EnrollmentDecision {
    private final int studentId;
    private final int courseId;
    private final String academicYear;
    private final String semester;
    private final double assignmentScore;
    private final int requestedRank;
    private final boolean mandatory;

    /**
     * יוצר החלטת שיבוץ בודדת לשמירה במסד הנתונים.
     *
     * @param studentId מזהה הסטודנט
     * @param courseId מזהה הקורס
     * @param academicYear שנת הלימודים
     * @param semester הסמסטר
     * @param assignmentScore ציון ההחלטה
     * @param requestedRank דירוג ההעדפה המקורי
     * @param mandatory האם מדובר בבקשת חובה
     */
    public EnrollmentDecision(
            int studentId,
            int courseId,
            String academicYear,
            String semester,
            double assignmentScore,
            int requestedRank,
            boolean mandatory
    ) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.academicYear = academicYear;
        this.semester = semester;
        this.assignmentScore = assignmentScore;
        this.requestedRank = requestedRank;
        this.mandatory = mandatory;
    }

    /**
     * מחזיר את מזהה הסטודנט.
     *
     * @return מזהה הסטודנט
     */
    public int getStudentId() {
        return studentId;
    }

    /**
     * מחזיר את מזהה הקורס.
     *
     * @return מזהה הקורס
     */
    public int getCourseId() {
        return courseId;
    }

    /**
     * מחזיר את שנת הלימודים של ההחלטה.
     *
     * @return שנת הלימודים של ההחלטה
     */
    public String getAcademicYear() {
        return academicYear;
    }

    /**
     * מחזיר את הסמסטר של ההחלטה.
     *
     * @return הסמסטר של ההחלטה
     */
    public String getSemester() {
        return semester;
    }

    /**
     * מחזיר את ציון השיבוץ של ההחלטה.
     *
     * @return ציון השיבוץ של ההחלטה
     */
    public double getAssignmentScore() {
        return assignmentScore;
    }

    /**
     * מחזיר את דירוג ההעדפה המקורי של הבקשה.
     *
     * @return דירוג ההעדפה המקורי של הבקשה
     */
    public int getRequestedRank() {
        return requestedRank;
    }

    /**
     * מחזיר האם מדובר בבקשת חובה.
     *
     * @return {@code true} אם ההחלטה מתייחסת לבקשת חובה
     */
    public boolean isMandatory() {
        return mandatory;
    }
}

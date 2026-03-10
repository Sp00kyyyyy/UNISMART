package com.example.java_main_proj.model;

public class EnrollmentDecision {
    private final int studentId;
    private final int courseId;
    private final String academicYear;
    private final String semester;
    private final double assignmentScore;
    private final int requestedRank;
    private final boolean mandatory;

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

    public int getStudentId() {
        return studentId;
    }

    public int getCourseId() {
        return courseId;
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public String getSemester() {
        return semester;
    }

    public double getAssignmentScore() {
        return assignmentScore;
    }

    public int getRequestedRank() {
        return requestedRank;
    }

    public boolean isMandatory() {
        return mandatory;
    }
}

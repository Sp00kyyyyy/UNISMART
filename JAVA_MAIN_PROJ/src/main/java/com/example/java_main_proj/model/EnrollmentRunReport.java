package com.example.java_main_proj.model;

import java.util.List;

public class EnrollmentRunReport {
    private final String academicYear;
    private final String semester;
    private final int studentsProcessed;
    private final int requestedCourses;
    private final int assignedCourses;
    private final int localImprovements;
    private final int fullAssignments;
    private final int partialAssignments;
    private final int unassignedStudents;
    private final List<String> logLines;

    public EnrollmentRunReport(
            String academicYear,
            String semester,
            int studentsProcessed,
            int requestedCourses,
            int assignedCourses,
            int localImprovements,
            int fullAssignments,
            int partialAssignments,
            int unassignedStudents,
            List<String> logLines
    ) {
        this.academicYear = academicYear;
        this.semester = semester;
        this.studentsProcessed = studentsProcessed;
        this.requestedCourses = requestedCourses;
        this.assignedCourses = assignedCourses;
        this.localImprovements = localImprovements;
        this.fullAssignments = fullAssignments;
        this.partialAssignments = partialAssignments;
        this.unassignedStudents = unassignedStudents;
        this.logLines = List.copyOf(logLines);
    }

    public String getAcademicYear() {
        return academicYear;
    }

    public String getSemester() {
        return semester;
    }

    public int getStudentsProcessed() {
        return studentsProcessed;
    }

    public int getRequestedCourses() {
        return requestedCourses;
    }

    public int getAssignedCourses() {
        return assignedCourses;
    }

    public int getLocalImprovements() {
        return localImprovements;
    }

    public int getFullAssignments() {
        return fullAssignments;
    }

    public int getPartialAssignments() {
        return partialAssignments;
    }

    public int getUnassignedStudents() {
        return unassignedStudents;
    }

    public List<String> getLogLines() {
        return logLines;
    }
}

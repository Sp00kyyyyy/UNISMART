package com.example.java_main_proj.model;

import java.util.List;

/**
 * אובייקט דוח שמרכז את תוצאות ריצת האלגוריתם למסך ההרצה.
 */
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

    /**
     * יוצר דוח ריצה מסכם עבור הרצת השיבוץ.
     *
     * @param academicYear שנת הלימודים של ההרצה
     * @param semester הסמסטר של ההרצה
     * @param studentsProcessed מספר הסטודנטים שטופלו
     * @param requestedCourses מספר הבקשות הכולל
     * @param assignedCourses מספר השיבוצים שבוצעו
     * @param localImprovements מספר השיפורים המקומיים שאושרו
     * @param fullAssignments מספר הסטודנטים שקיבלו שיבוץ מלא
     * @param partialAssignments מספר הסטודנטים שקיבלו שיבוץ חלקי
     * @param unassignedStudents מספר הסטודנטים שלא שובצו כלל
     * @param logLines שורות לוג של ההרצה
     */
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

    /**
     * @return שנת הלימודים של ההרצה
     */
    public String getAcademicYear() {
        return academicYear;
    }

    /**
     * @return הסמסטר של ההרצה
     */
    public String getSemester() {
        return semester;
    }

    /**
     * @return מספר הסטודנטים שטופלו בהרצה
     */
    public int getStudentsProcessed() {
        return studentsProcessed;
    }

    /**
     * @return מספר הבקשות הכולל שנבדק בהרצה
     */
    public int getRequestedCourses() {
        return requestedCourses;
    }

    /**
     * @return מספר הקורסים ששובצו בפועל
     */
    public int getAssignedCourses() {
        return assignedCourses;
    }

    /**
     * @return מספר השיפורים שאושרו בשלב החיפוש המקומי
     */
    public int getLocalImprovements() {
        return localImprovements;
    }

    /**
     * @return מספר הסטודנטים עם הצלחה מלאה
     */
    public int getFullAssignments() {
        return fullAssignments;
    }

    /**
     * @return מספר הסטודנטים עם שיבוץ חלקי
     */
    public int getPartialAssignments() {
        return partialAssignments;
    }

    /**
     * @return מספר הסטודנטים שלא שובצו כלל
     */
    public int getUnassignedStudents() {
        return unassignedStudents;
    }

    /**
     * @return שורות הלוג שנאספו במהלך ההרצה
     */
    public List<String> getLogLines() {
        return logLines;
    }
}

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
     * מחזיר את שנת הלימודים של ההרצה.
     *
     * @return שנת הלימודים של ההרצה
     */
    public String getAcademicYear() {
        return academicYear;
    }

    /**
     * מחזיר את הסמסטר של ההרצה.
     *
     * @return הסמסטר של ההרצה
     */
    public String getSemester() {
        return semester;
    }

    /**
     * מחזיר את מספר הסטודנטים שטופלו בהרצה.
     *
     * @return מספר הסטודנטים שטופלו בהרצה
     */
    public int getStudentsProcessed() {
        return studentsProcessed;
    }

    /**
     * מחזיר את מספר הבקשות הכולל שנבדק בהרצה.
     *
     * @return מספר הבקשות הכולל שנבדק בהרצה
     */
    public int getRequestedCourses() {
        return requestedCourses;
    }

    /**
     * מחזיר את מספר הקורסים ששובצו בפועל.
     *
     * @return מספר הקורסים ששובצו בפועל
     */
    public int getAssignedCourses() {
        return assignedCourses;
    }

    /**
     * מחזיר את מספר השיפורים שאושרו בשלב המקומי.
     *
     * @return מספר השיפורים שאושרו בשלב החיפוש המקומי
     */
    public int getLocalImprovements() {
        return localImprovements;
    }

    /**
     * מחזיר את מספר הסטודנטים עם הצלחה מלאה.
     *
     * @return מספר הסטודנטים עם הצלחה מלאה
     */
    public int getFullAssignments() {
        return fullAssignments;
    }

    /**
     * מחזיר את מספר הסטודנטים עם שיבוץ חלקי.
     *
     * @return מספר הסטודנטים עם שיבוץ חלקי
     */
    public int getPartialAssignments() {
        return partialAssignments;
    }

    /**
     * מחזיר את מספר הסטודנטים שלא שובצו כלל.
     *
     * @return מספר הסטודנטים שלא שובצו כלל
     */
    public int getUnassignedStudents() {
        return unassignedStudents;
    }

    /**
     * מחזיר את שורות הלוג שנאספו במהלך ההרצה.
     *
     * @return שורות הלוג שנאספו במהלך ההרצה
     */
    public List<String> getLogLines() {
        return logLines;
    }
}

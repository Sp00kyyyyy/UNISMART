package com.example.java_main_proj.repository;

import com.example.java_main_proj.db.DatabaseConnection;
import com.example.java_main_proj.model.Course;
import com.example.java_main_proj.model.CoursePreference;
import com.example.java_main_proj.model.CourseRequirement;
import com.example.java_main_proj.model.EnrollmentResult;
import com.example.java_main_proj.model.Student;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * מייצר את תצוגת התוצאות למסך הדוחות.
 */
final class EnrollmentResultsRepository {
    private static final String FULL_STATUS = "\u05D4\u05E6\u05DC\u05D7\u05D4 \u05DE\u05DC\u05D0\u05D4";
    private static final String PARTIAL_STATUS = "\u05E9\u05D9\u05D1\u05D5\u05E5 \u05D7\u05DC\u05E7\u05D9";
    private static final String EMPTY_STATUS = "\u05DC\u05DC\u05D0 \u05E9\u05D9\u05D1\u05D5\u05E5";
    private static final String YEAR_SUFFIX = "\u05E9\u05E0\u05D4";

    private final DatabaseCatalogReader catalogReader;

    /**
     * יוצר Repository להצגת תוצאות שמורות.
     *
     * @param catalogReader קורא הקטלוג המשמש לקריאת נתוני בסיס
     */
    EnrollmentResultsRepository(DatabaseCatalogReader catalogReader) {
        this.catalogReader = catalogReader;
    }

    /**
     * בונה שורת תוצאה לכל סטודנט שניתן היה לשבץ עבורו קורסים באותו סמסטר.
     *
     * @param academicYear שנת הלימודים של ההרצה
     * @param semester הסמסטר של ההרצה
     * @param requirements חוקי החובה של המערכת
     * @return רשימת תוצאות מוכנות להצגה
     */
    List<EnrollmentResult> loadEnrollmentResults(String academicYear, String semester, List<CourseRequirement> requirements) {
        // טוען קודם את הסטודנטים כדי לבנות תוצאה לכל מי שהיה רלוונטי בהרצה.
        Map<Integer, Student> studentsById = new LinkedHashMap<>();
        for (Student student : catalogReader.loadStudents()) {
            studentsById.put(student.getStudentID(), student);
        }

        // requestedCounts מחשב כמה קורסים כל סטודנט ביקש בפועל, כולל חובה.
        Map<Integer, Integer> requestedCounts = buildRequestedCountsByStudent(studentsById.values(), semester, requirements);
        // assignedCourseNames מושך את הקורסים שאליהם הוא שובץ בפועל בהרצה השמורה.
        Map<Integer, List<String>> assignedCourseNames = loadAssignedCourseNamesByStudent(academicYear, semester);
        List<EnrollmentResult> results = new ArrayList<>();

        for (Student student : studentsById.values()) {
            int requested = requestedCounts.getOrDefault(student.getStudentID(), 0);
            if (requested > 0) {
                List<String> assigned = assignedCourseNames.getOrDefault(student.getStudentID(), List.of());
                int enrolled = assigned.size();
                // הסטטוס נקבע לפי היחס בין מה שביקש לבין מה שקיבל.
                String status = enrolled == requested ? FULL_STATUS : enrolled > 0 ? PARTIAL_STATUS : EMPTY_STATUS;

                results.add(new EnrollmentResult(
                        student.getIdNumber(),
                        student.getFullName(),
                        student.getYear() + " " + YEAR_SUFFIX,
                        requested,
                        enrolled,
                        status,
                        String.join(", ", assigned)
                ));
            }
        }

        // ממיינים כך שמי שקיבל יותר קורסים יופיע קודם בדוח.
        results.sort(Comparator
                .comparingInt(EnrollmentResult::getEnrolledCourses).reversed()
                .thenComparing(EnrollmentResult::getStudentName));
        return results;
    }

    /**
     * מחשב כמה קורסים כל סטודנט ביקש בפועל, כולל קורסי חובה רלוונטיים.
     *
     * @param students רשימת הסטודנטים
     * @param semester הסמסטר הנבדק
     * @param requirements חוקי החובה של המערכת
     * @return מפת מספר בקשות לפי מזהה סטודנט
     */
    private Map<Integer, Integer> buildRequestedCountsByStudent(
            Iterable<Student> students,
            String semester,
            List<CourseRequirement> requirements
    ) {
        // נטען פעם אחת את רשימת הקורסים המוצעים כדי לספור רק בקשות רלוונטיות לסמסטר.
        List<Course> offeredCourses = catalogReader.loadCourses(semester);
        Set<Integer> offeredCourseIds = offeredCourses.stream()
                .map(Course::getCourseID)
                .collect(java.util.stream.Collectors.toSet());

        Map<String, Set<Integer>> mandatoryCoursesByTrackAndYear = new HashMap<>();
        for (CourseRequirement requirement : requirements) {
            if (requirement.isMandatory() && offeredCourseIds.contains(requirement.getCourseId())) {
                // המפתח Track|Year מאפשר גישה מהירה לקורסי החובה של כל קבוצה.
                mandatoryCoursesByTrackAndYear
                        .computeIfAbsent(requirement.getTrack() + "|" + requirement.getYear(), ignored -> new LinkedHashSet<>())
                        .add(requirement.getCourseId());
            }
        }

        Map<Integer, Integer> requestedCounts = new HashMap<>();
        for (Student student : students) {
            // LinkedHashSet מונע ספירה כפולה של אותו קורס אם הגיע גם כהעדפה וגם כחובה.
            Set<Integer> requestedCourseIds = new LinkedHashSet<>();
            for (CoursePreference preference : student.getPreferences()) {
                if (offeredCourseIds.contains(preference.getCourseId())) {
                    requestedCourseIds.add(preference.getCourseId());
                }
            }
            requestedCourseIds.addAll(mandatoryCoursesByTrackAndYear.getOrDefault(
                    student.getTrack() + "|" + student.getYear(),
                    Set.of()
            ));
            // גודל הקבוצה הוא מספר הבקשות הייחודיות של הסטודנט.
            requestedCounts.put(student.getStudentID(), requestedCourseIds.size());
        }

        return requestedCounts;
    }

    /**
     * טוען מהמסד את שמות הקורסים שאליהם שובץ כל סטודנט.
     *
     * @param academicYear שנת הלימודים של ההרצה
     * @param semester הסמסטר של ההרצה
     * @return מפת שמות קורסים משובצים לפי סטודנט
     */
    private Map<Integer, List<String>> loadAssignedCourseNamesByStudent(String academicYear, String semester) {
        Connection connection = DatabaseConnection.getConnection();
        Map<Integer, List<String>> assignedCourseNames = new HashMap<>();

        // השאילתה מחברת בין Enrollment ל-Courses כדי להחזיר שמות קריאים ולא רק מזהים.
        String sql = "SELECT e.StudentID, c.CourseName " +
                "FROM Enrollment e " +
                "INNER JOIN Courses c ON c.CourseID = e.CourseID " +
                "WHERE e.AcademicYear = ? AND e.Semester = ? " +
                "ORDER BY e.StudentID, e.RequestedRank";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, academicYear);
            statement.setString(2, semester);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    // שומר לכל סטודנט את רשימת שמות הקורסים לפי סדר הדירוג שנשמר בהרצה.
                    assignedCourseNames.computeIfAbsent(resultSet.getInt("StudentID"), ignored -> new ArrayList<>())
                            .add(resultSet.getString("CourseName"));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load assigned courses", exception);
        }

        return assignedCourseNames;
    }
}

package com.example.java_main_proj;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuidewayIntegrationTest {
    private static final String ACADEMIC_YEAR = "2025-2026";
    private static final String SEMESTER_A = "\u05E1\u05DE\u05E1\u05D8\u05E8 \u05D0'";
    private static final String SEMESTER_B = "\u05E1\u05DE\u05E1\u05D8\u05E8 \u05D1'";
    private static final String FULL_STATUS = "\u05D4\u05E6\u05DC\u05D7\u05D4 \u05DE\u05DC\u05D0\u05D4";

    @AfterEach
    void tearDown() {
        DatabaseConnection.closeConnection();
        System.clearProperty("unismart.db.path");
    }

    @Test
    void repositoryAndSchedulerWorkAgainstAnIsolatedDatabaseCopy() throws Exception {
        // Arrange
        useIsolatedDatabaseCopy();

        GuidewayRepository repository = new GuidewayRepository();
        HybridEnrollmentService service = new HybridEnrollmentService();

        // Act
        List<Student> students = repository.loadStudents();
        List<Course> courses = repository.loadCourses(null);
        Map<String, ConstraintRule> constraints = repository.loadConstraints();
        List<CourseRequirement> requirements = repository.loadCourseRequirements();
        EnrollmentRunReport report = service.runEnrollment(ACADEMIC_YEAR, SEMESTER_A);
        List<EnrollmentResult> results = repository.loadEnrollmentResults(ACADEMIC_YEAR, SEMESTER_A);
        List<String> semesters = repository.loadSemestersWithResults();

        // Assert
        assertAll(
                () -> assertEquals(11, students.size()),
                () -> assertEquals(10, courses.size()),
                () -> assertEquals(7, constraints.size()),
                () -> assertEquals(20, requirements.size()),
                () -> assertEquals(55, countPreferences()),
                () -> assertEquals(List.of(SEMESTER_A, SEMESTER_B), semesters),
                () -> assertEquals(11, report.getStudentsProcessed()),
                () -> assertEquals(33, report.getRequestedCourses()),
                () -> assertEquals(33, report.getAssignedCourses()),
                () -> assertEquals(11, report.getFullAssignments()),
                () -> assertEquals(0, report.getPartialAssignments()),
                () -> assertEquals(0, report.getUnassignedStudents()),
                () -> assertEquals(11, results.size()),
                () -> assertTrue(results.stream().allMatch(result -> FULL_STATUS.equals(result.getStatus()))),
                () -> assertFalse(tableExists("Prerequisites")),
                () -> assertFalse(tableExists("CompletedCourses")),
                () -> assertEquals(33, countEnrollmentsForRun(ACADEMIC_YEAR, SEMESTER_A)),
                () -> assertEquals(report.getAssignedCourses(), sumCourseEnrollmentCounts(SEMESTER_A))
        );

        assertRunRespectsHardConstraints(ACADEMIC_YEAR, SEMESTER_A, repository);
    }

    @Test
    void semesterARunAndSemesterBRunStaySeparated() throws Exception {
        // Arrange
        useIsolatedDatabaseCopy();

        GuidewayRepository repository = new GuidewayRepository();
        HybridEnrollmentService service = new HybridEnrollmentService();

        // Act
        EnrollmentRunReport semesterAReport = service.runEnrollment(ACADEMIC_YEAR, SEMESTER_A);
        Map<String, String> semesterAResults = loadAssignedCourseLists(ACADEMIC_YEAR, SEMESTER_A);
        int semesterAEnrollments = countEnrollmentsForRun(ACADEMIC_YEAR, SEMESTER_A);

        EnrollmentRunReport semesterBReport = service.runEnrollment(ACADEMIC_YEAR, SEMESTER_B);
        Map<String, String> semesterBResults = loadAssignedCourseLists(ACADEMIC_YEAR, SEMESTER_B);

        // Assert
        assertAll(
                () -> assertEquals(semesterAReport.getAssignedCourses(), semesterAEnrollments),
                () -> assertEquals(semesterAEnrollments, countEnrollmentsForRun(ACADEMIC_YEAR, SEMESTER_A)),
                () -> assertEquals(semesterBReport.getAssignedCourses(), countEnrollmentsForRun(ACADEMIC_YEAR, SEMESTER_B)),
                () -> assertEquals(semesterAReport.getAssignedCourses(), sumCourseEnrollmentCounts(SEMESTER_A)),
                () -> assertEquals(semesterBReport.getAssignedCourses(), sumCourseEnrollmentCounts(SEMESTER_B)),
                () -> assertTrue(semesterAReport.getAssignedCourses() > 0),
                () -> assertTrue(semesterBReport.getAssignedCourses() > 0),
                () -> assertEquals(List.of(SEMESTER_A, SEMESTER_B), repository.loadSemestersWithResults()),
                () -> assertNotEquals(semesterAResults, semesterBResults)
        );

        assertRunRespectsHardConstraints(ACADEMIC_YEAR, SEMESTER_A, repository);
        assertRunRespectsHardConstraints(ACADEMIC_YEAR, SEMESTER_B, repository);
    }

    private void useIsolatedDatabaseCopy() throws IOException {
        Path sourceDatabase = Path.of("src", "main", "resources", "UniSmartDB1.accdb").toAbsolutePath();
        Path tempDir = Files.createTempDirectory("unismart-db-test");
        Path isolatedDatabase = tempDir.resolve("UniSmartDB1.accdb");
        Files.copy(sourceDatabase, isolatedDatabase, StandardCopyOption.REPLACE_EXISTING);
        DatabaseConnection.closeConnection();
        System.setProperty("unismart.db.path", isolatedDatabase.toString());
    }

    private void assertRunRespectsHardConstraints(String academicYear, String semester, GuidewayRepository repository) throws Exception {
        Map<Integer, Student> studentsById = new LinkedHashMap<>();
        for (Student student : repository.loadStudents()) {
            studentsById.put(student.getStudentID(), student);
        }

        Map<Integer, Course> coursesById = new LinkedHashMap<>();
        for (Course course : repository.loadCourses(semester)) {
            coursesById.put(course.getCourseID(), course);
        }

        Map<Integer, Integer> assignmentsPerCourse = new LinkedHashMap<>();
        Map<Integer, Integer> mandatoryAssignmentsPerStudent = new LinkedHashMap<>();
        Map<Integer, List<Course>> assignmentsPerStudent = new LinkedHashMap<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT e.StudentID, e.CourseID, e.IsMandatory, c.Semester " +
                             "FROM Enrollment e " +
                             "INNER JOIN Courses c ON c.CourseID = e.CourseID " +
                             "WHERE e.AcademicYear = ? AND e.Semester = ?")) {
            statement.setString(1, academicYear);
            statement.setString(2, semester);

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    int studentId = resultSet.getInt("StudentID");
                    int courseId = resultSet.getInt("CourseID");
                    boolean mandatory = resultSet.getBoolean("IsMandatory");
                    Course course = coursesById.get(courseId);

                    assertEquals(semester, resultSet.getString("Semester"));
                    assertTrue(course != null, "Assigned course must belong to the selected semester");

                    assignmentsPerCourse.merge(courseId, 1, Integer::sum);
                    if (mandatory) {
                        mandatoryAssignmentsPerStudent.merge(studentId, 1, Integer::sum);
                    }
                    assignmentsPerStudent.computeIfAbsent(studentId, ignored -> new ArrayList<>()).add(course);
                }
            }
        }

        for (Map.Entry<Integer, Integer> entry : assignmentsPerCourse.entrySet()) {
            Course course = coursesById.get(entry.getKey());
            assertTrue(entry.getValue() <= course.getCapacity(), "Course capacity exceeded for course " + entry.getKey());
        }

        for (Map.Entry<Integer, Integer> entry : mandatoryAssignmentsPerStudent.entrySet()) {
            Student student = studentsById.get(entry.getKey());
            assertTrue(entry.getValue() <= student.getMaxMandatoryCourses(),
                    "Mandatory limit exceeded for student " + entry.getKey());
        }

        for (Map.Entry<Integer, List<Course>> entry : assignmentsPerStudent.entrySet()) {
            List<Course> assignedCourses = entry.getValue();
            for (int i = 0; i < assignedCourses.size(); i++) {
                for (int j = i + 1; j < assignedCourses.size(); j++) {
                    assertFalse(assignedCourses.get(i).overlapsWith(assignedCourses.get(j)),
                            "Time conflict detected for student " + entry.getKey());
                }
            }
        }
    }

    private Map<String, String> loadAssignedCourseLists(String academicYear, String semester) throws Exception {
        Map<String, String> courseListsByStudent = new LinkedHashMap<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT s.ID_Number, c.CourseName " +
                             "FROM Enrollment e " +
                             "INNER JOIN Students s ON s.StudentID = e.StudentID " +
                             "INNER JOIN Courses c ON c.CourseID = e.CourseID " +
                             "WHERE e.AcademicYear = ? AND e.Semester = ? " +
                             "ORDER BY s.ID_Number, e.RequestedRank")) {
            statement.setString(1, academicYear);
            statement.setString(2, semester);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String studentId = resultSet.getString("ID_Number");
                    courseListsByStudent.merge(
                            studentId,
                            resultSet.getString("CourseName"),
                            (left, right) -> left + ", " + right
                    );
                }
            }
        }
        return courseListsByStudent;
    }

    private boolean tableExists(String tableName) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME=?")) {
            statement.setString(1, tableName.toUpperCase());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    private int countPreferences() throws Exception {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM StudentCoursePreferences")) {
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private int countEnrollmentsForRun(String academicYear, String semester) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM Enrollment WHERE AcademicYear = ? AND Semester = ?")) {
            statement.setString(1, academicYear);
            statement.setString(2, semester);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private int sumCourseEnrollmentCounts(String semester) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COALESCE(SUM(EnrolledStudents), 0) FROM Courses WHERE Semester = ?")) {
            statement.setString(1, semester);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }
}

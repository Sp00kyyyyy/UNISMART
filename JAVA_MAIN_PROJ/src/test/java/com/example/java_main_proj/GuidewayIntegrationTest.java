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
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuidewayIntegrationTest {
    private static final String ACADEMIC_YEAR = "2025-2026";
    private static final String SEMESTER_A = "\u05E1\u05DE\u05E1\u05D8\u05E8 \u05D0'";
    private static final String SEMESTER_SUMMER = "\u05E1\u05DE\u05E1\u05D8\u05E8 \u05E7\u05D9\u05E5";
    private static final String FULL_STATUS = "\u05D4\u05E6\u05DC\u05D7\u05D4 \u05DE\u05DC\u05D0\u05D4";
    private static final String PARTIAL_STATUS = "\u05E9\u05D9\u05D1\u05D5\u05E5 \u05D7\u05DC\u05E7\u05D9";
    private static final String UNASSIGNED_STATUS = "\u05DC\u05DC\u05D0 \u05E9\u05D9\u05D1\u05D5\u05E5";

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
        Map<String, ConstraintRule> constraints = repository.loadConstraints();
        List<CourseRequirement> requirements = repository.loadCourseRequirements();
        EnrollmentRunReport report = service.runEnrollment("2025-2026", SEMESTER_A);
        List<EnrollmentResult> results = repository.loadEnrollmentResults("2025-2026", SEMESTER_A);
        List<String> semesters = repository.loadSemestersWithResults();

        // Assert
        assertAll(
                () -> assertEquals(11, students.size()),
                () -> assertEquals(7, constraints.size()),
                () -> assertEquals(22, requirements.size()),
                () -> assertEquals(11, report.getStudentsProcessed()),
                () -> assertEquals(33, report.getRequestedCourses()),
                () -> assertEquals(33, report.getAssignedCourses()),
                () -> assertEquals(11, report.getFullAssignments()),
                () -> assertEquals(0, report.getPartialAssignments()),
                () -> assertEquals(0, report.getUnassignedStudents()),
                () -> assertEquals(11, results.size()),
                () -> assertTrue(results.stream().allMatch(result -> FULL_STATUS.equals(result.getStatus()))),
                () -> assertTrue(semesters.contains(SEMESTER_A)),
                () -> assertFalse(tableExists("Prerequisites")),
                () -> assertFalse(tableExists("CompletedCourses")),
                () -> assertEquals(33, countEnrollmentsForRun(ACADEMIC_YEAR, SEMESTER_A))
        );
    }

    @Test
    void summerEdgeCaseScenarioRespectsCapacityMandatoryLimitsAndConflicts() throws Exception {
        // Arrange
        useIsolatedDatabaseCopy();

        GuidewayRepository repository = new GuidewayRepository();
        HybridEnrollmentService service = new HybridEnrollmentService();

        // Act
        List<Course> summerCourses = repository.loadCourses(SEMESTER_SUMMER);
        EnrollmentRunReport report = service.runEnrollment(ACADEMIC_YEAR, SEMESTER_SUMMER);
        List<EnrollmentResult> results = repository.loadEnrollmentResults(ACADEMIC_YEAR, SEMESTER_SUMMER);
        List<String> semesters = repository.loadSemestersWithResults();

        // Assert
        assertAll(
                () -> assertEquals(6, summerCourses.size()),
                () -> assertEquals(11, report.getStudentsProcessed()),
                () -> assertEquals(8, report.getRequestedCourses()),
                () -> assertEquals(3, report.getAssignedCourses()),
                () -> assertEquals(0, report.getFullAssignments()),
                () -> assertEquals(3, report.getPartialAssignments()),
                () -> assertEquals(8, report.getUnassignedStudents()),
                () -> assertEquals(11, results.size()),
                () -> assertEquals(3, results.stream().filter(result -> PARTIAL_STATUS.equals(result.getStatus())).count()),
                () -> assertEquals(8, results.stream().filter(result -> UNASSIGNED_STATUS.equals(result.getStatus())).count()),
                () -> assertTrue(semesters.contains(SEMESTER_SUMMER)),
                () -> assertEquals(3, countEnrollmentsForRun(ACADEMIC_YEAR, SEMESTER_SUMMER)),
                () -> assertTrue(isAssigned(11, 12, ACADEMIC_YEAR, SEMESTER_SUMMER)),
                () -> assertFalse(isAssigned(11, 15, ACADEMIC_YEAR, SEMESTER_SUMMER)),
                () -> assertTrue(isAssigned(8, 16, ACADEMIC_YEAR, SEMESTER_SUMMER)),
                () -> assertFalse(isAssigned(8, 12, ACADEMIC_YEAR, SEMESTER_SUMMER)),
                () -> assertTrue(isAssigned(2, 13, ACADEMIC_YEAR, SEMESTER_SUMMER)),
                () -> assertFalse(isAssigned(2, 17, ACADEMIC_YEAR, SEMESTER_SUMMER)),
                () -> assertEquals(0, countAssignmentsForStudent(3, ACADEMIC_YEAR, SEMESTER_SUMMER)),
                () -> assertEquals(0, countAssignmentsForCourse(14, ACADEMIC_YEAR, SEMESTER_SUMMER))
        );
    }

    private void useIsolatedDatabaseCopy() throws IOException {
        Path sourceDatabase = Path.of("src", "main", "resources", "UniSmartDB1.accdb").toAbsolutePath();
        Path tempDir = Files.createTempDirectory("unismart-db-test");
        Path isolatedDatabase = tempDir.resolve("UniSmartDB1.accdb");
        Files.copy(sourceDatabase, isolatedDatabase, StandardCopyOption.REPLACE_EXISTING);
        DatabaseConnection.closeConnection();
        System.setProperty("unismart.db.path", isolatedDatabase.toString());
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

    private boolean isAssigned(int studentId, int courseId, String academicYear, String semester) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM Enrollment WHERE StudentID = ? AND CourseID = ? AND AcademicYear = ? AND Semester = ?")) {
            statement.setInt(1, studentId);
            statement.setInt(2, courseId);
            statement.setString(3, academicYear);
            statement.setString(4, semester);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    private int countAssignmentsForStudent(int studentId, String academicYear, String semester) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM Enrollment WHERE StudentID = ? AND AcademicYear = ? AND Semester = ?")) {
            statement.setInt(1, studentId);
            statement.setString(2, academicYear);
            statement.setString(3, semester);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }

    private int countAssignmentsForCourse(int courseId, String academicYear, String semester) throws Exception {
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM Enrollment WHERE CourseID = ? AND AcademicYear = ? AND Semester = ?")) {
            statement.setInt(1, courseId);
            statement.setString(2, academicYear);
            statement.setString(3, semester);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1);
            }
        }
    }
}

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
    private static final String SEMESTER_A = "\u05E1\u05DE\u05E1\u05D8\u05E8 \u05D0'";
    private static final String FULL_STATUS = "\u05D4\u05E6\u05DC\u05D7\u05D4 \u05DE\u05DC\u05D0\u05D4";

    @AfterEach
    void tearDown() {
        DatabaseConnection.closeConnection();
        System.clearProperty("unismart.db.path");
    }

    @Test
    void repositoryAndSchedulerWorkAgainstAnIsolatedDatabaseCopy() throws Exception {
        // Arrange
        Path sourceDatabase = Path.of("src", "main", "resources", "UniSmartDB1.accdb").toAbsolutePath();
        Path tempDir = Files.createTempDirectory("unismart-db-test");
        Path isolatedDatabase = tempDir.resolve("UniSmartDB1.accdb");
        Files.copy(sourceDatabase, isolatedDatabase, StandardCopyOption.REPLACE_EXISTING);
        DatabaseConnection.closeConnection();
        System.setProperty("unismart.db.path", isolatedDatabase.toString());

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
                () -> assertEquals(20, requirements.size()),
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
                () -> assertEquals(33, countEnrollmentsForRun("2025-2026", SEMESTER_A))
        );
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
}

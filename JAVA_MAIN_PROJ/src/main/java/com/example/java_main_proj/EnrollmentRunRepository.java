package com.example.java_main_proj;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

final class EnrollmentRunRepository {
    private static final String SEMESTER_A = "\u05E1\u05DE\u05E1\u05D8\u05E8 \u05D0'";
    private static final String SEMESTER_B = "\u05E1\u05DE\u05E1\u05D8\u05E8 \u05D1'";

    void replaceEnrollmentRun(String academicYear, String semester, List<EnrollmentDecision> decisions) {
        Connection connection = DatabaseConnection.getConnection();

        clearEnrollmentRun(connection, academicYear, semester);
        if (decisions.isEmpty()) {
            synchronizeCourseEnrollmentCounts(connection, academicYear, semester);
            return;
        }

        int nextEnrollmentId = nextIdentifier(connection, "Enrollment", "EnrollmentID");
        saveEnrollmentDecisions(connection, decisions, nextEnrollmentId);
        synchronizeCourseEnrollmentCounts(connection, academicYear, semester);
    }

    List<String> loadAcademicYearsWithResults() {
        Connection connection = DatabaseConnection.getConnection();
        Set<String> values = new TreeSet<>(java.util.Comparator.reverseOrder());

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT DISTINCT AcademicYear FROM Enrollment WHERE AcademicYear IS NOT NULL")) {
            while (resultSet.next()) {
                String academicYear = resultSet.getString("AcademicYear");
                if (academicYear != null && !academicYear.isBlank()) {
                    values.add(academicYear);
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load academic years", exception);
        }

        if (values.isEmpty()) {
            values.add("2025-2026");
        }

        return List.copyOf(values);
    }

    List<String> loadSemestersWithResults() {
        return List.of(SEMESTER_A, SEMESTER_B);
    }

    private void clearEnrollmentRun(Connection connection, String academicYear, String semester) {
        try (PreparedStatement deleteStatement = connection.prepareStatement(
                "DELETE FROM Enrollment WHERE AcademicYear = ? AND Semester = ?")) {
            deleteStatement.setString(1, academicYear);
            deleteStatement.setString(2, semester);
            deleteStatement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to clear previous enrollment results", exception);
        }
    }

    private void saveEnrollmentDecisions(Connection connection, List<EnrollmentDecision> decisions, int nextEnrollmentId) {
        try (PreparedStatement insertStatement = connection.prepareStatement(
                "INSERT INTO Enrollment " +
                        "(EnrollmentID, StudentID, CourseID, EnrollmentDate, Status, AcademicYear, Semester, AssignmentScore, RequestedRank, IsMandatory) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            for (EnrollmentDecision decision : decisions) {
                insertStatement.setInt(1, nextEnrollmentId++);
                insertStatement.setInt(2, decision.getStudentId());
                insertStatement.setInt(3, decision.getCourseId());
                insertStatement.setTimestamp(4, java.sql.Timestamp.valueOf(LocalDateTime.now()));
                insertStatement.setString(5, "ASSIGNED");
                insertStatement.setString(6, decision.getAcademicYear());
                insertStatement.setString(7, decision.getSemester());
                insertStatement.setDouble(8, decision.getAssignmentScore());
                insertStatement.setInt(9, decision.getRequestedRank());
                insertStatement.setBoolean(10, decision.isMandatory());
                insertStatement.addBatch();
            }
            insertStatement.executeBatch();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to save enrollment decisions", exception);
        }
    }

    private int nextIdentifier(Connection connection, String tableName, String columnName) {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT MAX(" + columnName + ") FROM " + tableName)) {
            if (resultSet.next()) {
                return resultSet.getInt(1) + 1;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to compute next identifier for " + tableName, exception);
        }
        return 1;
    }

    private void synchronizeCourseEnrollmentCounts(Connection connection, String academicYear, String semester) {
        Map<Integer, Integer> enrollmentCountsByCourse = new HashMap<>();

        try (PreparedStatement countStatement = connection.prepareStatement(
                "SELECT CourseID, COUNT(*) AS EnrolledCount " +
                        "FROM Enrollment " +
                        "WHERE AcademicYear = ? AND Semester = ? " +
                        "GROUP BY CourseID")) {
            countStatement.setString(1, academicYear);
            countStatement.setString(2, semester);
            try (ResultSet resultSet = countStatement.executeQuery()) {
                while (resultSet.next()) {
                    enrollmentCountsByCourse.put(resultSet.getInt("CourseID"), resultSet.getInt("EnrolledCount"));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to calculate course enrollment counts", exception);
        }

        try (PreparedStatement resetStatement = connection.prepareStatement(
                "UPDATE Courses SET EnrolledStudents = 0 WHERE Semester = ?");
             PreparedStatement updateStatement = connection.prepareStatement(
                     "UPDATE Courses SET EnrolledStudents = ? WHERE CourseID = ?")) {
            resetStatement.setString(1, semester);
            resetStatement.executeUpdate();

            for (Map.Entry<Integer, Integer> entry : enrollmentCountsByCourse.entrySet()) {
                updateStatement.setInt(1, entry.getValue());
                updateStatement.setInt(2, entry.getKey());
                updateStatement.addBatch();
            }
            if (!enrollmentCountsByCourse.isEmpty()) {
                updateStatement.executeBatch();
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to synchronize course enrollment counts", exception);
        }
    }
}

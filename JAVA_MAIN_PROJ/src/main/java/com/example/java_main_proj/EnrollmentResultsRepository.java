package com.example.java_main_proj;

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

final class EnrollmentResultsRepository {
    private static final String FULL_STATUS = "\u05D4\u05E6\u05DC\u05D7\u05D4 \u05DE\u05DC\u05D0\u05D4";
    private static final String PARTIAL_STATUS = "\u05E9\u05D9\u05D1\u05D5\u05E5 \u05D7\u05DC\u05E7\u05D9";
    private static final String EMPTY_STATUS = "\u05DC\u05DC\u05D0 \u05E9\u05D9\u05D1\u05D5\u05E5";
    private static final String YEAR_SUFFIX = "\u05E9\u05E0\u05D4";

    private final DatabaseCatalogReader catalogReader;

    EnrollmentResultsRepository(DatabaseCatalogReader catalogReader) {
        this.catalogReader = catalogReader;
    }

    List<EnrollmentResult> loadEnrollmentResults(String academicYear, String semester, List<CourseRequirement> requirements) {
        Map<Integer, Student> studentsById = new LinkedHashMap<>();
        for (Student student : catalogReader.loadStudents()) {
            studentsById.put(student.getStudentID(), student);
        }

        Map<Integer, Integer> requestedCounts = buildRequestedCountsByStudent(studentsById.values(), semester, requirements);
        Map<Integer, List<String>> assignedCourseNames = loadAssignedCourseNamesByStudent(academicYear, semester);
        List<EnrollmentResult> results = new ArrayList<>();

        for (Student student : studentsById.values()) {
            int requested = requestedCounts.getOrDefault(student.getStudentID(), 0);
            if (requested == 0) {
                continue;
            }

            List<String> assigned = assignedCourseNames.getOrDefault(student.getStudentID(), List.of());
            int enrolled = assigned.size();
            String status = requested > 0 && enrolled == requested
                    ? FULL_STATUS
                    : enrolled > 0 ? PARTIAL_STATUS : EMPTY_STATUS;

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

        results.sort(Comparator
                .comparingInt(EnrollmentResult::getEnrolledCourses).reversed()
                .thenComparing(EnrollmentResult::getStudentName));
        return results;
    }

    private Map<Integer, Integer> buildRequestedCountsByStudent(
            Iterable<Student> students,
            String semester,
            List<CourseRequirement> requirements
    ) {
        List<Course> offeredCourses = catalogReader.loadCourses(semester);
        Set<Integer> offeredCourseIds = offeredCourses.stream()
                .map(Course::getCourseID)
                .collect(java.util.stream.Collectors.toSet());

        Map<String, Set<Integer>> mandatoryCoursesByTrackAndYear = new HashMap<>();
        for (CourseRequirement requirement : requirements) {
            if (!requirement.isMandatory() || !offeredCourseIds.contains(requirement.getCourseId())) {
                continue;
            }
            mandatoryCoursesByTrackAndYear
                    .computeIfAbsent(requirement.getTrack() + "|" + requirement.getYear(), ignored -> new LinkedHashSet<>())
                    .add(requirement.getCourseId());
        }

        Map<Integer, Integer> requestedCounts = new HashMap<>();
        for (Student student : students) {
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
            requestedCounts.put(student.getStudentID(), requestedCourseIds.size());
        }

        return requestedCounts;
    }

    private Map<Integer, List<String>> loadAssignedCourseNamesByStudent(String academicYear, String semester) {
        Connection connection = DatabaseConnection.getConnection();
        Map<Integer, List<String>> assignedCourseNames = new HashMap<>();

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

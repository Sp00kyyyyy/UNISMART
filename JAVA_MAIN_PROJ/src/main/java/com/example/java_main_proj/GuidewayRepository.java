package com.example.java_main_proj;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class GuidewayRepository {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public List<Student> loadStudents() {
        Connection connection = DatabaseConnection.getConnection();
        List<Student> students = new ArrayList<>();

        String sql = "SELECT StudentID, FullName, ID_Number, [Year], Track, PriorityLevel, Seniority, GPA, " +
                "TimePreference, PreferredDays, MaxMandatoryCourses FROM Students";

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                Student student = new Student();
                student.setStudentID(resultSet.getInt("StudentID"));
                student.setFullName(resultSet.getString("FullName"));
                student.setIdNumber(resultSet.getString("ID_Number"));
                student.setYear(resultSet.getInt("Year"));
                student.setTrack(resultSet.getString("Track"));
                student.setPriorityLevel(resultSet.getInt("PriorityLevel"));
                student.setSeniority(resultSet.getInt("Seniority"));
                student.setGpa(resultSet.getDouble("GPA"));
                student.setTimePreference(resultSet.getString("TimePreference"));
                student.setPreferredDays(resultSet.getString("PreferredDays"));
                student.setMaxMandatoryCourses(resultSet.getInt("MaxMandatoryCourses"));
                students.add(student);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load students", exception);
        }

        Map<Integer, List<CoursePreference>> preferencesByStudent = loadPreferencesByStudent();
        for (Student student : students) {
            student.setPreferences(preferencesByStudent.getOrDefault(student.getStudentID(), List.of()));
        }

        students.sort(Comparator
                .comparingInt(Student::getPriorityLevel)
                .thenComparing(Comparator.comparingInt(Student::getSeniority).reversed())
                .thenComparing(Comparator.comparingDouble(Student::getGpa).reversed())
                .thenComparing(Student::getStudentID));

        return students;
    }

    public List<Course> loadCourses(String semester) {
        Connection connection = DatabaseConnection.getConnection();
        List<Course> courses = new ArrayList<>();

        String sql = "SELECT CourseID, CourseName, CourseType, Lecturer, Day, StartTime, EndTime, " +
                "Capacity, EnrolledStudents, Semester FROM Courses";
        if (semester != null && !semester.isBlank()) {
            sql += " WHERE Semester = ?";
        }

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (semester != null && !semester.isBlank()) {
                statement.setString(1, semester);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    courses.add(new Course(
                            resultSet.getInt("CourseID"),
                            resultSet.getString("CourseName"),
                            resultSet.getString("CourseType"),
                            resultSet.getString("Lecturer"),
                            resultSet.getString("Day"),
                            formatTime(resultSet.getTime("StartTime")),
                            formatTime(resultSet.getTime("EndTime")),
                            resultSet.getInt("Capacity"),
                            resultSet.getInt("EnrolledStudents"),
                            resultSet.getString("Semester")
                    ));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load courses", exception);
        }

        courses.sort(Comparator.comparing(Course::getCourseID));
        return courses;
    }

    public List<CourseRequirement> loadCourseRequirements() {
        Connection connection = DatabaseConnection.getConnection();
        List<CourseRequirement> requirements = new ArrayList<>();

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT CourseID, Track, [Year], IsMandatory FROM CourseTrackRules")) {
            while (resultSet.next()) {
                requirements.add(new CourseRequirement(
                        resultSet.getInt("CourseID"),
                        resultSet.getString("Track"),
                        resultSet.getInt("Year"),
                        resultSet.getBoolean("IsMandatory")
                ));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load course-track rules", exception);
        }

        return requirements;
    }

    public Map<String, ConstraintRule> loadConstraints() {
        Connection connection = DatabaseConnection.getConnection();
        Map<String, ConstraintRule> constraints = new LinkedHashMap<>();

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT ConstraintName, Description, ConstraintType, Weight FROM [Constraints]")) {
            while (resultSet.next()) {
                ConstraintRule rule = new ConstraintRule(
                        resultSet.getString("ConstraintName"),
                        resultSet.getString("Description"),
                        resultSet.getString("ConstraintType"),
                        resultSet.getInt("Weight")
                );
                constraints.put(rule.getName(), rule);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load constraints", exception);
        }

        return constraints;
    }

    public void replaceEnrollmentRun(String academicYear, String semester, List<EnrollmentDecision> decisions) {
        Connection connection = DatabaseConnection.getConnection();

        try (PreparedStatement deleteStatement = connection.prepareStatement(
                "DELETE FROM Enrollment WHERE AcademicYear = ? AND Semester = ?")) {
            deleteStatement.setString(1, academicYear);
            deleteStatement.setString(2, semester);
            deleteStatement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to clear previous enrollment results", exception);
        }

        int nextEnrollmentId = nextIdentifier(connection, "Enrollment", "EnrollmentID");

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

    public List<EnrollmentResult> loadEnrollmentResults(String academicYear, String semester) {
        Map<Integer, Student> studentsById = new LinkedHashMap<>();
        for (Student student : loadStudents()) {
            studentsById.put(student.getStudentID(), student);
        }

        Map<Integer, Integer> requestedCounts = loadRequestedCountsByStudent(semester);
        Map<Integer, List<String>> assignedCourseNames = loadAssignedCourseNamesByStudent(academicYear, semester);
        List<EnrollmentResult> results = new ArrayList<>();

        for (Student student : studentsById.values()) {
            int requested = requestedCounts.getOrDefault(student.getStudentID(), 0);
            List<String> assigned = assignedCourseNames.getOrDefault(student.getStudentID(), List.of());
            int enrolled = assigned.size();
            String status;
            if (requested > 0 && enrolled == requested) {
                status = "האלמ החלצה";
            } else if (enrolled > 0) {
                status = "יקלח ץוביש";
            } else {
                status = "ץוביש אלל";
            }

            results.add(new EnrollmentResult(
                    student.getIdNumber(),
                    student.getFullName(),
                    student.getYear() + " הנש",
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

    public List<String> loadAcademicYearsWithResults() {
        Connection connection = DatabaseConnection.getConnection();
        Set<String> values = new TreeSet<>(Comparator.reverseOrder());

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

        return new ArrayList<>(values);
    }

    public List<String> loadSemestersWithResults() {
        Connection connection = DatabaseConnection.getConnection();
        Set<String> values = new TreeSet<>(
                Comparator.comparingInt(this::semesterRank).thenComparing(value -> value == null ? "" : value)
        );

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT DISTINCT Semester FROM Enrollment WHERE Semester IS NOT NULL")) {
            while (resultSet.next()) {
                String semester = resultSet.getString("Semester");
                if (semester != null && !semester.isBlank()) {
                    values.add(semester);
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load semesters", exception);
        }

        if (values.isEmpty()) {
            values.add("'א רטסמס");
            values.add("'ב רטסמס");
        }

        return new ArrayList<>(values);
    }

    private Map<Integer, Integer> loadRequestedCountsByStudent(String semester) {
        Connection connection = DatabaseConnection.getConnection();
        Map<Integer, Integer> requestedCounts = new HashMap<>();

        String sql = "SELECT p.StudentID, COUNT(*) AS RequestedCount " +
                "FROM StudentCoursePreferences p " +
                "INNER JOIN Courses c ON c.CourseID = p.CourseID " +
                "WHERE c.Semester = ? " +
                "GROUP BY p.StudentID";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, semester);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    requestedCounts.put(resultSet.getInt("StudentID"), resultSet.getInt("RequestedCount"));
                }
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load request counts", exception);
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

    private Map<Integer, List<CoursePreference>> loadPreferencesByStudent() {
        Connection connection = DatabaseConnection.getConnection();
        Map<Integer, List<CoursePreference>> preferencesByStudent = new HashMap<>();

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT StudentID, CourseID, PreferenceRank FROM StudentCoursePreferences ORDER BY StudentID, PreferenceRank")) {
            while (resultSet.next()) {
                preferencesByStudent.computeIfAbsent(resultSet.getInt("StudentID"), ignored -> new ArrayList<>())
                        .add(new CoursePreference(resultSet.getInt("CourseID"), resultSet.getInt("PreferenceRank")));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load preferences", exception);
        }

        return preferencesByStudent;
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

    private int semesterRank(String semester) {
        return switch (semester) {
            case "'א רטסמס" -> 1;
            case "'ב רטסמס" -> 2;
            case "ץיק רטסמס" -> 3;
            default -> 99;
        };
    }

    private String formatTime(Time time) {
        if (time == null) {
            return "";
        }
        return time.toLocalTime().format(TIME_FORMATTER);
    }
}

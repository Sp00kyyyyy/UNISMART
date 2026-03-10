package com.example.java_main_proj.repository;

import com.example.java_main_proj.db.DatabaseConnection;
import com.example.java_main_proj.model.ConstraintRule;
import com.example.java_main_proj.model.Course;
import com.example.java_main_proj.model.CoursePreference;
import com.example.java_main_proj.model.CourseRequirement;
import com.example.java_main_proj.model.Student;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class DatabaseCatalogReader {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    List<Student> loadStudents() {
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

        Map<Integer, List<CoursePreference>> preferencesByStudent = loadPreferencesByStudent(connection);
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

    List<Course> loadCourses(String semester) {
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

    List<CourseRequirement> loadCourseRequirements() {
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

    Map<String, ConstraintRule> loadConstraints() {
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

    private Map<Integer, List<CoursePreference>> loadPreferencesByStudent(Connection connection) {
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

    private String formatTime(Time time) {
        if (time == null) {
            return "";
        }
        return time.toLocalTime().format(TIME_FORMATTER);
    }
}

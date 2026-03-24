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

/**
 * שכבת קריאה ממסד הנתונים עבור נתוני הקטלוג של המערכת.
 */
final class DatabaseCatalogReader {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * יוצר קורא קטלוג למסד הנתונים.
     */
    DatabaseCatalogReader() {
    }

    /**
     * טוען את כל הסטודנטים ומעשיר כל סטודנט גם בהעדפות הקורסים שלו.
     *
     * @return רשימת הסטודנטים עם העדפות קורסים
     */
    List<Student> loadStudents() {
        Connection connection = DatabaseConnection.getConnection();
        List<Student> students = new ArrayList<>();

        String sql = "SELECT StudentID, FullName, ID_Number, [Year], Track, PriorityLevel, Seniority, GPA, " +
                "TimePreference, PreferredDays, MaxMandatoryCourses FROM Students";

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            while (resultSet.next()) {
                // בונה את אובייקט הסטודנט מתוך שורת מסד אחת.
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
            // לאחר טעינת הנתונים הבסיסיים מצרפים לכל סטודנט את רשימת ההעדפות שלו.
            student.setPreferences(preferencesByStudent.getOrDefault(student.getStudentID(), List.of()));
        }

        // מיון קבוע עוזר לשמור על התנהגות דטרמיניסטית גם לפני שלב התכנון.
        students.sort(Comparator
                .comparingInt(Student::getPriorityLevel)
                .thenComparing(Comparator.comparingInt(Student::getSeniority).reversed())
                .thenComparing(Comparator.comparingDouble(Student::getGpa).reversed())
                .thenComparing(Student::getStudentID));

        return students;
    }

    /**
     * טוען קורסים לפי סמסטר, או את כל הקורסים אם לא נבחר סמסטר.
     *
     * @param semester הסמסטר המבוקש, או {@code null} עבור כל הקורסים
     * @return רשימת הקורסים הרלוונטיים
     */
    List<Course> loadCourses(String semester) {
        Connection connection = DatabaseConnection.getConnection();
        List<Course> courses = new ArrayList<>();

        String sql = "SELECT CourseID, CourseName, CourseType, Lecturer, Day, StartTime, EndTime, " +
                "Capacity, EnrolledStudents, Semester FROM Courses";
        if (semester != null && !semester.isBlank()) {
            // אם הועבר סמסטר, מגבילים את הקריאה רק לקורסים הרלוונטיים.
            sql += " WHERE Semester = ?";
        }

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            if (semester != null && !semester.isBlank()) {
                statement.setString(1, semester);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    // כל שורה בטבלת Courses מתורגמת למודל Course מלא.
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

        // מיון לפי מזהה יוצר פלט עקבי וקל למעקב.
        courses.sort(Comparator.comparing(Course::getCourseID));
        return courses;
    }

    /**
     * טוען את כללי ההתאמה בין קורסים, מסלולים ושנים.
     *
     * @return רשימת חוקי החובה וההתאמה
     */
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

    /**
     * טוען את רשימת האילוצים והמשקלים שהאלגוריתם משתמש בהם.
     *
     * @return מפת אילוצים לפי שם האילוץ
     */
    Map<String, ConstraintRule> loadConstraints() {
        Connection connection = DatabaseConnection.getConnection();
        Map<String, ConstraintRule> constraints = new LinkedHashMap<>();

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT ConstraintName, Description, ConstraintType, Weight FROM [Constraints]")) {
            while (resultSet.next()) {
                // נשמר לפי שם האילוץ כדי לאפשר גישה ישירה לפי מפתח.
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

    /**
     * טוען העדפות קורסים לכל סטודנט ומקבץ אותן לפי מזהה סטודנט.
     *
     * @param connection חיבור פעיל למסד הנתונים
     * @return מפת העדפות קורסים לפי מזהה סטודנט
     */
    private Map<Integer, List<CoursePreference>> loadPreferencesByStudent(Connection connection) {
        Map<Integer, List<CoursePreference>> preferencesByStudent = new HashMap<>();

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT StudentID, CourseID, PreferenceRank FROM StudentCoursePreferences ORDER BY StudentID, PreferenceRank")) {
            while (resultSet.next()) {
                // הקיבוץ לפי StudentID מייצר רשימת העדפות מסודרת לכל סטודנט.
                preferencesByStudent.computeIfAbsent(resultSet.getInt("StudentID"), ignored -> new ArrayList<>())
                        .add(new CoursePreference(resultSet.getInt("CourseID"), resultSet.getInt("PreferenceRank")));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to load preferences", exception);
        }

        return preferencesByStudent;
    }

    /**
     * שומר על פורמט זמן אחיד במעבר מהמסד למודל.
     *
     * @param time ערך זמן מתוך המסד
     * @return מחרוזת זמן בפורמט {@code HH:mm}
     */
    private String formatTime(Time time) {
        if (time == null) {
            return "";
        }
        // מאחד את ייצוגי הזמן למסך ולמודלים בפורמט HH:mm.
        return time.toLocalTime().format(TIME_FORMATTER);
    }
}

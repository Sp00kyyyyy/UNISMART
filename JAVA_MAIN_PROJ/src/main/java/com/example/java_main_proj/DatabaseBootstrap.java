package com.example.java_main_proj;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DatabaseBootstrap {
    private static boolean synchronizedOnce;

    private DatabaseBootstrap() {
    }

    public static synchronized void synchronize(Connection connection) {
        if (synchronizedOnce) {
            return;
        }

        try {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                ensureGuidewaySchema(connection);
                dropObsoleteTables(connection);
                normalizeStudentData(connection);
                normalizeCourseData(connection);
                normalizeEnrollmentSemesters(connection);
                seedConstraints(connection);
                seedCourseTrackRules(connection);
                seedStudentPreferences(connection);
                connection.commit();
                synchronizedOnce = true;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Failed to synchronize guideway database state", exception);
        }
    }

    private static void ensureGuidewaySchema(Connection connection) throws SQLException {
        ensureColumn(connection, "Courses", "Semester", "TEXT(20)");
        ensureColumn(connection, "Enrollment", "AcademicYear", "TEXT(20)");
        ensureColumn(connection, "Enrollment", "Semester", "TEXT(20)");
        ensureColumn(connection, "Enrollment", "AssignmentScore", "DOUBLE");
        ensureColumn(connection, "Enrollment", "RequestedRank", "LONG");
        ensureColumn(connection, "Enrollment", "IsMandatory", "YESNO");

        if (!tableExists(connection, "CourseTrackRules")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(
                        "CREATE TABLE CourseTrackRules (" +
                                "RuleID LONG, " +
                                "CourseID LONG, " +
                                "Track TEXT(100), " +
                                "[Year] LONG, " +
                                "IsMandatory YESNO)"
                );
            }
        }
    }

    private static void dropObsoleteTables(Connection connection) throws SQLException {
        dropTableIfExists(connection, "Prerequisites");
        dropTableIfExists(connection, "CompletedCourses");
    }

    private static void normalizeStudentData(Connection connection) throws SQLException {
        List<StudentDisplaySeed> seeds = List.of(
                new StudentDisplaySeed(1, "ילאפר ןתנוהי", "בשחמה יעדמ", "רקוב", "יעיבר,ישילש,ןושאר"),
                new StudentDisplaySeed(2, "ןהכ הרש", "בשחמה יעדמ", "ברע", "ישימח,יעיבר,ינש"),
                new StudentDisplaySeed(3, "יול דוד", "הנכות תסדנה", "רקוב", "ישילש,ינש,ןושאר"),
                new StudentDisplaySeed(4, "םהרבא לכימ", "עדימ תוכרעמ", "רקוב", "יעיבר,ישילש,ינש"),
                new StudentDisplaySeed(5, "יחרזמ יסוי", "בשחמה יעדמ", "ברע", "ישימח,יעיבר,ןושאר"),
                new StudentDisplaySeed(6, "ילארשי תינור", "הנכות תסדנה", "רקוב", "ישימח,ישילש,ינש"),
                new StudentDisplaySeed(7, "ןועמש יבא", "בשחמה יעדמ", "ברע", "ישימח,ינש,ןושאר"),
                new StudentDisplaySeed(8, "דוד העונ", "עדימ תוכרעמ", "רקוב", "יעיבר,ישילש,ןושאר"),
                new StudentDisplaySeed(9, "רומ ילא", "הנכות תסדנה", "ברע", "ישימח,יעיבר,ינש"),
                new StudentDisplaySeed(10, "יול רמת", "בשחמה יעדמ", "רקוב", "ישימח,ישילש,ןושאר"),
                new StudentDisplaySeed(11, "יטרופנוק קפוא", "בשחמה יעדמ", "ברע", "ישימח,ישילש,ןושאר")
        );

        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE Students SET FullName=?, Track=?, TimePreference=?, PreferredDays=? WHERE StudentID=?")) {
            for (StudentDisplaySeed seed : seeds) {
                statement.setString(1, seed.fullName());
                statement.setString(2, seed.track());
                statement.setString(3, seed.timePreference());
                statement.setString(4, seed.preferredDays());
                statement.setInt(5, seed.studentId());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void normalizeCourseData(Connection connection) throws SQLException {
        List<CourseDisplaySeed> seeds = List.of(
                new CourseDisplaySeed(1, "בשחמה יעדמל אובמ", "הבוח", "ןהכ יכדרמ פורפ", "ןושאר", "'א רטסמס"),
                new CourseDisplaySeed(2, "םימתירוגלאו םינותנ ינבמ", "הבוח", "יול לחר רד", "ינש", "'ב רטסמס"),
                new CourseDisplaySeed(3, "םימצע החנומ תונכת", "הבוח", "דוד םהרבא פורפ", "ישילש", "'א רטסמס"),
                new CourseDisplaySeed(5, "תיתוכאלמ הניב", "הריחב", "ילארשי הרש רד", "יעיבר", "'ב רטסמס"),
                new CourseDisplaySeed(6, "עדימ תוכרעמ", "הבוח", "יחרזמ ףסוי פורפ", "ןושאר", "'א רטסמס"),
                new CourseDisplaySeed(7, "הנכות תסדנה", "הבוח", "םהרבא העונ רד", "ישילש", "'ב רטסמס"),
                new CourseDisplaySeed(8, "הנוכמ תדימל", "הריחב", "ןועמש דוד פורפ", "ישימח", "'ב רטסמס"),
                new CourseDisplaySeed(9, "םינותנ ידסמ", "הבוח", "ןהכ לכימ רד", "ינש", "'א רטסמס"),
                new CourseDisplaySeed(10, "עדימ תחטבא", "הריחב", "קרב השמ פורפ", "יעיבר", "'ב רטסמס"),
                new CourseDisplaySeed(11, "םיבשחמ תותשר", "הבוח", "ץכ לעי רד", "ישימח", "'א רטסמס")
        );

        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE Courses SET CourseName=?, CourseType=?, Lecturer=?, Day=?, Semester=? WHERE CourseID=?")) {
            for (CourseDisplaySeed seed : seeds) {
                statement.setString(1, seed.courseName());
                statement.setString(2, seed.courseType());
                statement.setString(3, seed.lecturer());
                statement.setString(4, seed.day());
                statement.setString(5, seed.semester());
                statement.setInt(6, seed.courseId());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void normalizeEnrollmentSemesters(Connection connection) throws SQLException {
        List<SemesterSeed> seeds = List.of(
                new SemesterSeed("\u05e1\u05de\u05e1\u05d8\u05e8 \u05d0'", "'א רטסמס"),
                new SemesterSeed("\u05e1\u05de\u05e1\u05d8\u05e8 \u05d1'", "'ב רטסמס"),
                new SemesterSeed("\u05e1\u05de\u05e1\u05d8\u05e8 \u05e7\u05d9\u05e5", "ץיק רטסמס")
        );

        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE Enrollment SET Semester=? WHERE Semester=?")) {
            for (SemesterSeed seed : seeds) {
                statement.setString(1, seed.reversedValue());
                statement.setString(2, seed.normalValue());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void seedConstraints(Connection connection) throws SQLException {
        List<ConstraintSeed> seeds = List.of(
                new ConstraintSeed(1, "NO_TIME_CONFLICT", "A student cannot be assigned to overlapping courses.", "HARD", 1000),
                new ConstraintSeed(2, "COURSE_CAPACITY", "Course capacity cannot be exceeded.", "HARD", 1000),
                new ConstraintSeed(3, "MAX_MANDATORY_COURSES", "A student cannot exceed the semester limit for mandatory courses.", "HARD", 1000),
                new ConstraintSeed(4, "MANDATORY_PRIORITY", "When seats are contested, mandatory requests get higher priority.", "POLICY", 75),
                new ConstraintSeed(5, "TIME_PREFERENCE", "Prefer courses that match the student's time preference.", "SOFT", 18),
                new ConstraintSeed(6, "PREFERRED_DAYS", "Prefer courses on the student's preferred days.", "SOFT", 14),
                new ConstraintSeed(7, "COURSE_PREFERENCE_RANK", "Prefer higher-ranked requests first.", "SOFT", 24)
        );

        try (Statement deleteStatement = connection.createStatement()) {
            deleteStatement.executeUpdate("DELETE FROM [Constraints]");
        }

        try (PreparedStatement insertStatement = connection.prepareStatement(
                "INSERT INTO [Constraints] (ConstraintID, ConstraintName, Description, ConstraintType, Weight) VALUES (?, ?, ?, ?, ?)")) {
            for (ConstraintSeed seed : seeds) {
                insertStatement.setInt(1, seed.id());
                insertStatement.setString(2, seed.name());
                insertStatement.setString(3, seed.description());
                insertStatement.setString(4, seed.type());
                insertStatement.setInt(5, seed.weight());
                insertStatement.addBatch();
            }
            insertStatement.executeBatch();
        }
    }

    private static void seedCourseTrackRules(Connection connection) throws SQLException {
        List<CourseTrackRuleSeed> seeds = List.of(
                new CourseTrackRuleSeed(1, 1, "בשחמה יעדמ", 1, true),
                new CourseTrackRuleSeed(2, 3, "בשחמה יעדמ", 2, true),
                new CourseTrackRuleSeed(3, 9, "בשחמה יעדמ", 2, true),
                new CourseTrackRuleSeed(4, 2, "בשחמה יעדמ", 3, true),
                new CourseTrackRuleSeed(5, 11, "בשחמה יעדמ", 3, true),
                new CourseTrackRuleSeed(6, 1, "הנכות תסדנה", 1, true),
                new CourseTrackRuleSeed(7, 3, "הנכות תסדנה", 1, true),
                new CourseTrackRuleSeed(8, 6, "הנכות תסדנה", 2, true),
                new CourseTrackRuleSeed(9, 9, "הנכות תסדנה", 2, true),
                new CourseTrackRuleSeed(10, 2, "הנכות תסדנה", 3, true),
                new CourseTrackRuleSeed(11, 7, "הנכות תסדנה", 3, true),
                new CourseTrackRuleSeed(12, 11, "הנכות תסדנה", 4, true),
                new CourseTrackRuleSeed(13, 1, "עדימ תוכרעמ", 1, true),
                new CourseTrackRuleSeed(14, 6, "עדימ תוכרעמ", 1, true),
                new CourseTrackRuleSeed(15, 9, "עדימ תוכרעמ", 2, true),
                new CourseTrackRuleSeed(16, 2, "עדימ תוכרעמ", 3, true),
                new CourseTrackRuleSeed(17, 10, "עדימ תוכרעמ", 3, false),
                new CourseTrackRuleSeed(18, 5, "בשחמה יעדמ", 4, false),
                new CourseTrackRuleSeed(19, 8, "בשחמה יעדמ", 4, false),
                new CourseTrackRuleSeed(20, 10, "הנכות תסדנה", 4, false)
        );

        try (Statement deleteStatement = connection.createStatement()) {
            deleteStatement.executeUpdate("DELETE FROM CourseTrackRules");
        }

        try (PreparedStatement insertStatement = connection.prepareStatement(
                "INSERT INTO CourseTrackRules (RuleID, CourseID, Track, [Year], IsMandatory) VALUES (?, ?, ?, ?, ?)")) {
            for (CourseTrackRuleSeed seed : seeds) {
                insertStatement.setInt(1, seed.ruleId());
                insertStatement.setInt(2, seed.courseId());
                insertStatement.setString(3, seed.track());
                insertStatement.setInt(4, seed.year());
                insertStatement.setBoolean(5, seed.mandatory());
                insertStatement.addBatch();
            }
            insertStatement.executeBatch();
        }
    }

    private static void seedStudentPreferences(Connection connection) throws SQLException {
        List<StudentSeed> students = loadStudents(connection);
        Map<Integer, CourseSeed> coursesById = loadCourses(connection);
        Map<String, Set<Integer>> mandatoryCoursesByTrackAndYear = loadMandatoryCoursesByTrackAndYear(connection);

        try (Statement deleteStatement = connection.createStatement()) {
            deleteStatement.executeUpdate("DELETE FROM StudentCoursePreferences");
        }

        int nextId = 1;
        try (PreparedStatement insertStatement = connection.prepareStatement(
                "INSERT INTO StudentCoursePreferences (PreferenceID, StudentID, CourseID, PreferenceRank) VALUES (?, ?, ?, ?)")) {
            for (StudentSeed student : students) {
                List<Integer> rankedCourses = buildRankedCoursePreferences(student, coursesById, mandatoryCoursesByTrackAndYear);
                int rank = 1;
                for (Integer courseId : rankedCourses) {
                    insertStatement.setInt(1, nextId++);
                    insertStatement.setInt(2, student.studentId());
                    insertStatement.setInt(3, courseId);
                    insertStatement.setInt(4, rank++);
                    insertStatement.addBatch();
                }
            }
            insertStatement.executeBatch();
        }
    }

    private static List<Integer> buildRankedCoursePreferences(
            StudentSeed student,
            Map<Integer, CourseSeed> coursesById,
            Map<String, Set<Integer>> mandatoryCoursesByTrackAndYear
    ) {
        List<Integer> rankedCourseIds = new ArrayList<>();

        List<Integer> mandatoryCourses = new ArrayList<>(mandatoryCoursesByTrackAndYear.getOrDefault(
                trackYearKey(student.track(), student.year()), Set.of()));
        mandatoryCourses.sort(Integer::compareTo);
        rankedCourseIds.addAll(mandatoryCourses);

        List<CourseSeed> optionalCourses = coursesById.values().stream()
                .filter(course -> !rankedCourseIds.contains(course.courseId()))
                .sorted(Comparator
                        .comparingInt((CourseSeed course) -> compatibilityScore(student, course))
                        .reversed()
                        .thenComparingInt(CourseSeed::courseId))
                .toList();

        for (CourseSeed course : optionalCourses) {
            rankedCourseIds.add(course.courseId());
            if (rankedCourseIds.size() >= 5) {
                break;
            }
        }

        return rankedCourseIds;
    }

    private static int compatibilityScore(StudentSeed student, CourseSeed course) {
        int score = 0;
        if (student.preferredDays().contains(course.day())) {
            score += 20;
        }

        if ("רקוב".equals(student.timePreference()) && course.startHour() < 14) {
            score += 12;
        } else if ("ברע".equals(student.timePreference()) && course.startHour() >= 14) {
            score += 12;
        }

        if (trackRelevantCourseIds(student.track()).contains(course.courseId())) {
            score += 16;
        }

        if ("הבוח".equals(course.courseType())) {
            score += 8;
        }

        if ("'א רטסמס".equals(course.semester()) && student.year() <= 2) {
            score += 3;
        }

        return score;
    }

    private static Set<Integer> trackRelevantCourseIds(String track) {
        return switch (track) {
            case "בשחמה יעדמ" -> Set.of(2, 5, 8, 10, 11);
            case "הנכות תסדנה" -> Set.of(2, 7, 8, 10, 11);
            case "עדימ תוכרעמ" -> Set.of(5, 6, 9, 10);
            default -> Set.of();
        };
    }

    private static List<StudentSeed> loadStudents(Connection connection) throws SQLException {
        List<StudentSeed> students = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT StudentID, [Year], Track, TimePreference, PreferredDays FROM Students")) {
            while (resultSet.next()) {
                students.add(new StudentSeed(
                        resultSet.getInt("StudentID"),
                        resultSet.getInt("Year"),
                        resultSet.getString("Track"),
                        resultSet.getString("TimePreference"),
                        parsePreferredDays(resultSet.getString("PreferredDays"))
                ));
            }
        }
        return students;
    }

    private static Map<Integer, CourseSeed> loadCourses(Connection connection) throws SQLException {
        Map<Integer, CourseSeed> courses = new LinkedHashMap<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT CourseID, CourseType, Day, StartTime, Semester FROM Courses")) {
            while (resultSet.next()) {
                courses.put(resultSet.getInt("CourseID"), new CourseSeed(
                        resultSet.getInt("CourseID"),
                        resultSet.getString("CourseType"),
                        resultSet.getString("Day"),
                        resultSet.getTime("StartTime").toLocalTime().getHour(),
                        resultSet.getString("Semester")
                ));
            }
        }
        return courses;
    }

    private static Map<String, Set<Integer>> loadMandatoryCoursesByTrackAndYear(Connection connection) throws SQLException {
        Map<String, Set<Integer>> rules = new LinkedHashMap<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT CourseID, Track, [Year] FROM CourseTrackRules WHERE IsMandatory = true")) {
            while (resultSet.next()) {
                rules.computeIfAbsent(
                                trackYearKey(resultSet.getString("Track"), resultSet.getInt("Year")),
                                ignored -> new java.util.LinkedHashSet<>())
                        .add(resultSet.getInt("CourseID"));
            }
        }
        return rules;
    }

    private static Set<String> parsePreferredDays(String value) {
        Set<String> days = new java.util.LinkedHashSet<>();
        if (value == null || value.isBlank()) {
            return days;
        }

        for (String part : value.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                days.add(trimmed);
            }
        }
        return days;
    }

    private static void dropTableIfExists(Connection connection, String tableName) throws SQLException {
        if (!tableExists(connection, tableName)) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE [" + tableName + "]");
        }
    }

    private static void ensureColumn(Connection connection, String tableName, String columnName, String definition) throws SQLException {
        if (columnExists(connection, tableName, columnName)) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            statement.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
    }

    private static boolean tableExists(Connection connection, String tableName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getTables(null, null, tableName, null)) {
            while (resultSet.next()) {
                if (tableName.equalsIgnoreCase(resultSet.getString("TABLE_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getColumns(null, null, tableName, columnName)) {
            while (resultSet.next()) {
                if (columnName.equalsIgnoreCase(resultSet.getString("COLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String trackYearKey(String track, int year) {
        return track + "|" + year;
    }

    private record ConstraintSeed(int id, String name, String description, String type, int weight) {
    }

    private record SemesterSeed(String normalValue, String reversedValue) {
    }

    private record StudentDisplaySeed(
            int studentId,
            String fullName,
            String track,
            String timePreference,
            String preferredDays
    ) {
    }

    private record CourseDisplaySeed(
            int courseId,
            String courseName,
            String courseType,
            String lecturer,
            String day,
            String semester
    ) {
    }

    private record CourseTrackRuleSeed(int ruleId, int courseId, String track, int year, boolean mandatory) {
    }

    private record StudentSeed(
            int studentId,
            int year,
            String track,
            String timePreference,
            Set<String> preferredDays
    ) {
    }

    private record CourseSeed(int courseId, String courseType, String day, int startHour, String semester) {
    }
}

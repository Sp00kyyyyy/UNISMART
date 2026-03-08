package com.example.java_main_proj;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
                normalizeLegacyValues(connection);
                seedConstraints(connection);
                seedCourseMetadata(connection);
                seedCourseTrackRules(connection);
                seedPrerequisites(connection);
                seedCompletedCourses(connection);
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

    private static void normalizeLegacyValues(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE Students SET TimePreference=? WHERE TimePreference=?")) {
            statement.setString(1, "ערב");
            statement.setString(2, "בערב");
            statement.executeUpdate();
        }
    }

    private static void seedConstraints(Connection connection) throws SQLException {
        if (countRows(connection, "Constraints") > 0) {
            return;
        }

        List<ConstraintSeed> seeds = List.of(
                new ConstraintSeed(1, "NO_TIME_CONFLICT", "A student cannot be assigned to overlapping courses.", "HARD", 1000),
                new ConstraintSeed(2, "COURSE_CAPACITY", "Course capacity cannot be exceeded.", "HARD", 1000),
                new ConstraintSeed(3, "PREREQUISITE_REQUIRED", "A student must satisfy all prerequisites before enrollment.", "HARD", 1000),
                new ConstraintSeed(4, "MAX_MANDATORY_COURSES", "A student cannot exceed the semester limit for mandatory courses.", "HARD", 1000),
                new ConstraintSeed(5, "MANDATORY_PRIORITY", "When seats are contested, mandatory requests get higher priority.", "POLICY", 75),
                new ConstraintSeed(6, "TIME_PREFERENCE", "Prefer courses that match the student's time preference.", "SOFT", 18),
                new ConstraintSeed(7, "PREFERRED_DAYS", "Prefer courses on the student's preferred days.", "SOFT", 14),
                new ConstraintSeed(8, "COURSE_PREFERENCE_RANK", "Prefer higher-ranked requests first.", "SOFT", 24)
        );

        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO [Constraints] (ConstraintID, ConstraintName, Description, ConstraintType, Weight) VALUES (?, ?, ?, ?, ?)")) {
            for (ConstraintSeed seed : seeds) {
                statement.setInt(1, seed.id());
                statement.setString(2, seed.name());
                statement.setString(3, seed.description());
                statement.setString(4, seed.type());
                statement.setInt(5, seed.weight());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void seedCourseMetadata(Connection connection) throws SQLException {
        Map<Integer, String> semesterByCourseId = new LinkedHashMap<>();
        semesterByCourseId.put(1, "סמסטר א'");
        semesterByCourseId.put(2, "סמסטר ב'");
        semesterByCourseId.put(3, "סמסטר א'");
        semesterByCourseId.put(5, "סמסטר ב'");
        semesterByCourseId.put(6, "סמסטר א'");
        semesterByCourseId.put(7, "סמסטר ב'");
        semesterByCourseId.put(8, "סמסטר ב'");
        semesterByCourseId.put(9, "סמסטר א'");
        semesterByCourseId.put(10, "סמסטר ב'");
        semesterByCourseId.put(11, "סמסטר א'");

        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE Courses SET Semester=? WHERE CourseID=?")) {
            for (Map.Entry<Integer, String> entry : semesterByCourseId.entrySet()) {
                statement.setString(1, entry.getValue());
                statement.setInt(2, entry.getKey());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void seedCourseTrackRules(Connection connection) throws SQLException {
        if (countRows(connection, "CourseTrackRules") > 0) {
            return;
        }

        List<CourseTrackRuleSeed> seeds = List.of(
                new CourseTrackRuleSeed(1, 1, "מדעי המחשב", 1, true),
                new CourseTrackRuleSeed(2, 3, "מדעי המחשב", 2, true),
                new CourseTrackRuleSeed(3, 9, "מדעי המחשב", 2, true),
                new CourseTrackRuleSeed(4, 2, "מדעי המחשב", 3, true),
                new CourseTrackRuleSeed(5, 11, "מדעי המחשב", 3, true),
                new CourseTrackRuleSeed(6, 1, "הנדסת תוכנה", 1, true),
                new CourseTrackRuleSeed(7, 3, "הנדסת תוכנה", 1, true),
                new CourseTrackRuleSeed(8, 6, "הנדסת תוכנה", 2, true),
                new CourseTrackRuleSeed(9, 9, "הנדסת תוכנה", 2, true),
                new CourseTrackRuleSeed(10, 2, "הנדסת תוכנה", 3, true),
                new CourseTrackRuleSeed(11, 7, "הנדסת תוכנה", 3, true),
                new CourseTrackRuleSeed(12, 11, "הנדסת תוכנה", 4, true),
                new CourseTrackRuleSeed(13, 1, "מערכות מידע", 1, true),
                new CourseTrackRuleSeed(14, 6, "מערכות מידע", 1, true),
                new CourseTrackRuleSeed(15, 9, "מערכות מידע", 2, true),
                new CourseTrackRuleSeed(16, 2, "מערכות מידע", 3, true),
                new CourseTrackRuleSeed(17, 10, "מערכות מידע", 3, false),
                new CourseTrackRuleSeed(18, 5, "מדעי המחשב", 4, false),
                new CourseTrackRuleSeed(19, 8, "מדעי המחשב", 4, false),
                new CourseTrackRuleSeed(20, 10, "הנדסת תוכנה", 4, false)
        );

        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO CourseTrackRules (RuleID, CourseID, Track, [Year], IsMandatory) VALUES (?, ?, ?, ?, ?)")) {
            for (CourseTrackRuleSeed seed : seeds) {
                statement.setInt(1, seed.ruleId());
                statement.setInt(2, seed.courseId());
                statement.setString(3, seed.track());
                statement.setInt(4, seed.year());
                statement.setBoolean(5, seed.mandatory());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void seedPrerequisites(Connection connection) throws SQLException {
        if (countRows(connection, "Prerequisites") > 0) {
            return;
        }

        List<int[]> seeds = List.of(
                new int[]{1, 2, 1},
                new int[]{2, 3, 1},
                new int[]{3, 5, 2},
                new int[]{4, 5, 3},
                new int[]{5, 7, 3},
                new int[]{6, 8, 2},
                new int[]{7, 8, 9},
                new int[]{8, 10, 9},
                new int[]{9, 11, 9}
        );

        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO Prerequisites (PrerequisiteID, CourseID, PrerequisiteCourseID) VALUES (?, ?, ?)")) {
            for (int[] seed : seeds) {
                statement.setInt(1, seed[0]);
                statement.setInt(2, seed[1]);
                statement.setInt(3, seed[2]);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private static void seedCompletedCourses(Connection connection) throws SQLException {
        if (countRows(connection, "CompletedCourses") > 0) {
            return;
        }

        Map<String, Set<Integer>> mandatoryCoursesByTrackAndYear = loadMandatoryCoursesByTrackAndYear(connection);
        List<StudentSeed> students = loadStudents(connection);
        int nextId = 1;

        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO CompletedCourses (CompletedID, StudentID, CourseID, CompletionDate, Grade) VALUES (?, ?, ?, ?, ?)")) {
            for (StudentSeed student : students) {
                Set<Integer> completed = new HashSet<>();
                for (int year = 1; year < student.year(); year++) {
                    completed.addAll(mandatoryCoursesByTrackAndYear.getOrDefault(trackYearKey(student.track(), year), Set.of()));
                }

                List<Integer> orderedCourses = completed.stream().sorted().toList();
                for (Integer courseId : orderedCourses) {
                    statement.setInt(1, nextId++);
                    statement.setInt(2, student.studentId());
                    statement.setInt(3, courseId);
                    statement.setDate(4, Date.valueOf(LocalDate.of(2025, 6, 30).minusDays(courseId)));
                    statement.setDouble(5, 78 + ((student.studentId() * 7 + courseId) % 18));
                    statement.addBatch();
                }
            }
            statement.executeBatch();
        }
    }

    private static void seedStudentPreferences(Connection connection) throws SQLException {
        if (countRows(connection, "StudentCoursePreferences") > 0) {
            return;
        }

        List<StudentSeed> students = loadStudents(connection);
        Map<Integer, CourseSeed> coursesById = loadCourses(connection);
        Map<Integer, Set<Integer>> prerequisitesByCourse = loadPrerequisiteMap(connection);
        Map<Integer, Set<Integer>> completedByStudent = loadCompletedCourseMap(connection);
        Map<String, Set<Integer>> mandatoryCoursesByTrackAndYear = loadMandatoryCoursesByTrackAndYear(connection);

        int nextId = 1;
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO StudentCoursePreferences (PreferenceID, StudentID, CourseID, PreferenceRank) VALUES (?, ?, ?, ?)")) {
            for (StudentSeed student : students) {
                List<Integer> rankedCourses = buildRankedCoursePreferences(
                        student,
                        coursesById,
                        prerequisitesByCourse,
                        completedByStudent.getOrDefault(student.studentId(), Set.of()),
                        mandatoryCoursesByTrackAndYear
                );

                int rank = 1;
                for (Integer courseId : rankedCourses) {
                    statement.setInt(1, nextId++);
                    statement.setInt(2, student.studentId());
                    statement.setInt(3, courseId);
                    statement.setInt(4, rank++);
                    statement.addBatch();
                }
            }
            statement.executeBatch();
        }
    }

    private static List<Integer> buildRankedCoursePreferences(
            StudentSeed student,
            Map<Integer, CourseSeed> coursesById,
            Map<Integer, Set<Integer>> prerequisitesByCourse,
            Set<Integer> completedCourseIds,
            Map<String, Set<Integer>> mandatoryCoursesByTrackAndYear
    ) {
        Set<Integer> chosenCourseIds = new HashSet<>();
        List<Integer> rankedCourseIds = new ArrayList<>();

        List<Integer> mandatoryCourses = new ArrayList<>(mandatoryCoursesByTrackAndYear.getOrDefault(
                trackYearKey(student.track(), student.year()), Set.of()));
        mandatoryCourses.sort(Integer::compareTo);
        for (Integer courseId : mandatoryCourses) {
            if (completedCourseIds.contains(courseId)) {
                continue;
            }
            if (prerequisitesSatisfied(prerequisitesByCourse, completedCourseIds, courseId)) {
                rankedCourseIds.add(courseId);
                chosenCourseIds.add(courseId);
            }
        }

        List<CourseSeed> optionalCourses = coursesById.values().stream()
                .filter(course -> !chosenCourseIds.contains(course.courseId()))
                .filter(course -> !completedCourseIds.contains(course.courseId()))
                .filter(course -> prerequisitesSatisfied(prerequisitesByCourse, completedCourseIds, course.courseId()))
                .sorted(Comparator
                        .comparingInt((CourseSeed course) -> compatibilityScore(student, course))
                        .reversed()
                        .thenComparingInt(CourseSeed::courseId))
                .toList();

        for (CourseSeed course : optionalCourses) {
            rankedCourseIds.add(course.courseId());
            chosenCourseIds.add(course.courseId());
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

        if ("בוקר".equals(student.timePreference()) && course.startHour() < 14) {
            score += 12;
        } else if ("ערב".equals(student.timePreference()) && course.startHour() >= 14) {
            score += 12;
        }

        if (trackRelevantCourseIds(student.track()).contains(course.courseId())) {
            score += 16;
        }

        if ("חובה".equals(course.courseType())) {
            score += 8;
        }

        if ("סמסטר א'".equals(course.semester()) && student.year() <= 2) {
            score += 3;
        }

        return score;
    }

    private static Set<Integer> trackRelevantCourseIds(String track) {
        return switch (track) {
            case "מדעי המחשב" -> Set.of(2, 5, 8, 10, 11);
            case "הנדסת תוכנה" -> Set.of(2, 7, 8, 10, 11);
            case "מערכות מידע" -> Set.of(5, 6, 9, 10);
            default -> Set.of();
        };
    }

    private static boolean prerequisitesSatisfied(
            Map<Integer, Set<Integer>> prerequisitesByCourse,
            Set<Integer> completedCourseIds,
            int courseId
    ) {
        return completedCourseIds.containsAll(prerequisitesByCourse.getOrDefault(courseId, Set.of()));
    }

    private static List<StudentSeed> loadStudents(Connection connection) throws SQLException {
        List<StudentSeed> students = new ArrayList<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT StudentID, FullName, [Year], Track, TimePreference, PreferredDays FROM Students")) {
            while (resultSet.next()) {
                students.add(new StudentSeed(
                        resultSet.getInt("StudentID"),
                        resultSet.getString("FullName"),
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
        Map<Integer, CourseSeed> courses = new HashMap<>();
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

    private static Map<Integer, Set<Integer>> loadPrerequisiteMap(Connection connection) throws SQLException {
        Map<Integer, Set<Integer>> prerequisites = new HashMap<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT CourseID, PrerequisiteCourseID FROM Prerequisites")) {
            while (resultSet.next()) {
                prerequisites.computeIfAbsent(resultSet.getInt("CourseID"), ignored -> new HashSet<>())
                        .add(resultSet.getInt("PrerequisiteCourseID"));
            }
        }
        return prerequisites;
    }

    private static Map<Integer, Set<Integer>> loadCompletedCourseMap(Connection connection) throws SQLException {
        Map<Integer, Set<Integer>> completed = new HashMap<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT StudentID, CourseID FROM CompletedCourses")) {
            while (resultSet.next()) {
                completed.computeIfAbsent(resultSet.getInt("StudentID"), ignored -> new HashSet<>())
                        .add(resultSet.getInt("CourseID"));
            }
        }
        return completed;
    }

    private static Map<String, Set<Integer>> loadMandatoryCoursesByTrackAndYear(Connection connection) throws SQLException {
        Map<String, Set<Integer>> rules = new HashMap<>();
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT CourseID, Track, [Year] FROM CourseTrackRules WHERE IsMandatory = true")) {
            while (resultSet.next()) {
                rules.computeIfAbsent(
                                trackYearKey(resultSet.getString("Track"), resultSet.getInt("Year")),
                                ignored -> new HashSet<>())
                        .add(resultSet.getInt("CourseID"));
            }
        }
        return rules;
    }

    private static Set<String> parsePreferredDays(String value) {
        Set<String> days = new HashSet<>();
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

    private static int countRows(Connection connection, String tableName) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM [" + tableName + "]")) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private static String trackYearKey(String track, int year) {
        return track + "|" + year;
    }

    private record ConstraintSeed(int id, String name, String description, String type, int weight) {
    }

    private record CourseTrackRuleSeed(int ruleId, int courseId, String track, int year, boolean mandatory) {
    }

    private record StudentSeed(
            int studentId,
            String fullName,
            int year,
            String track,
            String timePreference,
            Set<String> preferredDays
    ) {
    }

    private record CourseSeed(int courseId, String courseType, String day, int startHour, String semester) {
    }
}

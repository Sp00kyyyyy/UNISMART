package com.example.java_main_proj;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DatabaseBootstrap {
    private DatabaseBootstrap() {
    }

    public static synchronized void synchronize(Connection connection) {
        try {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                ensureGuidewaySchema(connection);
                dropObsoleteTables(connection);
                normalizeStudentData(connection);
                upsertEdgeCaseStudents(connection);
                normalizeCourseData(connection);
                upsertEdgeCaseCourses(connection);
                normalizeEnrollmentSemesters(connection);
                seedConstraints(connection);
                seedCourseTrackRules(connection);
                seedStudentPreferences(connection);
                connection.commit();
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
                new StudentDisplaySeed(1, "יהונתן רפאלי", "מדעי המחשב", "בוקר", "ראשון,שלישי,רביעי"),
                new StudentDisplaySeed(2, "שרה כהן", "מדעי המחשב", "ערב", "שני,רביעי,חמישי"),
                new StudentDisplaySeed(3, "דוד לוי", "הנדסת תוכנה", "בוקר", "ראשון,שני,שלישי"),
                new StudentDisplaySeed(4, "מיכל אברהם", "מערכות מידע", "בוקר", "שני,שלישי,רביעי"),
                new StudentDisplaySeed(5, "יוסי מזרחי", "מדעי המחשב", "ערב", "ראשון,רביעי,חמישי"),
                new StudentDisplaySeed(6, "רונית ישראלי", "הנדסת תוכנה", "בוקר", "שני,שלישי,חמישי"),
                new StudentDisplaySeed(7, "אבי שמעון", "מדעי המחשב", "ערב", "ראשון,שני,חמישי"),
                new StudentDisplaySeed(8, "נועה דוד", "מערכות מידע", "בוקר", "ראשון,שלישי,רביעי"),
                new StudentDisplaySeed(9, "אלי מור", "הנדסת תוכנה", "ערב", "שני,רביעי,חמישי"),
                new StudentDisplaySeed(10, "תמר לוי", "מדעי המחשב", "בוקר", "ראשון,שלישי,חמישי"),
                new StudentDisplaySeed(11, "אופק קונפורטי", "מדעי המחשב", "ערב", "ראשון,שלישי,חמישי")
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

        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE Students SET MaxMandatoryCourses=? WHERE StudentID=?")) {
            statement.setInt(1, 3);
            statement.setInt(2, 11);
            statement.executeUpdate();
        }
    }

    private static void upsertEdgeCaseStudents(Connection connection) throws SQLException {
        List<EdgeCaseStudentSeed> seeds = List.of(
                new EdgeCaseStudentSeed(12, "שחר חובה", "300000012", 1, "תרחיש קיץ", 1, 1, 95.0, "בוקר", "ראשון,שלישי,חמישי", 1),
                new EdgeCaseStudentSeed(13, "מאיה גיבוי", "300000013", 2, "תרחיש קיץ", 2, 2, 88.0, "בוקר", "ראשון,שלישי,חמישי", 5),
                new EdgeCaseStudentSeed(14, "גל חפיפה", "300000014", 2, "תרחיש קיץ", 2, 2, 84.0, "בוקר", "ראשון,חמישי", 5),
                new EdgeCaseStudentSeed(15, "נועה חלופה", "300000015", 4, "תרחיש קיץ", 1, 4, 91.0, "בוקר", "רביעי,חמישי", 4),
                new EdgeCaseStudentSeed(16, "רז ערב", "300000016", 3, "תרחיש קיץ", 2, 3, 87.0, "ערב", "חמישי", 4),
                new EdgeCaseStudentSeed(17, "איל מלא בלבד", "300000017", 3, "תרחיש קיץ", 3, 3, 79.0, "בוקר", "רביעי", 4),
                new EdgeCaseStudentSeed(18, "אור חובה אוטומטי", "300000018", 1, "תרחיש קיץ", 2, 1, 90.0, "בוקר", "שלישי,רביעי", 2),
                new EdgeCaseStudentSeed(19, "ליה עדיפות", "300000019", 2, "תרחיש קיץ", 1, 3, 93.0, "בוקר", "רביעי", 4),
                new EdgeCaseStudentSeed(20, "דן גיבוי מושב", "300000020", 2, "תרחיש קיץ", 3, 2, 82.0, "בוקר", "רביעי", 4),
                new EdgeCaseStudentSeed(21, "יעל בוקר", "300000021", 2, "תרחיש קיץ", 2, 2, 86.0, "בוקר", "רביעי,חמישי", 4),
                new EdgeCaseStudentSeed(22, "תום מלא גמיש", "300000022", 3, "תרחיש קיץ", 3, 3, 85.0, "", "שלישי,רביעי,חמישי", 4)
        );

        for (EdgeCaseStudentSeed seed : seeds) {
            upsertStudent(connection, seed);
        }
    }

    private static void normalizeCourseData(Connection connection) throws SQLException {
        List<CourseDisplaySeed> seeds = List.of(
                new CourseDisplaySeed(1, "מבוא למדעי המחשב", "חובה", "פרופ מרדכי כהן", "ראשון", "סמסטר א'"),
                new CourseDisplaySeed(2, "מבני נתונים ואלגוריתמים", "חובה", "דר רחל לוי", "שני", "סמסטר ב'"),
                new CourseDisplaySeed(3, "תכנות מונחה עצמים", "חובה", "פרופ אברהם דוד", "שלישי", "סמסטר א'"),
                new CourseDisplaySeed(5, "בינה מלאכותית", "בחירה", "דר שרה ישראלי", "רביעי", "סמסטר ב'"),
                new CourseDisplaySeed(6, "מערכות מידע", "חובה", "פרופ יוסף מזרחי", "ראשון", "סמסטר א'"),
                new CourseDisplaySeed(7, "הנדסת תוכנה", "חובה", "דר נועה אברהם", "שלישי", "סמסטר ב'"),
                new CourseDisplaySeed(8, "למידת מכונה", "בחירה", "פרופ דוד שמעון", "חמישי", "סמסטר ב'"),
                new CourseDisplaySeed(9, "מסדי נתונים", "חובה", "דר מיכל כהן", "שני", "סמסטר א'"),
                new CourseDisplaySeed(10, "אבטחת מידע", "בחירה", "פרופ משה ברק", "רביעי", "סמסטר ב'"),
                new CourseDisplaySeed(11, "רשתות מחשבים", "חובה", "דר יעל כץ", "חמישי", "סמסטר א'")
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

    private static void upsertEdgeCaseCourses(Connection connection) throws SQLException {
        List<EdgeCaseCourseSeed> seeds = List.of(
                new EdgeCaseCourseSeed(12, "סדנת קיץ חובה", "חובה", "ד\"ר יעל בן דוד", "ראשון", "09:00", "11:00", 1, 0, "סמסטר קיץ"),
                new EdgeCaseCourseSeed(13, "מעבדת קיץ חופפת", "בחירה", "ד\"ר עומר לוי", "ראשון", "10:00", "12:00", 2, 0, "סמסטר קיץ"),
                new EdgeCaseCourseSeed(14, "קורס קיץ מלא", "בחירה", "פרופ' דנה כהן", "רביעי", "14:00", "16:00", 1, 1, "סמסטר קיץ"),
                new EdgeCaseCourseSeed(15, "מעבדת קיץ חובה ב", "חובה", "ד\"ר אלון רז", "שלישי", "09:00", "11:00", 2, 0, "סמסטר קיץ"),
                new EdgeCaseCourseSeed(16, "סמינר ערב קיץ", "בחירה", "ד\"ר מיכל ברק", "חמישי", "17:00", "19:00", 3, 0, "סמסטר קיץ"),
                new EdgeCaseCourseSeed(17, "תרגול קיץ חופף", "בחירה", "ד\"ר נועה שלו", "ראשון", "09:30", "11:30", 2, 0, "סמסטר קיץ"),
                new EdgeCaseCourseSeed(18, "מעבדת פרויקט קיץ", "בחירה", "ד\"ר אורי בר", "שלישי", "12:00", "14:00", 5, 0, "סמסטר קיץ"),
                new EdgeCaseCourseSeed(19, "סדנת צהריים קיץ", "בחירה", "ד\"ר מיה שלו", "חמישי", "13:00", "15:00", 3, 0, "סמסטר קיץ"),
                new EdgeCaseCourseSeed(20, "סדנת ערב מתנגשת", "בחירה", "ד\"ר תמר אור", "חמישי", "14:00", "16:00", 3, 0, "סמסטר קיץ"),
                new EdgeCaseCourseSeed(21, "סמינר ביקוש גבוה", "בחירה", "ד\"ר ניר שדה", "רביעי", "09:00", "11:00", 1, 0, "סמסטר קיץ"),
                new EdgeCaseCourseSeed(22, "מעבדה חלופית", "בחירה", "ד\"ר ליאת שחר", "רביעי", "12:00", "14:00", 5, 0, "סמסטר קיץ")
        );

        for (EdgeCaseCourseSeed seed : seeds) {
            upsertCourse(connection, seed);
        }
    }

    private static void normalizeEnrollmentSemesters(Connection connection) throws SQLException {
        List<SemesterSeed> seeds = List.of(
                new SemesterSeed(reverse("סמסטר א'"), "סמסטר א'"),
                new SemesterSeed(reverse("סמסטר ב'"), "סמסטר ב'"),
                new SemesterSeed(reverse("סמסטר קיץ"), "סמסטר קיץ")
        );

        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE Enrollment SET Semester=? WHERE Semester=?")) {
            for (SemesterSeed seed : seeds) {
                statement.setString(1, seed.normalValue());
                statement.setString(2, seed.previousValue());
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
                new CourseTrackRuleSeed(20, 10, "הנדסת תוכנה", 4, false),
                new CourseTrackRuleSeed(21, 12, "תרחיש קיץ", 1, true),
                new CourseTrackRuleSeed(22, 15, "תרחיש קיץ", 1, true)
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
                List<StudentPreferenceSeed> explicitPreferences = edgeCasePreferences(student.studentId());
                if (!explicitPreferences.isEmpty()) {
                    for (StudentPreferenceSeed preference : explicitPreferences) {
                        if (!coursesById.containsKey(preference.courseId())) {
                            continue;
                        }
                        insertStatement.setInt(1, nextId++);
                        insertStatement.setInt(2, student.studentId());
                        insertStatement.setInt(3, preference.courseId());
                        insertStatement.setInt(4, preference.rank());
                        insertStatement.addBatch();
                    }
                    continue;
                }

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
        mandatoryCourses.removeIf(courseId -> isSummerCourse(coursesById.get(courseId)));
        mandatoryCourses.sort(Integer::compareTo);
        rankedCourseIds.addAll(mandatoryCourses);

        List<CourseSeed> optionalCourses = coursesById.values().stream()
                .filter(course -> !isSummerCourse(course))
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

    private static List<StudentPreferenceSeed> edgeCasePreferences(int studentId) {
        switch (studentId) {
            case 12 -> {
                return List.of(
                        new StudentPreferenceSeed(12, 1),
                        new StudentPreferenceSeed(15, 2),
                        new StudentPreferenceSeed(18, 3)
                );
            }
            case 13 -> {
                return List.of(
                        new StudentPreferenceSeed(12, 1),
                        new StudentPreferenceSeed(18, 2)
                );
            }
            case 14 -> {
                return List.of(
                        new StudentPreferenceSeed(13, 1),
                        new StudentPreferenceSeed(17, 2),
                        new StudentPreferenceSeed(16, 3)
                );
            }
            case 15 -> {
                return List.of(
                        new StudentPreferenceSeed(14, 1),
                        new StudentPreferenceSeed(19, 2)
                );
            }
            case 16 -> {
                return List.of(
                        new StudentPreferenceSeed(19, 1),
                        new StudentPreferenceSeed(20, 1),
                        new StudentPreferenceSeed(18, 2)
                );
            }
            case 17 -> {
                return List.of(new StudentPreferenceSeed(14, 1));
            }
            case 18 -> {
                return List.of(
                        new StudentPreferenceSeed(18, 1),
                        new StudentPreferenceSeed(22, 2)
                );
            }
            case 19 -> {
                return List.of(
                        new StudentPreferenceSeed(21, 1),
                        new StudentPreferenceSeed(22, 2)
                );
            }
            case 20 -> {
                return List.of(
                        new StudentPreferenceSeed(21, 1),
                        new StudentPreferenceSeed(22, 2)
                );
            }
            case 21 -> {
                return List.of(
                        new StudentPreferenceSeed(19, 1),
                        new StudentPreferenceSeed(20, 1),
                        new StudentPreferenceSeed(22, 2)
                );
            }
            case 22 -> {
                return List.of(
                        new StudentPreferenceSeed(18, 1),
                        new StudentPreferenceSeed(22, 2),
                        new StudentPreferenceSeed(16, 3)
                );
            }
            default -> {
                return List.of();
            }
        }
    }

    private static boolean isSummerCourse(CourseSeed course) {
        return course != null && "סמסטר קיץ".equals(course.semester());
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

    private static void upsertStudent(Connection connection, EdgeCaseStudentSeed seed) throws SQLException {
        try (PreparedStatement updateStatement = connection.prepareStatement(
                "UPDATE Students SET FullName=?, ID_Number=?, [Year]=?, Track=?, PriorityLevel=?, Seniority=?, GPA=?, TimePreference=?, PreferredDays=?, MaxMandatoryCourses=? WHERE StudentID=?")) {
            updateStatement.setString(1, seed.fullName());
            updateStatement.setString(2, seed.idNumber());
            updateStatement.setInt(3, seed.year());
            updateStatement.setString(4, seed.track());
            updateStatement.setInt(5, seed.priorityLevel());
            updateStatement.setInt(6, seed.seniority());
            updateStatement.setDouble(7, seed.gpa());
            updateStatement.setString(8, seed.timePreference());
            updateStatement.setString(9, seed.preferredDays());
            updateStatement.setInt(10, seed.maxMandatoryCourses());
            updateStatement.setInt(11, seed.studentId());

            int updated = updateStatement.executeUpdate();
            if (updated > 0) {
                return;
            }
        }

        try (PreparedStatement insertStatement = connection.prepareStatement(
                "INSERT INTO Students (StudentID, FullName, ID_Number, [Year], Track, PriorityLevel, Seniority, GPA, TimePreference, PreferredDays, MaxMandatoryCourses) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            insertStatement.setInt(1, seed.studentId());
            insertStatement.setString(2, seed.fullName());
            insertStatement.setString(3, seed.idNumber());
            insertStatement.setInt(4, seed.year());
            insertStatement.setString(5, seed.track());
            insertStatement.setInt(6, seed.priorityLevel());
            insertStatement.setInt(7, seed.seniority());
            insertStatement.setDouble(8, seed.gpa());
            insertStatement.setString(9, seed.timePreference());
            insertStatement.setString(10, seed.preferredDays());
            insertStatement.setInt(11, seed.maxMandatoryCourses());
            insertStatement.executeUpdate();
        }
    }

    private static void upsertCourse(Connection connection, EdgeCaseCourseSeed seed) throws SQLException {
        try (PreparedStatement updateStatement = connection.prepareStatement(
                "UPDATE Courses SET CourseName=?, CourseType=?, Lecturer=?, Day=?, StartTime=?, EndTime=?, Capacity=?, EnrolledStudents=?, Semester=? WHERE CourseID=?")) {
            updateStatement.setString(1, seed.courseName());
            updateStatement.setString(2, seed.courseType());
            updateStatement.setString(3, seed.lecturer());
            updateStatement.setString(4, seed.day());
            updateStatement.setTime(5, Time.valueOf(seed.startTime() + ":00"));
            updateStatement.setTime(6, Time.valueOf(seed.endTime() + ":00"));
            updateStatement.setInt(7, seed.capacity());
            updateStatement.setInt(8, seed.enrolledStudents());
            updateStatement.setString(9, seed.semester());
            updateStatement.setInt(10, seed.courseId());

            int updated = updateStatement.executeUpdate();
            if (updated > 0) {
                return;
            }
        }

        try (PreparedStatement insertStatement = connection.prepareStatement(
                "INSERT INTO Courses (CourseID, CourseName, CourseType, Lecturer, Day, StartTime, EndTime, Capacity, EnrolledStudents, Semester) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            insertStatement.setInt(1, seed.courseId());
            insertStatement.setString(2, seed.courseName());
            insertStatement.setString(3, seed.courseType());
            insertStatement.setString(4, seed.lecturer());
            insertStatement.setString(5, seed.day());
            insertStatement.setTime(6, Time.valueOf(seed.startTime() + ":00"));
            insertStatement.setTime(7, Time.valueOf(seed.endTime() + ":00"));
            insertStatement.setInt(8, seed.capacity());
            insertStatement.setInt(9, seed.enrolledStudents());
            insertStatement.setString(10, seed.semester());
            insertStatement.executeUpdate();
        }
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

    private static String reverse(String value) {
        return new StringBuilder(value).reverse().toString();
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

    private record SemesterSeed(String previousValue, String normalValue) {
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

    private record EdgeCaseStudentSeed(
            int studentId,
            String fullName,
            String idNumber,
            int year,
            String track,
            int priorityLevel,
            int seniority,
            double gpa,
            String timePreference,
            String preferredDays,
            int maxMandatoryCourses
    ) {
    }

    private record EdgeCaseCourseSeed(
            int courseId,
            String courseName,
            String courseType,
            String lecturer,
            String day,
            String startTime,
            String endTime,
            int capacity,
            int enrolledStudents,
            String semester
    ) {
    }

    private record StudentPreferenceSeed(int courseId, int rank) {
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

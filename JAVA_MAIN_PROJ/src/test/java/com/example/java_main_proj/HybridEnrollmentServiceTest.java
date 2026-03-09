package com.example.java_main_proj;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HybridEnrollmentServiceTest {
    private static final String FULL_STATUS = "\u05D4\u05E6\u05DC\u05D7\u05D4 \u05DE\u05DC\u05D0\u05D4";
    private static final String PARTIAL_STATUS = "\u05E9\u05D9\u05D1\u05D5\u05E5 \u05D7\u05DC\u05E7\u05D9";
    private static final String EMPTY_STATUS = "\u05DC\u05DC\u05D0 \u05E9\u05D9\u05D1\u05D5\u05E5";
    private static final String YEAR_SUFFIX = "\u05E9\u05E0\u05D4";

    @Test
    void runEnrollmentGivesTheSingleSeatToTheMandatoryStudent() {
        // Arrange
        Course sharedCourse = course(1, "Algorithms", "Monday", "10:00", "12:00", 1, "Fall");
        Student electiveStudent = student(1, "Ada", "OTHER", 1, 1, 4, 95.0, 5, "", "Monday,Tuesday,Wednesday,Thursday,Friday", List.of(
                new CoursePreference(1, 1)
        ));
        Student mandatoryStudent = student(2, "Grace", "CS", 1, 4, 1, 60.0, 5, "", "Monday,Tuesday,Wednesday,Thursday,Friday", List.of(
                new CoursePreference(1, 1)
        ));

        StubGuidewayRepository repository = new StubGuidewayRepository(
                List.of(electiveStudent, mandatoryStudent),
                List.of(sharedCourse),
                defaultConstraints(),
                List.of(new CourseRequirement(1, "CS", 1, true))
        );
        HybridEnrollmentService service = new HybridEnrollmentService(repository);

        // Act
        EnrollmentRunReport report = service.runEnrollment("2025-2026", "Fall");

        // Assert
        assertAll(
                () -> assertEquals(1, repository.savedDecisions().size()),
                () -> assertEquals(2, repository.savedDecisions().getFirst().getStudentId()),
                () -> assertEquals(1, report.getAssignedCourses()),
                () -> assertEquals(1, report.getFullAssignments()),
                () -> assertEquals(1, report.getUnassignedStudents())
        );
    }

    @Test
    void runEnrollmentStopsWhenTheStudentReachedTheMandatoryCourseLimit() {
        // Arrange
        Course firstMandatory = course(1, "Algorithms", "Monday", "09:00", "11:00", 20, "Spring");
        Course secondMandatory = course(2, "Databases", "Tuesday", "09:00", "11:00", 20, "Spring");
        Student student = student(1, "Dana", "CS", 1, 1, 3, 88.0, 1, "", "Monday,Tuesday,Wednesday,Thursday,Friday", List.of(
                new CoursePreference(1, 1),
                new CoursePreference(2, 2)
        ));

        StubGuidewayRepository repository = new StubGuidewayRepository(
                List.of(student),
                List.of(firstMandatory, secondMandatory),
                defaultConstraints(),
                List.of(
                        new CourseRequirement(1, "CS", 1, true),
                        new CourseRequirement(2, "CS", 1, true)
                )
        );
        HybridEnrollmentService service = new HybridEnrollmentService(repository);

        // Act
        EnrollmentRunReport report = service.runEnrollment("2025-2026", "Spring");

        // Assert
        assertAll(
                () -> assertEquals(1, repository.savedDecisions().size()),
                () -> assertEquals(1, repository.savedDecisions().getFirst().getCourseId()),
                () -> assertEquals(1, report.getAssignedCourses()),
                () -> assertEquals(1, report.getPartialAssignments()),
                () -> assertEquals(0, report.getFullAssignments())
        );
    }

    @Test
    void runEnrollmentKeepsTheHigherRankedCourseWhenRequestsOverlap() {
        // Arrange
        Course lowerRankedCourse = course(1, "Algorithms", "Monday", "10:00", "12:00", 20, "Summer");
        Course higherRankedCourse = course(2, "Networks", "Monday", "11:00", "13:00", 20, "Summer");
        Student student = student(1, "Noa", "OTHER", 1, 2, 2, 84.0, 5, "", "Monday,Tuesday,Wednesday,Thursday,Friday", List.of(
                new CoursePreference(1, 2),
                new CoursePreference(2, 1)
        ));

        StubGuidewayRepository repository = new StubGuidewayRepository(
                List.of(student),
                List.of(lowerRankedCourse, higherRankedCourse),
                defaultConstraints(),
                List.of()
        );
        HybridEnrollmentService service = new HybridEnrollmentService(repository);

        // Act
        EnrollmentRunReport report = service.runEnrollment("2025-2026", "Summer");

        // Assert
        assertAll(
                () -> assertEquals(1, repository.savedDecisions().size()),
                () -> assertEquals(2, repository.savedDecisions().getFirst().getCourseId()),
                () -> assertEquals(1, repository.savedDecisions().getFirst().getRequestedRank()),
                () -> assertEquals(1, report.getPartialAssignments()),
                () -> assertEquals(0, report.getFullAssignments())
        );
    }

    @Test
    void runEnrollmentUsesPreferenceScoreToBreakEqualRankOverlap() {
        // Arrange
        Course lowerScoredCourse = course(1, "Lab A", "Tuesday", "13:00", "16:00", 20, "Spring");
        Course higherScoredCourse = course(2, "Lab B", "Tuesday", "14:00", "17:00", 20, "Spring");
        Student student = student(
                1,
                "Tal",
                "OTHER",
                1,
                2,
                2,
                80.0,
                5,
                "\u05E2\u05E8\u05D1",
                "Tuesday",
                List.of(
                        new CoursePreference(1, 1),
                        new CoursePreference(2, 1)
                )
        );

        StubGuidewayRepository repository = new StubGuidewayRepository(
                List.of(student),
                List.of(lowerScoredCourse, higherScoredCourse),
                defaultConstraints(),
                List.of()
        );
        HybridEnrollmentService service = new HybridEnrollmentService(repository);

        // Act
        service.runEnrollment("2025-2026", "Spring");

        // Assert
        assertAll(
                () -> assertEquals(1, repository.savedDecisions().size()),
                () -> assertEquals(2, repository.savedDecisions().getFirst().getCourseId()),
                () -> assertEquals(1, repository.savedDecisions().getFirst().getRequestedRank())
        );
    }

    @Test
    void runEnrollmentFallsBackWhenTheMandatoryStudentGetsTheContestedSeat() {
        // Arrange
        Course contestedSeat = course(1, "Operating Systems", "Monday", "10:00", "12:00", 1, "Spring");
        Course fallbackCourse = course(2, "Databases", "Tuesday", "10:00", "12:00", 1, "Spring");
        Student electiveStudent = student(
                1,
                "Lior",
                "OTHER",
                1,
                1,
                3,
                89.0,
                5,
                "",
                "Monday,Tuesday,Wednesday,Thursday,Friday",
                List.of(
                        new CoursePreference(1, 1),
                        new CoursePreference(2, 2)
                )
        );
        Student mandatoryStudent = student(
                2,
                "Maya",
                "CS",
                1,
                4,
                1,
                70.0,
                5,
                "",
                "Monday,Tuesday,Wednesday,Thursday,Friday",
                List.of(new CoursePreference(1, 1))
        );

        StubGuidewayRepository repository = new StubGuidewayRepository(
                List.of(electiveStudent, mandatoryStudent),
                List.of(contestedSeat, fallbackCourse),
                defaultConstraints(),
                List.of(new CourseRequirement(1, "CS", 1, true))
        );
        HybridEnrollmentService service = new HybridEnrollmentService(repository);

        // Act
        EnrollmentRunReport report = service.runEnrollment("2025-2026", "Spring");
        Map<Integer, Integer> decisionCountsByCourse = repository.savedDecisions().stream()
                .collect(Collectors.toMap(
                        EnrollmentDecision::getCourseId,
                        ignored -> 1,
                        Integer::sum,
                        LinkedHashMap::new
                ));

        // Assert
        assertAll(
                () -> assertEquals(2, repository.savedDecisions().size()),
                () -> assertTrue(repository.savedDecisions().stream()
                        .anyMatch(decision -> decision.getStudentId() == 2 && decision.getCourseId() == 1)),
                () -> assertTrue(repository.savedDecisions().stream()
                        .anyMatch(decision -> decision.getStudentId() == 1 && decision.getCourseId() == 2)),
                () -> assertEquals(1, decisionCountsByCourse.get(1)),
                () -> assertEquals(1, decisionCountsByCourse.get(2)),
                () -> assertEquals(1, report.getFullAssignments()),
                () -> assertEquals(1, report.getPartialAssignments())
        );
    }

    private static Student student(
            int studentId,
            String fullName,
            String track,
            int year,
            int priorityLevel,
            int seniority,
            double gpa,
            int maxMandatoryCourses,
            String timePreference,
            String preferredDays,
            List<CoursePreference> preferences
    ) {
        Student student = new Student();
        student.setStudentID(studentId);
        student.setFullName(fullName);
        student.setIdNumber("ID-" + studentId);
        student.setTrack(track);
        student.setYear(year);
        student.setPriorityLevel(priorityLevel);
        student.setSeniority(seniority);
        student.setGpa(gpa);
        student.setMaxMandatoryCourses(maxMandatoryCourses);
        student.setTimePreference(timePreference);
        student.setPreferredDays(preferredDays);
        student.setPreferences(preferences);
        return student;
    }

    private static Course course(
            int courseId,
            String courseName,
            String day,
            String startTime,
            String endTime,
            int capacity,
            String semester
    ) {
        return new Course(courseId, courseName, "Required", "Lecturer", day, startTime, endTime, capacity, 0, semester);
    }

    private static Map<String, ConstraintRule> defaultConstraints() {
        Map<String, ConstraintRule> constraints = new LinkedHashMap<>();
        constraints.put("NO_TIME_CONFLICT", new ConstraintRule("NO_TIME_CONFLICT", "No overlaps", "HARD", 100));
        constraints.put("MANDATORY_PRIORITY", new ConstraintRule("MANDATORY_PRIORITY", "Mandatory first", "HARD", 75));
        constraints.put("MAX_MANDATORY_COURSES", new ConstraintRule("MAX_MANDATORY_COURSES", "Limit mandatory", "HARD", 100));
        constraints.put("COURSE_CAPACITY", new ConstraintRule("COURSE_CAPACITY", "Capacity", "HARD", 100));
        constraints.put("TIME_PREFERENCE", new ConstraintRule("TIME_PREFERENCE", "Time preference", "SOFT", 18));
        constraints.put("PREFERRED_DAYS", new ConstraintRule("PREFERRED_DAYS", "Preferred days", "SOFT", 14));
        constraints.put("COURSE_PREFERENCE_RANK", new ConstraintRule("COURSE_PREFERENCE_RANK", "Ranked requests", "SOFT", 24));
        return constraints;
    }

    private static final class StubGuidewayRepository extends GuidewayRepository {
        private final List<Student> students;
        private final List<Course> courses;
        private final Map<String, ConstraintRule> constraints;
        private final List<CourseRequirement> requirements;
        private List<EnrollmentDecision> savedDecisions = List.of();

        private StubGuidewayRepository(
                List<Student> students,
                List<Course> courses,
                Map<String, ConstraintRule> constraints,
                List<CourseRequirement> requirements
        ) {
            this.students = List.copyOf(students);
            this.courses = List.copyOf(courses);
            this.constraints = new LinkedHashMap<>(constraints);
            this.requirements = List.copyOf(requirements);
        }

        @Override
        public List<Student> loadStudents() {
            return students;
        }

        @Override
        public List<Course> loadCourses(String semester) {
            return courses.stream()
                    .filter(course -> semester.equals(course.getSemester()))
                    .toList();
        }

        @Override
        public List<CourseRequirement> loadCourseRequirements() {
            return requirements;
        }

        @Override
        public Map<String, ConstraintRule> loadConstraints() {
            return constraints;
        }

        @Override
        public void replaceEnrollmentRun(String academicYear, String semester, List<EnrollmentDecision> decisions) {
            this.savedDecisions = List.copyOf(decisions);
        }

        @Override
        public List<EnrollmentResult> loadEnrollmentResults(String academicYear, String semester) {
            Map<Integer, Course> coursesById = courses.stream()
                    .collect(Collectors.toMap(Course::getCourseID, course -> course));
            Map<Integer, List<EnrollmentDecision>> decisionsByStudent = savedDecisions.stream()
                    .collect(Collectors.groupingBy(
                            EnrollmentDecision::getStudentId,
                            LinkedHashMap::new,
                            Collectors.collectingAndThen(Collectors.toList(), decisions ->
                                    decisions.stream()
                                            .sorted(Comparator.comparingInt(EnrollmentDecision::getRequestedRank))
                                            .toList())
                    ));

            List<EnrollmentResult> results = new ArrayList<>();
            for (Student student : students) {
                int requested = (int) student.getPreferences().stream()
                        .map(CoursePreference::getCourseId)
                        .filter(coursesById::containsKey)
                        .filter(courseId -> semester.equals(coursesById.get(courseId).getSemester()))
                        .count();
                List<EnrollmentDecision> enrolledDecisions = decisionsByStudent.getOrDefault(student.getStudentID(), List.of());
                int enrolled = enrolledDecisions.size();
                String status = requested > 0 && enrolled == requested
                        ? FULL_STATUS
                        : enrolled > 0 ? PARTIAL_STATUS : EMPTY_STATUS;
                String courseList = enrolledDecisions.stream()
                        .map(EnrollmentDecision::getCourseId)
                        .map(coursesById::get)
                        .map(Course::getCourseName)
                        .collect(Collectors.joining(", "));

                results.add(new EnrollmentResult(
                        student.getIdNumber(),
                        student.getFullName(),
                        student.getYear() + " " + YEAR_SUFFIX,
                        requested,
                        enrolled,
                        status,
                        courseList
                ));
            }

            return results;
        }

        private List<EnrollmentDecision> savedDecisions() {
            return savedDecisions;
        }
    }
}

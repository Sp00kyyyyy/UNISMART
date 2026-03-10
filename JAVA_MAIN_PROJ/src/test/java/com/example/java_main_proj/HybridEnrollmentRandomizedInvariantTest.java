package com.example.java_main_proj;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HybridEnrollmentRandomizedInvariantTest {
    private static final String ACADEMIC_YEAR = "2025-2026";
    private static final String SEMESTER_A = "\u05E1\u05DE\u05E1\u05D8\u05E8 \u05D0'";
    private static final List<String> DAYS = List.of("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday");
    private static final List<String> TRACKS = List.of("CS", "IS", "SE");

    @Test
    void randomizedScenariosAlwaysRespectSchedulingInvariants() {
        Random random = new Random(42L);

        for (int scenario = 0; scenario < 40; scenario++) {
            ScenarioData data = createScenario(random, scenario);
            RandomizedRepository repository = new RandomizedRepository(
                    data.students(),
                    data.courses(),
                    defaultConstraints(),
                    data.requirements()
            );

            HybridEnrollmentService service = new HybridEnrollmentService(repository);
            EnrollmentRunReport report = service.runEnrollment(ACADEMIC_YEAR, SEMESTER_A);

            assertInvariants(repository, data, report);
        }
    }

    private void assertInvariants(RandomizedRepository repository, ScenarioData data, EnrollmentRunReport report) {
        List<EnrollmentDecision> decisions = repository.savedDecisions();
        Map<Integer, Course> coursesById = data.courses().stream()
                .collect(Collectors.toMap(Course::getCourseID, course -> course));
        Map<Integer, Student> studentsById = data.students().stream()
                .collect(Collectors.toMap(Student::getStudentID, student -> student));
        Map<Integer, Integer> assignmentsPerCourse = new LinkedHashMap<>();
        Map<Integer, Integer> mandatoryAssignmentsPerStudent = new LinkedHashMap<>();
        Map<Integer, Set<Integer>> seenCoursesPerStudent = new LinkedHashMap<>();
        Map<Integer, List<Course>> assignedCoursesPerStudent = new LinkedHashMap<>();

        assertEquals(report.getAssignedCourses(), decisions.size());

        for (EnrollmentDecision decision : decisions) {
            Course course = coursesById.get(decision.getCourseId());
            Student student = studentsById.get(decision.getStudentId());

            assertEquals(SEMESTER_A, decision.getSemester());
            assertEquals(ACADEMIC_YEAR, decision.getAcademicYear());
            assertTrue(course != null, "Every decision must reference a known course");
            assertTrue(student != null, "Every decision must reference a known student");
            assertEquals(SEMESTER_A, course.getSemester());

            assignmentsPerCourse.merge(course.getCourseID(), 1, Integer::sum);
            if (decision.isMandatory()) {
                mandatoryAssignmentsPerStudent.merge(student.getStudentID(), 1, Integer::sum);
            }

            Set<Integer> seenCourses = seenCoursesPerStudent.computeIfAbsent(student.getStudentID(), ignored -> new LinkedHashSet<>());
            assertTrue(seenCourses.add(course.getCourseID()), "Student received the same course twice");

            assignedCoursesPerStudent.computeIfAbsent(student.getStudentID(), ignored -> new ArrayList<>()).add(course);
        }

        for (Map.Entry<Integer, Integer> entry : assignmentsPerCourse.entrySet()) {
            Course course = coursesById.get(entry.getKey());
            assertTrue(entry.getValue() <= course.getCapacity(), "Course capacity exceeded");
        }

        for (Map.Entry<Integer, Integer> entry : mandatoryAssignmentsPerStudent.entrySet()) {
            Student student = studentsById.get(entry.getKey());
            assertTrue(entry.getValue() <= student.getMaxMandatoryCourses(), "Mandatory limit exceeded");
        }

        for (List<Course> assignedCourses : assignedCoursesPerStudent.values()) {
            for (int i = 0; i < assignedCourses.size(); i++) {
                for (int j = i + 1; j < assignedCourses.size(); j++) {
                    assertFalse(assignedCourses.get(i).overlapsWith(assignedCourses.get(j)), "Student has a time conflict");
                }
            }
        }
    }

    private ScenarioData createScenario(Random random, int scenarioIndex) {
        int courseCount = 6 + random.nextInt(5);
        int studentCount = 8 + random.nextInt(7);

        List<Course> courses = new ArrayList<>();
        for (int i = 0; i < courseCount; i++) {
            String day = DAYS.get(random.nextInt(DAYS.size()));
            int startHour = 8 + random.nextInt(9);
            int durationHours = 2 + random.nextInt(2);
            int capacity = 1 + random.nextInt(4);
            courses.add(new Course(
                    i + 1,
                    "Course " + scenarioIndex + "-" + (i + 1),
                    random.nextBoolean() ? "Required" : "Elective",
                    "Lecturer " + (i + 1),
                    day,
                    String.format("%02d:00", startHour),
                    String.format("%02d:00", startHour + durationHours),
                    capacity,
                    0,
                    SEMESTER_A
            ));
        }

        List<Student> students = new ArrayList<>();
        for (int i = 0; i < studentCount; i++) {
            String track = TRACKS.get(random.nextInt(TRACKS.size()));
            int year = 1 + random.nextInt(3);
            Student student = new Student();
            student.setStudentID(i + 1);
            student.setFullName("Student " + scenarioIndex + "-" + (i + 1));
            student.setIdNumber("ID-" + scenarioIndex + "-" + (i + 1));
            student.setTrack(track);
            student.setYear(year);
            student.setPriorityLevel(1 + random.nextInt(4));
            student.setSeniority(random.nextInt(5));
            student.setGpa(60 + random.nextInt(41));
            student.setMaxMandatoryCourses(1 + random.nextInt(3));
            student.setTimePreference(random.nextBoolean() ? "\u05D1\u05D5\u05E7\u05E8" : "\u05E2\u05E8\u05D1");
            student.setPreferredDays(randomPreferredDays(random));
            student.setPreferences(randomPreferences(random, courses));
            students.add(student);
        }

        List<CourseRequirement> requirements = new ArrayList<>();
        for (String track : TRACKS) {
            for (int year = 1; year <= 3; year++) {
                int mandatoryCount = 1 + random.nextInt(Math.min(3, courses.size()));
                List<Course> shuffled = new ArrayList<>(courses);
                shuffled.sort(Comparator.comparingInt(Course::getCourseID));
                java.util.Collections.shuffle(shuffled, random);
                for (int i = 0; i < mandatoryCount; i++) {
                    requirements.add(new CourseRequirement(shuffled.get(i).getCourseID(), track, year, true));
                }
            }
        }

        return new ScenarioData(List.copyOf(students), List.copyOf(courses), List.copyOf(requirements));
    }

    private List<CoursePreference> randomPreferences(Random random, List<Course> courses) {
        List<Course> shuffled = new ArrayList<>(courses);
        java.util.Collections.shuffle(shuffled, random);
        int preferenceCount = 2 + random.nextInt(Math.max(1, Math.min(4, shuffled.size()) - 1 + 1));
        List<CoursePreference> preferences = new ArrayList<>();
        for (int i = 0; i < preferenceCount && i < shuffled.size(); i++) {
            preferences.add(new CoursePreference(shuffled.get(i).getCourseID(), i + 1));
        }
        return preferences;
    }

    private String randomPreferredDays(Random random) {
        List<String> shuffledDays = new ArrayList<>(DAYS);
        java.util.Collections.shuffle(shuffledDays, random);
        int dayCount = 1 + random.nextInt(3);
        return String.join(",", shuffledDays.subList(0, dayCount));
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

    private record ScenarioData(List<Student> students, List<Course> courses, List<CourseRequirement> requirements) {
    }

    private static final class RandomizedRepository extends GuidewayRepository {
        private final List<Student> students;
        private final List<Course> courses;
        private final Map<String, ConstraintRule> constraints;
        private final List<CourseRequirement> requirements;
        private List<EnrollmentDecision> savedDecisions = List.of();

        private RandomizedRepository(
                List<Student> students,
                List<Course> courses,
                Map<String, ConstraintRule> constraints,
                List<CourseRequirement> requirements
        ) {
            this.students = students;
            this.courses = courses;
            this.constraints = constraints;
            this.requirements = requirements;
        }

        @Override
        public List<Student> loadStudents() {
            return students;
        }

        @Override
        public List<Course> loadCourses(String semester) {
            return courses.stream()
                    .filter(course -> semester == null || semester.equals(course.getSemester()))
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
            Map<Integer, List<EnrollmentDecision>> byStudent = savedDecisions.stream()
                    .collect(Collectors.groupingBy(EnrollmentDecision::getStudentId));
            List<EnrollmentResult> results = new ArrayList<>();
            for (Student student : students) {
                int requested = (int) student.getPreferences().stream()
                        .map(CoursePreference::getCourseId)
                        .distinct()
                        .count();
                int enrolled = byStudent.getOrDefault(student.getStudentID(), List.of()).size();
                String status = requested > 0 && enrolled == requested
                        ? "\u05D4\u05E6\u05DC\u05D7\u05D4 \u05DE\u05DC\u05D0\u05D4"
                        : enrolled > 0 ? "\u05E9\u05D9\u05D1\u05D5\u05E5 \u05D7\u05DC\u05E7\u05D9" : "\u05DC\u05DC\u05D0 \u05E9\u05D9\u05D1\u05D5\u05E5";
                results.add(new EnrollmentResult(
                        student.getIdNumber(),
                        student.getFullName(),
                        student.getYear() + " \u05E9\u05E0\u05D4",
                        requested,
                        enrolled,
                        status,
                        ""
                ));
            }
            return results;
        }

        private List<EnrollmentDecision> savedDecisions() {
            return savedDecisions;
        }
    }
}

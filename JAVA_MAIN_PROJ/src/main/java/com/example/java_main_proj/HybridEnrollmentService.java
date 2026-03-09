package com.example.java_main_proj;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class HybridEnrollmentService {
    private static final String FULL_STATUS = "הצלחה מלאה";
    private static final String PARTIAL_STATUS = "שיבוץ חלקי";

    private final GuidewayRepository repository = new GuidewayRepository();

    public EnrollmentRunReport runEnrollment(String academicYear, String semester) {
        List<String> logLines = new ArrayList<>();
        Map<String, ConstraintRule> constraints = repository.loadConstraints();
        List<Student> students = repository.loadStudents();
        List<Course> offeredCourses = repository.loadCourses(semester);
        Map<Integer, Course> coursesById = offeredCourses.stream()
                .collect(Collectors.toMap(Course::getCourseID, course -> course));
        List<CourseRequirement> requirements = repository.loadCourseRequirements();

        Map<Integer, Set<Integer>> mandatoryCoursesByStudent = buildMandatoryCoursesByStudent(students, requirements, coursesById);
        Map<Integer, List<RequestChoice>> requestsByStudent = buildRequestsByStudent(
                students, coursesById, mandatoryCoursesByStudent, constraints);

        List<Student> orderedStudents = new ArrayList<>(students);
        orderedStudents.sort(studentComparator(mandatoryCoursesByStudent, requestsByStudent));

        AssignmentState state = new AssignmentState(offeredCourses);
        int requestedCourses = requestsByStudent.values().stream().mapToInt(List::size).sum();

        logLines.add("Guideway sync complete.");
        logLines.add("Loaded " + students.size() + " students.");
        logLines.add("Loaded " + offeredCourses.size() + " courses for " + semester + ".");
        logLines.add("Processing " + requestedCourses + " ranked requests.");

        for (Student student : orderedStudents) {
            assignGreedy(student, requestsByStudent.getOrDefault(student.getStudentID(), List.of()), state, constraints);
        }

        int initialAssignments = state.totalAssignments();
        int improvements = runLocalImprovement(orderedStudents, requestsByStudent, state, constraints);
        int finalAssignments = state.totalAssignments();

        List<EnrollmentDecision> decisions = state.toDecisions(academicYear, semester);
        repository.replaceEnrollmentRun(academicYear, semester, decisions);

        List<EnrollmentResult> results = repository.loadEnrollmentResults(academicYear, semester);
        int fullAssignments = 0;
        int partialAssignments = 0;
        int unassignedStudents = 0;
        for (EnrollmentResult result : results) {
            if (FULL_STATUS.equals(result.getStatus())) {
                fullAssignments++;
            } else if (PARTIAL_STATUS.equals(result.getStatus())) {
                partialAssignments++;
            } else {
                unassignedStudents++;
            }
        }

        logLines.add("Greedy pass assigned " + initialAssignments + " courses.");
        logLines.add("Local improvement accepted " + improvements + " changes.");
        logLines.add("Final assignment count: " + finalAssignments + ".");
        logLines.add("Full schedules: " + fullAssignments + ", partial: " + partialAssignments + ", unassigned: " + unassignedStudents + ".");

        return new EnrollmentRunReport(
                academicYear,
                semester,
                students.size(),
                requestedCourses,
                finalAssignments,
                improvements,
                fullAssignments,
                partialAssignments,
                unassignedStudents,
                logLines
        );
    }

    private Map<Integer, Set<Integer>> buildMandatoryCoursesByStudent(
            List<Student> students,
            List<CourseRequirement> requirements,
            Map<Integer, Course> coursesById
    ) {
        Map<Integer, Set<Integer>> mandatoryCoursesByStudent = new HashMap<>();

        for (Student student : students) {
            Set<Integer> mandatoryCourses = requirements.stream()
                    .filter(CourseRequirement::isMandatory)
                    .filter(requirement -> requirement.getTrack().equals(student.getTrack()))
                    .filter(requirement -> requirement.getYear() == student.getYear())
                    .map(CourseRequirement::getCourseId)
                    .filter(coursesById::containsKey)
                    .collect(Collectors.toSet());
            mandatoryCoursesByStudent.put(student.getStudentID(), mandatoryCourses);
        }

        return mandatoryCoursesByStudent;
    }

    private Map<Integer, List<RequestChoice>> buildRequestsByStudent(
            List<Student> students,
            Map<Integer, Course> coursesById,
            Map<Integer, Set<Integer>> mandatoryCoursesByStudent,
            Map<String, ConstraintRule> constraints
    ) {
        Map<Integer, List<RequestChoice>> requestsByStudent = new HashMap<>();

        for (Student student : students) {
            Map<Integer, Integer> rankedCourses = new LinkedHashMap<>();
            for (CoursePreference preference : student.getPreferences()) {
                if (coursesById.containsKey(preference.getCourseId())) {
                    rankedCourses.putIfAbsent(preference.getCourseId(), preference.getPreferenceRank());
                }
            }

            for (Integer mandatoryCourseId : mandatoryCoursesByStudent.getOrDefault(student.getStudentID(), Set.of())) {
                if (coursesById.containsKey(mandatoryCourseId)) {
                    rankedCourses.putIfAbsent(mandatoryCourseId, rankedCourses.size() + 1);
                }
            }

            List<RequestChoice> requests = new ArrayList<>();
            for (Map.Entry<Integer, Integer> entry : rankedCourses.entrySet()) {
                Course course = coursesById.get(entry.getKey());
                if (course == null) {
                    continue;
                }

                boolean mandatory = mandatoryCoursesByStudent.getOrDefault(student.getStudentID(), Set.of())
                        .contains(course.getCourseID());
                double score = scoreRequest(student, course, entry.getValue(), mandatory, constraints);
                requests.add(new RequestChoice(course, entry.getValue(), mandatory, score));
            }

            requests.sort(Comparator
                    .comparing(RequestChoice::mandatory).reversed()
                    .thenComparingInt(RequestChoice::rank)
                    .thenComparing(Comparator.comparingDouble(RequestChoice::score).reversed()));
            requestsByStudent.put(student.getStudentID(), requests);
        }

        return requestsByStudent;
    }

    private Comparator<Student> studentComparator(
            Map<Integer, Set<Integer>> mandatoryCoursesByStudent,
            Map<Integer, List<RequestChoice>> requestsByStudent
    ) {
        return Comparator
                .comparingInt((Student student) -> pendingMandatoryCount(student, mandatoryCoursesByStudent, requestsByStudent))
                .reversed()
                .thenComparingInt(Student::getPriorityLevel)
                .thenComparing(Comparator.comparingInt(Student::getSeniority).reversed())
                .thenComparing(Comparator.comparingDouble(Student::getGpa).reversed())
                .thenComparingInt(Student::getStudentID);
    }

    private int pendingMandatoryCount(
            Student student,
            Map<Integer, Set<Integer>> mandatoryCoursesByStudent,
            Map<Integer, List<RequestChoice>> requestsByStudent
    ) {
        Set<Integer> mandatory = mandatoryCoursesByStudent.getOrDefault(student.getStudentID(), Set.of());
        return (int) requestsByStudent.getOrDefault(student.getStudentID(), List.of()).stream()
                .filter(request -> mandatory.contains(request.course().getCourseID()))
                .count();
    }

    private void assignGreedy(
            Student student,
            List<RequestChoice> requests,
            AssignmentState state,
            Map<String, ConstraintRule> constraints
    ) {
        for (RequestChoice request : requests) {
            if (state.isAssigned(student.getStudentID(), request.course().getCourseID())) {
                continue;
            }
            if (isFeasible(student, request, state)) {
                state.assign(student, request, scoreRequest(student, request.course(), request.rank(), request.mandatory(), constraints));
            }
        }
    }

    private int runLocalImprovement(
            List<Student> students,
            Map<Integer, List<RequestChoice>> requestsByStudent,
            AssignmentState state,
            Map<String, ConstraintRule> constraints
    ) {
        int improvements = 0;
        boolean changed;
        int pass = 0;

        do {
            changed = false;
            pass++;

            for (Student student : students) {
                for (RequestChoice request : requestsByStudent.getOrDefault(student.getStudentID(), List.of())) {
                    if (state.isAssigned(student.getStudentID(), request.course().getCourseID())) {
                        continue;
                    }

                    if (state.hasSeat(request.course().getCourseID()) && isFeasible(student, request, state)) {
                        state.assign(student, request, scoreRequest(student, request.course(), request.rank(), request.mandatory(), constraints));
                        improvements++;
                        changed = true;
                        continue;
                    }

                    if (tryDisplacement(student, request, requestsByStudent, state, constraints)) {
                        improvements++;
                        changed = true;
                    }
                }
            }
        } while (changed && pass < 3);

        return improvements;
    }

    private boolean tryDisplacement(
            Student requester,
            RequestChoice requestedCourse,
            Map<Integer, List<RequestChoice>> requestsByStudent,
            AssignmentState state,
            Map<String, ConstraintRule> constraints
    ) {
        List<AssignmentChoice> currentAssignees = new ArrayList<>(state.assignmentsForCourse(requestedCourse.course().getCourseID()));
        currentAssignees.sort(Comparator
                .comparingDouble((AssignmentChoice choice) -> accessPriority(choice.student(), choice.mandatory()))
                .thenComparingInt(choice -> choice.student().getStudentID()));

        for (AssignmentChoice assignee : currentAssignees) {
            if (accessPriority(requester, requestedCourse.mandatory()) <= accessPriority(assignee.student(), assignee.mandatory())) {
                continue;
            }

            RequestChoice alternative = findBestAlternativeRequest(
                    assignee.student(),
                    requestsByStudent.getOrDefault(assignee.student().getStudentID(), List.of()),
                    state,
                    requestedCourse.course().getCourseID()
            );
            if (alternative == null) {
                continue;
            }

            double currentScore = assignee.score();
            double replacementScore = scoreRequest(assignee.student(), alternative.course(), alternative.rank(), alternative.mandatory(), constraints);
            double requesterScore = scoreRequest(requester, requestedCourse.course(), requestedCourse.rank(), requestedCourse.mandatory(), constraints);

            boolean mandatoryUpgrade = requestedCourse.mandatory() && !assignee.mandatory();
            boolean betterTotalScore = requesterScore + replacementScore > currentScore;
            if (!mandatoryUpgrade && !betterTotalScore) {
                continue;
            }

            state.unassign(assignee.student().getStudentID(), requestedCourse.course().getCourseID());
            state.assign(assignee.student(), alternative, replacementScore);
            if (isFeasible(requester, requestedCourse, state)) {
                state.assign(requester, requestedCourse, requesterScore);
                return true;
            }

            state.unassign(assignee.student().getStudentID(), alternative.course().getCourseID());
            state.assign(assignee.student(), assignee.request(), assignee.score());
        }

        return false;
    }

    private RequestChoice findBestAlternativeRequest(
            Student student,
            List<RequestChoice> requests,
            AssignmentState state,
            int excludedCourseId
    ) {
        for (RequestChoice request : requests) {
            if (request.course().getCourseID() == excludedCourseId) {
                continue;
            }
            if (state.isAssigned(student.getStudentID(), request.course().getCourseID())) {
                continue;
            }
            if (!state.hasSeat(request.course().getCourseID())) {
                continue;
            }
            if (isFeasible(student, request, state)) {
                return request;
            }
        }
        return null;
    }

    private boolean isFeasible(Student student, RequestChoice request, AssignmentState state) {
        Course course = request.course();
        if (!state.hasSeat(course.getCourseID())) {
            return false;
        }

        if (request.mandatory() && state.mandatoryAssignmentCount(student.getStudentID()) >= student.getMaxMandatoryCourses()) {
            return false;
        }

        for (AssignmentChoice assignment : state.assignmentsForStudent(student.getStudentID())) {
            if (assignment.course().overlapsWith(course)) {
                return false;
            }
        }

        return true;
    }

    private double scoreRequest(
            Student student,
            Course course,
            int preferenceRank,
            boolean mandatory,
            Map<String, ConstraintRule> constraints
    ) {
        int coursePreferenceWeight = constraintWeight(constraints, "COURSE_PREFERENCE_RANK", 24);
        int dayWeight = constraintWeight(constraints, "PREFERRED_DAYS", 14);
        int timeWeight = constraintWeight(constraints, "TIME_PREFERENCE", 18);
        int mandatoryWeight = constraintWeight(constraints, "MANDATORY_PRIORITY", 75);

        int invertedRank = Math.max(1, 6 - preferenceRank);
        double score = invertedRank * coursePreferenceWeight;
        if (student.prefersDay(course.getDay())) {
            score += dayWeight;
        }
        if (student.prefersCourseTime(course)) {
            score += timeWeight;
        }
        if (mandatory) {
            score += mandatoryWeight;
        }

        score += Math.max(0, 5 - student.getPriorityLevel()) * 6.0;
        score += student.getSeniority() * 2.0;
        score += student.getGpa();
        return score;
    }

    private double accessPriority(Student student, boolean mandatory) {
        double score = (4 - student.getPriorityLevel()) * 100.0;
        score += student.getSeniority() * 10.0;
        score += student.getGpa() * 5.0;
        if (mandatory) {
            score += 75.0;
        }
        return score;
    }

    private int constraintWeight(Map<String, ConstraintRule> constraints, String name, int defaultValue) {
        ConstraintRule rule = constraints.get(name);
        return rule == null ? defaultValue : rule.getWeight();
    }

    private record RequestChoice(Course course, int rank, boolean mandatory, double score) {
    }

    private static final class AssignmentState {
        private final Map<Integer, Integer> remainingSeatsByCourse = new HashMap<>();
        private final Map<Integer, Map<Integer, AssignmentChoice>> assignmentsByStudent = new HashMap<>();
        private final Map<Integer, List<AssignmentChoice>> assignmentsByCourse = new HashMap<>();

        private AssignmentState(Collection<Course> courses) {
            for (Course course : courses) {
                remainingSeatsByCourse.put(course.getCourseID(), Math.max(0, course.getCapacity() - course.getEnrolledStudents()));
            }
        }

        private boolean hasSeat(int courseId) {
            return remainingSeatsByCourse.getOrDefault(courseId, 0) > 0;
        }

        private boolean isAssigned(int studentId, int courseId) {
            return assignmentsByStudent.getOrDefault(studentId, Map.of()).containsKey(courseId);
        }

        private void assign(Student student, RequestChoice request, double score) {
            AssignmentChoice choice = new AssignmentChoice(student, request.course(), request, score);
            assignmentsByStudent.computeIfAbsent(student.getStudentID(), ignored -> new LinkedHashMap<>())
                    .put(request.course().getCourseID(), choice);
            assignmentsByCourse.computeIfAbsent(request.course().getCourseID(), ignored -> new ArrayList<>())
                    .add(choice);
            remainingSeatsByCourse.computeIfPresent(request.course().getCourseID(), (ignored, seats) -> seats - 1);
        }

        private void unassign(int studentId, int courseId) {
            AssignmentChoice removed = assignmentsByStudent.getOrDefault(studentId, Map.of()).remove(courseId);
            if (removed == null) {
                return;
            }
            assignmentsByCourse.getOrDefault(courseId, List.of())
                    .removeIf(choice -> choice.student().getStudentID() == studentId);
            remainingSeatsByCourse.computeIfPresent(courseId, (ignored, seats) -> seats + 1);
        }

        private List<AssignmentChoice> assignmentsForStudent(int studentId) {
            return new ArrayList<>(assignmentsByStudent.getOrDefault(studentId, Map.of()).values());
        }

        private List<AssignmentChoice> assignmentsForCourse(int courseId) {
            return assignmentsByCourse.getOrDefault(courseId, List.of());
        }

        private int mandatoryAssignmentCount(int studentId) {
            return (int) assignmentsForStudent(studentId).stream().filter(AssignmentChoice::mandatory).count();
        }

        private int totalAssignments() {
            return assignmentsByStudent.values().stream().mapToInt(Map::size).sum();
        }

        private List<EnrollmentDecision> toDecisions(String academicYear, String semester) {
            List<EnrollmentDecision> decisions = new ArrayList<>();
            for (Map<Integer, AssignmentChoice> assignmentMap : assignmentsByStudent.values()) {
                for (AssignmentChoice assignment : assignmentMap.values()) {
                    decisions.add(new EnrollmentDecision(
                            assignment.student().getStudentID(),
                            assignment.course().getCourseID(),
                            academicYear,
                            semester,
                            assignment.score(),
                            assignment.request().rank(),
                            assignment.mandatory()
                    ));
                }
            }
            decisions.sort(Comparator
                    .comparingInt(EnrollmentDecision::getStudentId)
                    .thenComparingInt(EnrollmentDecision::getRequestedRank));
            return decisions;
        }
    }

    private record AssignmentChoice(Student student, Course course, RequestChoice request, double score) {
        private boolean mandatory() {
            return request.mandatory();
        }
    }
}

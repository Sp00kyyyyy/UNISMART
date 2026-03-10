package com.example.java_main_proj;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class HybridEnrollmentService {
    private static final String FULL_STATUS = "הצלחה מלאה";
    private static final String PARTIAL_STATUS = "שיבוץ חלקי";

    private final GuidewayRepository repository;

    public HybridEnrollmentService() {
        this(new GuidewayRepository());
    }

    public HybridEnrollmentService(GuidewayRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public EnrollmentRunReport runEnrollment(String academicYear, String semester) {
        List<String> logLines = new ArrayList<>();
        Map<String, ConstraintRule> constraints = repository.loadConstraints();
        WeightProfile weights = WeightProfile.from(constraints);
        List<Student> students = repository.loadStudents();
        List<Course> offeredCourses = repository.loadCourses(semester);
        Map<Integer, Course> coursesById = offeredCourses.stream()
                .collect(Collectors.toMap(Course::getCourseID, course -> course));
        List<CourseRequirement> requirements = repository.loadCourseRequirements();

        Map<Integer, Set<Integer>> mandatoryCoursesByStudent = buildMandatoryCoursesByStudent(students, requirements, coursesById);
        Map<Integer, StudentRequests> requestsByStudent = buildRequestsByStudent(
                students, coursesById, mandatoryCoursesByStudent, weights);
        int activeStudents = (int) requestsByStudent.values().stream()
                .filter(studentRequests -> !studentRequests.requests().isEmpty())
                .count();

        List<Student> orderedStudents = new ArrayList<>(students);
        orderedStudents.sort(studentComparator(mandatoryCoursesByStudent, requestsByStudent));

        AssignmentState state = new AssignmentState(offeredCourses);
        int requestedCourses = requestsByStudent.values().stream().mapToInt(StudentRequests::size).sum();

        logLines.add("Guideway sync complete.");
        logLines.add("Loaded " + students.size() + " students.");
        logLines.add("Loaded " + offeredCourses.size() + " courses for " + semester + ".");
        logLines.add("Processing " + requestedCourses + " ranked requests.");

        for (Student student : orderedStudents) {
            assignGreedy(student, requestsByStudent.getOrDefault(student.getStudentID(), StudentRequests.EMPTY).requests(), state);
        }

        int initialAssignments = state.totalAssignments();
        int improvements = runLocalImprovement(orderedStudents, requestsByStudent, state);
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
                activeStudents,
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
        Map<TrackYearKey, Set<Integer>> mandatoryCoursesByTrackYear = requirements.stream()
                .filter(CourseRequirement::isMandatory)
                .filter(requirement -> coursesById.containsKey(requirement.getCourseId()))
                .collect(Collectors.groupingBy(
                        requirement -> new TrackYearKey(requirement.getTrack(), requirement.getYear()),
                        Collectors.mapping(CourseRequirement::getCourseId, Collectors.toSet())
                ));
        Map<Integer, Set<Integer>> mandatoryCoursesByStudent = new HashMap<>();

        for (Student student : students) {
            Set<Integer> mandatoryCourses = mandatoryCoursesByTrackYear.getOrDefault(
                    new TrackYearKey(student.getTrack(), student.getYear()),
                    Set.of());
            mandatoryCoursesByStudent.put(student.getStudentID(), mandatoryCourses);
        }

        return mandatoryCoursesByStudent;
    }

    private Map<Integer, StudentRequests> buildRequestsByStudent(
            List<Student> students,
            Map<Integer, Course> coursesById,
            Map<Integer, Set<Integer>> mandatoryCoursesByStudent,
            WeightProfile weights
    ) {
        Map<Integer, StudentRequests> requestsByStudent = new HashMap<>();

        for (Student student : students) {
            Map<Integer, Integer> rankedCourses = new LinkedHashMap<>();
            Set<Integer> mandatoryCourses = mandatoryCoursesByStudent.getOrDefault(student.getStudentID(), Set.of());
            for (CoursePreference preference : student.getPreferences()) {
                if (coursesById.containsKey(preference.getCourseId())) {
                    rankedCourses.putIfAbsent(preference.getCourseId(), preference.getPreferenceRank());
                }
            }

            for (Integer mandatoryCourseId : mandatoryCourses) {
                if (coursesById.containsKey(mandatoryCourseId)) {
                    rankedCourses.putIfAbsent(mandatoryCourseId, rankedCourses.size() + 1);
                }
            }

            List<RequestChoice> requests = new ArrayList<>();
            int mandatoryRequestCount = 0;
            for (Map.Entry<Integer, Integer> entry : rankedCourses.entrySet()) {
                Course course = coursesById.get(entry.getKey());
                if (course == null) {
                    continue;
                }

                boolean mandatory = mandatoryCourses.contains(course.getCourseID());
                if (mandatory) {
                    mandatoryRequestCount++;
                }
                requests.add(new RequestChoice(
                        course,
                        entry.getValue(),
                        mandatory,
                        scoreRequest(student, course, entry.getValue(), mandatory, weights),
                        accessPriority(student, mandatory)
                ));
            }

            requests.sort(Comparator
                    .comparing(RequestChoice::mandatory).reversed()
                    .thenComparingInt(RequestChoice::rank)
                    .thenComparing(Comparator.comparingDouble(RequestChoice::score).reversed()));
            requestsByStudent.put(student.getStudentID(), new StudentRequests(List.copyOf(requests), mandatoryRequestCount));
        }

        return requestsByStudent;
    }

    private Comparator<Student> studentComparator(
            Map<Integer, Set<Integer>> mandatoryCoursesByStudent,
            Map<Integer, StudentRequests> requestsByStudent
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
            Map<Integer, StudentRequests> requestsByStudent
    ) {
        Set<Integer> mandatory = mandatoryCoursesByStudent.getOrDefault(student.getStudentID(), Set.of());
        if (mandatory.isEmpty()) {
            return 0;
        }
        return requestsByStudent.getOrDefault(student.getStudentID(), StudentRequests.EMPTY).mandatoryRequestCount();
    }

    private void assignGreedy(
            Student student,
            List<RequestChoice> requests,
            AssignmentState state
    ) {
        for (RequestChoice request : requests) {
            if (state.isAssigned(student.getStudentID(), request.course().getCourseID())) {
                continue;
            }
            if (isFeasible(student, request, state)) {
                state.assign(student, request);
            }
        }
    }

    private int runLocalImprovement(
            List<Student> students,
            Map<Integer, StudentRequests> requestsByStudent,
            AssignmentState state
    ) {
        int improvements = 0;
        boolean changed;
        int pass = 0;

        do {
            changed = false;
            pass++;

            for (Student student : students) {
                for (RequestChoice request : requestsByStudent.getOrDefault(student.getStudentID(), StudentRequests.EMPTY).requests()) {
                    if (state.isAssigned(student.getStudentID(), request.course().getCourseID())) {
                        continue;
                    }

                    if (state.hasSeat(request.course().getCourseID()) && isFeasible(student, request, state)) {
                        state.assign(student, request);
                        improvements++;
                        changed = true;
                        continue;
                    }

                    if (tryDisplacement(student, request, requestsByStudent, state)) {
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
            Map<Integer, StudentRequests> requestsByStudent,
            AssignmentState state
    ) {
        List<AssignmentChoice> currentAssignees = new ArrayList<>(state.assignmentsForCourse(requestedCourse.course().getCourseID()));
        currentAssignees.sort(Comparator
                .comparingDouble(AssignmentChoice::accessPriority)
                .thenComparingInt(choice -> choice.student().getStudentID()));

        for (AssignmentChoice assignee : currentAssignees) {
            if (requestedCourse.accessPriority() <= assignee.accessPriority()) {
                continue;
            }

            RequestChoice alternative = findBestAlternativeRequest(
                    assignee.student(),
                    requestsByStudent.getOrDefault(assignee.student().getStudentID(), StudentRequests.EMPTY).requests(),
                    state,
                    requestedCourse.course().getCourseID()
            );
            if (alternative == null) {
                continue;
            }

            double currentScore = assignee.score();
            double replacementScore = alternative.score();
            double requesterScore = requestedCourse.score();

            boolean mandatoryUpgrade = requestedCourse.mandatory() && !assignee.mandatory();
            boolean betterTotalScore = requesterScore + replacementScore > currentScore;
            if (!mandatoryUpgrade && !betterTotalScore) {
                continue;
            }

            state.unassign(assignee.student().getStudentID(), requestedCourse.course().getCourseID());
            state.assign(assignee.student(), alternative);
            if (isFeasible(requester, requestedCourse, state)) {
                state.assign(requester, requestedCourse);
                return true;
            }

            state.unassign(assignee.student().getStudentID(), alternative.course().getCourseID());
            state.assign(assignee.student(), assignee.request());
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

        for (AssignmentChoice assignment : state.assignmentsForStudentOnDay(student.getStudentID(), course.getDay())) {
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
            WeightProfile weights
    ) {
        int invertedRank = Math.max(1, 6 - preferenceRank);
        double score = invertedRank * weights.coursePreferenceWeight();
        if (student.prefersDay(course.getDay())) {
            score += weights.dayWeight();
        }
        if (student.prefersCourseTime(course)) {
            score += weights.timeWeight();
        }
        if (mandatory) {
            score += weights.mandatoryWeight();
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

    private record WeightProfile(int coursePreferenceWeight, int dayWeight, int timeWeight, int mandatoryWeight) {
        private static WeightProfile from(Map<String, ConstraintRule> constraints) {
            return new WeightProfile(
                    constraintWeight(constraints, "COURSE_PREFERENCE_RANK", 24),
                    constraintWeight(constraints, "PREFERRED_DAYS", 14),
                    constraintWeight(constraints, "TIME_PREFERENCE", 18),
                    constraintWeight(constraints, "MANDATORY_PRIORITY", 75)
            );
        }

        private static int constraintWeight(Map<String, ConstraintRule> constraints, String name, int defaultValue) {
            ConstraintRule rule = constraints.get(name);
            return rule == null ? defaultValue : rule.getWeight();
        }
    }

    private record TrackYearKey(String track, int year) {
    }

    private record StudentRequests(List<RequestChoice> requests, int mandatoryRequestCount) {
        private static final StudentRequests EMPTY = new StudentRequests(List.of(), 0);

        private int size() {
            return requests.size();
        }
    }

    private record RequestChoice(Course course, int rank, boolean mandatory, double score, double accessPriority) {
    }

    private static final class AssignmentState {
        private final Map<Integer, Integer> remainingSeatsByCourse = new HashMap<>();
        private final Map<Integer, Map<Integer, AssignmentChoice>> assignmentsByStudent = new HashMap<>();
        private final Map<Integer, Map<String, List<AssignmentChoice>>> assignmentsByStudentDay = new HashMap<>();
        private final Map<Integer, Integer> mandatoryAssignmentsByStudent = new HashMap<>();
        private final Map<Integer, Map<Integer, AssignmentChoice>> assignmentsByCourse = new HashMap<>();

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

        private void assign(Student student, RequestChoice request) {
            AssignmentChoice choice = new AssignmentChoice(student, request.course(), request, request.score(), request.accessPriority());
            assignmentsByStudent.computeIfAbsent(student.getStudentID(), ignored -> new LinkedHashMap<>())
                    .put(request.course().getCourseID(), choice);
            assignmentsByStudentDay
                    .computeIfAbsent(student.getStudentID(), ignored -> new HashMap<>())
                    .computeIfAbsent(request.course().getDay(), ignored -> new ArrayList<>())
                    .add(choice);
            assignmentsByCourse.computeIfAbsent(request.course().getCourseID(), ignored -> new LinkedHashMap<>())
                    .put(student.getStudentID(), choice);
            if (request.mandatory()) {
                mandatoryAssignmentsByStudent.merge(student.getStudentID(), 1, Integer::sum);
            }
            remainingSeatsByCourse.computeIfPresent(request.course().getCourseID(), (ignored, seats) -> seats - 1);
        }

        private void unassign(int studentId, int courseId) {
            AssignmentChoice removed = assignmentsByStudent.getOrDefault(studentId, Map.of()).remove(courseId);
            if (removed == null) {
                return;
            }
            Map<String, List<AssignmentChoice>> dayAssignments = assignmentsByStudentDay.getOrDefault(studentId, Map.of());
            List<AssignmentChoice> assignmentsOnDay = dayAssignments.getOrDefault(removed.course().getDay(), List.of());
            assignmentsOnDay.removeIf(choice -> choice.course().getCourseID() == courseId);
            if (assignmentsOnDay.isEmpty() && dayAssignments.containsKey(removed.course().getDay())) {
                dayAssignments.remove(removed.course().getDay());
            }
            assignmentsByCourse.getOrDefault(courseId, Map.of()).remove(studentId);
            if (removed.mandatory()) {
                mandatoryAssignmentsByStudent.computeIfPresent(studentId, (ignored, count) -> Math.max(0, count - 1));
            }
            remainingSeatsByCourse.computeIfPresent(courseId, (ignored, seats) -> seats + 1);
        }

        private Collection<AssignmentChoice> assignmentsForStudentOnDay(int studentId, String day) {
            return assignmentsByStudentDay.getOrDefault(studentId, Map.of()).getOrDefault(day, List.of());
        }

        private Collection<AssignmentChoice> assignmentsForCourse(int courseId) {
            return assignmentsByCourse.getOrDefault(courseId, Map.of()).values();
        }

        private int mandatoryAssignmentCount(int studentId) {
            return mandatoryAssignmentsByStudent.getOrDefault(studentId, 0);
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

    private record AssignmentChoice(Student student, Course course, RequestChoice request, double score, double accessPriority) {
        private boolean mandatory() {
            return request.mandatory();
        }
    }
}

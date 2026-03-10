package com.example.java_main_proj;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class HybridEnrollmentService {
    private static final String FULL_STATUS = "\u05D4\u05E6\u05DC\u05D7\u05D4 \u05DE\u05DC\u05D0\u05D4";
    private static final String PARTIAL_STATUS = "\u05E9\u05D9\u05D1\u05D5\u05E5 \u05D7\u05DC\u05E7\u05D9";

    private final GuidewayRepository repository;
    private final EnrollmentPlanningSupport planningSupport;

    public HybridEnrollmentService() {
        this(new GuidewayRepository());
    }

    public HybridEnrollmentService(GuidewayRepository repository) {
        this(repository, new EnrollmentPlanningSupport());
    }

    HybridEnrollmentService(GuidewayRepository repository, EnrollmentPlanningSupport planningSupport) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.planningSupport = Objects.requireNonNull(planningSupport, "planningSupport");
    }

    public EnrollmentRunReport runEnrollment(String academicYear, String semester) {
        List<String> logLines = new ArrayList<>();
        Map<String, ConstraintRule> constraints = repository.loadConstraints();
        List<Student> students = repository.loadStudents();
        List<Course> offeredCourses = repository.loadCourses(semester);
        offeredCourses.forEach(course -> course.setEnrolledStudents(0));
        Map<Integer, Course> coursesById = offeredCourses.stream()
                .collect(Collectors.toMap(Course::getCourseID, course -> course));
        List<CourseRequirement> requirements = repository.loadCourseRequirements();

        Map<Integer, Set<Integer>> mandatoryCoursesByStudent =
                planningSupport.buildMandatoryCoursesByStudent(students, requirements, coursesById);
        Map<Integer, EnrollmentPlanningSupport.StudentRequests> requestsByStudent =
                planningSupport.buildRequestsByStudent(students, coursesById, mandatoryCoursesByStudent, constraints);
        int activeStudents = (int) requestsByStudent.values().stream()
                .filter(studentRequests -> !studentRequests.requests().isEmpty())
                .count();

        List<Student> orderedStudents = new ArrayList<>(students);
        orderedStudents.sort(planningSupport.studentComparator(mandatoryCoursesByStudent, requestsByStudent));

        AssignmentState state = new AssignmentState(offeredCourses);
        int requestedCourses = requestsByStudent.values().stream()
                .mapToInt(EnrollmentPlanningSupport.StudentRequests::size)
                .sum();

        logLines.add("Guideway sync complete.");
        logLines.add("Loaded " + students.size() + " students.");
        logLines.add("Loaded " + offeredCourses.size() + " courses for " + semester + ".");
        logLines.add("Processing " + requestedCourses + " ranked requests.");

        for (Student student : orderedStudents) {
            assignGreedy(
                    student,
                    requestsByStudent.getOrDefault(student.getStudentID(), EnrollmentPlanningSupport.StudentRequests.EMPTY).requests(),
                    state
            );
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

    private void assignGreedy(
            Student student,
            List<EnrollmentPlanningSupport.RequestChoice> requests,
            AssignmentState state
    ) {
        for (EnrollmentPlanningSupport.RequestChoice request : requests) {
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
            Map<Integer, EnrollmentPlanningSupport.StudentRequests> requestsByStudent,
            AssignmentState state
    ) {
        int improvements = 0;
        boolean changed;
        int pass = 0;

        do {
            changed = false;
            pass++;

            for (Student student : students) {
                for (EnrollmentPlanningSupport.RequestChoice request
                        : requestsByStudent.getOrDefault(student.getStudentID(), EnrollmentPlanningSupport.StudentRequests.EMPTY).requests()) {
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
            EnrollmentPlanningSupport.RequestChoice requestedCourse,
            Map<Integer, EnrollmentPlanningSupport.StudentRequests> requestsByStudent,
            AssignmentState state
    ) {
        List<AssignmentState.AssignmentChoice> currentAssignees =
                new ArrayList<>(state.assignmentsForCourse(requestedCourse.course().getCourseID()));
        currentAssignees.sort(Comparator
                .comparingDouble(AssignmentState.AssignmentChoice::accessPriority)
                .thenComparingInt(choice -> choice.student().getStudentID()));

        for (AssignmentState.AssignmentChoice assignee : currentAssignees) {
            if (requestedCourse.accessPriority() <= assignee.accessPriority()) {
                continue;
            }

            EnrollmentPlanningSupport.RequestChoice alternative = findBestAlternativeRequest(
                    assignee.student(),
                    requestsByStudent.getOrDefault(
                            assignee.student().getStudentID(),
                            EnrollmentPlanningSupport.StudentRequests.EMPTY
                    ).requests(),
                    state,
                    requestedCourse.course().getCourseID()
            );
            if (alternative == null) {
                continue;
            }

            boolean mandatoryUpgrade = requestedCourse.mandatory() && !assignee.mandatory();
            boolean betterTotalScore = requestedCourse.score() + alternative.score() > assignee.score();
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

    private EnrollmentPlanningSupport.RequestChoice findBestAlternativeRequest(
            Student student,
            List<EnrollmentPlanningSupport.RequestChoice> requests,
            AssignmentState state,
            int excludedCourseId
    ) {
        for (EnrollmentPlanningSupport.RequestChoice request : requests) {
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

    private boolean isFeasible(Student student, EnrollmentPlanningSupport.RequestChoice request, AssignmentState state) {
        Course course = request.course();
        if (!state.hasSeat(course.getCourseID())) {
            return false;
        }

        if (request.mandatory() && state.mandatoryAssignmentCount(student.getStudentID()) >= student.getMaxMandatoryCourses()) {
            return false;
        }

        for (AssignmentState.AssignmentChoice assignment : state.assignmentsForStudentOnDay(student.getStudentID(), course.getDay())) {
            if (assignment.course().overlapsWith(course)) {
                return false;
            }
        }

        return true;
    }
}

package com.example.java_main_proj;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AssignmentState {
    private final Map<Integer, Integer> remainingSeatsByCourse = new HashMap<>();
    private final Map<Integer, Map<Integer, AssignmentChoice>> assignmentsByStudent = new HashMap<>();
    private final Map<Integer, Map<String, List<AssignmentChoice>>> assignmentsByStudentDay = new HashMap<>();
    private final Map<Integer, Integer> mandatoryAssignmentsByStudent = new HashMap<>();
    private final Map<Integer, Map<Integer, AssignmentChoice>> assignmentsByCourse = new HashMap<>();

    AssignmentState(Collection<Course> courses) {
        for (Course course : courses) {
            remainingSeatsByCourse.put(course.getCourseID(), Math.max(0, course.getCapacity() - course.getEnrolledStudents()));
        }
    }

    boolean hasSeat(int courseId) {
        return remainingSeatsByCourse.getOrDefault(courseId, 0) > 0;
    }

    boolean isAssigned(int studentId, int courseId) {
        return assignmentsByStudent.getOrDefault(studentId, Map.of()).containsKey(courseId);
    }

    void assign(Student student, SchedulePlanningService.RequestChoice request) {
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

    void unassign(int studentId, int courseId) {
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

    Collection<AssignmentChoice> assignmentsForStudentOnDay(int studentId, String day) {
        return assignmentsByStudentDay.getOrDefault(studentId, Map.of()).getOrDefault(day, List.of());
    }

    Collection<AssignmentChoice> assignmentsForCourse(int courseId) {
        return assignmentsByCourse.getOrDefault(courseId, Map.of()).values();
    }

    int mandatoryAssignmentCount(int studentId) {
        return mandatoryAssignmentsByStudent.getOrDefault(studentId, 0);
    }

    int totalAssignments() {
        return assignmentsByStudent.values().stream().mapToInt(Map::size).sum();
    }

    List<EnrollmentDecision> toDecisions(String academicYear, String semester) {
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

    record AssignmentChoice(
            Student student,
            Course course,
            SchedulePlanningService.RequestChoice request,
            double score,
            double accessPriority
    ) {
        boolean mandatory() {
            return request.mandatory();
        }
    }
}

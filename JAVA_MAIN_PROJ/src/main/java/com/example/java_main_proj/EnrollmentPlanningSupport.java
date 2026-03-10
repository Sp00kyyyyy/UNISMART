package com.example.java_main_proj;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class EnrollmentPlanningSupport {
    Map<Integer, Set<Integer>> buildMandatoryCoursesByStudent(
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
            mandatoryCoursesByStudent.put(
                    student.getStudentID(),
                    mandatoryCoursesByTrackYear.getOrDefault(new TrackYearKey(student.getTrack(), student.getYear()), Set.of())
            );
        }

        return mandatoryCoursesByStudent;
    }

    Map<Integer, StudentRequests> buildRequestsByStudent(
            List<Student> students,
            Map<Integer, Course> coursesById,
            Map<Integer, Set<Integer>> mandatoryCoursesByStudent,
            Map<String, ConstraintRule> constraints
    ) {
        WeightProfile weights = WeightProfile.from(constraints);
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

    Comparator<Student> studentComparator(
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

    record WeightProfile(int coursePreferenceWeight, int dayWeight, int timeWeight, int mandatoryWeight) {
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

    record TrackYearKey(String track, int year) {
    }

    record StudentRequests(List<RequestChoice> requests, int mandatoryRequestCount) {
        static final StudentRequests EMPTY = new StudentRequests(List.of(), 0);

        int size() {
            return requests.size();
        }
    }

    record RequestChoice(Course course, int rank, boolean mandatory, double score, double accessPriority) {
    }
}

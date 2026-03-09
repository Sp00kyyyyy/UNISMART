package com.example.java_main_proj;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudentTest {
    private static final String MORNING = "\u05D1\u05D5\u05E7\u05E8";
    private static final String EVENING = "\u05E2\u05E8\u05D1";

    @Test
    void prefersDayMatchesConfiguredPreferredDayList() {
        // Arrange
        Student student = new Student();
        student.setPreferredDays("Monday, Wednesday, Friday");

        // Act
        boolean prefersWednesday = student.prefersDay("Wednesday");
        boolean prefersTuesday = student.prefersDay("Tuesday");

        // Assert
        assertTrue(prefersWednesday);
        assertFalse(prefersTuesday);
    }

    @Test
    void prefersCourseTimeMatchesMorningAndEveningPreferences() {
        // Arrange
        Student morningStudent = new Student();
        morningStudent.setTimePreference(MORNING);

        Student eveningStudent = new Student();
        eveningStudent.setTimePreference(EVENING);

        Course morningCourse = new Course(1, "Algorithms", "Required", "Lecturer", "Monday", "09:00", "11:00", 30, 5);
        Course eveningCourse = new Course(2, "Networks", "Required", "Lecturer", "Monday", "17:00", "19:00", 30, 5);

        // Act
        boolean morningMatch = morningStudent.prefersCourseTime(morningCourse);
        boolean morningMismatch = morningStudent.prefersCourseTime(eveningCourse);
        boolean eveningMatch = eveningStudent.prefersCourseTime(eveningCourse);
        boolean eveningMismatch = eveningStudent.prefersCourseTime(morningCourse);

        // Assert
        assertTrue(morningMatch);
        assertFalse(morningMismatch);
        assertTrue(eveningMatch);
        assertFalse(eveningMismatch);
    }

    @Test
    void prefersCourseTimeDefaultsToTrueWhenNoPreferenceIsConfigured() {
        // Arrange
        Student student = new Student();
        Course course = new Course(1, "Algorithms", "Required", "Lecturer", "Monday", "18:00", "20:00", 30, 5);

        // Act
        boolean preferred = student.prefersCourseTime(course);

        // Assert
        assertTrue(preferred);
    }
}

package com.example.java_main_proj;

import com.example.java_main_proj.model.Course;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CourseTest {

    @Test
    void overlapsWithReturnsTrueForCoursesOnTheSameDayWithIntersectingTimes() {
        // Arrange
        Course firstCourse = new Course(1, "Algorithms", "Required", "Lecturer", "Monday", "10:00", "12:00", 30, 10);
        Course secondCourse = new Course(2, "Networks", "Required", "Lecturer", "Monday", "11:00", "13:00", 30, 5);

        // Act
        boolean overlaps = firstCourse.overlapsWith(secondCourse);

        // Assert
        assertTrue(overlaps);
    }

    @Test
    void overlapsWithReturnsFalseForDifferentDaysOrTouchingBoundaries() {
        // Arrange
        Course differentDay = new Course(1, "Algorithms", "Required", "Lecturer", "Monday", "10:00", "12:00", 30, 10);
        Course otherDay = new Course(2, "Networks", "Required", "Lecturer", "Tuesday", "11:00", "13:00", 30, 5);
        Course touchingBoundary = new Course(3, "Databases", "Required", "Lecturer", "Monday", "12:00", "14:00", 30, 8);

        // Act
        boolean overlapsDifferentDay = differentDay.overlapsWith(otherDay);
        boolean overlapsBoundary = differentDay.overlapsWith(touchingBoundary);

        // Assert
        assertFalse(overlapsDifferentDay);
        assertFalse(overlapsBoundary);
    }

    @Test
    void seatHelpersReflectRemainingCapacity() {
        // Arrange
        Course availableCourse = new Course(1, "Algorithms", "Required", "Lecturer", "Monday", "10:00", "12:00", 30, 28);
        Course fullCourse = new Course(2, "Networks", "Required", "Lecturer", "Tuesday", "14:00", "16:00", 20, 20);

        // Act
        int availableSeats = availableCourse.getAvailableSeats();
        boolean hasAvailableSeats = availableCourse.hasAvailableSeats();
        boolean hasSeatsWhenFull = fullCourse.hasAvailableSeats();

        // Assert
        assertEquals(2, availableSeats);
        assertTrue(hasAvailableSeats);
        assertFalse(hasSeatsWhenFull);
    }
}

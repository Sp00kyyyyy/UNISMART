package com.example.java_main_proj;

import java.time.LocalTime;

public class Course {
    private int courseID;
    private String courseName;
    private String courseType;
    private String lecturer;
    private String day;
    private String startTime;
    private String endTime;
    private int capacity;
    private int enrolledStudents;
    private String semester;

    public Course() {
    }

    public Course(int courseID, String courseName, String courseType, String lecturer,
                  String day, String startTime, String endTime, int capacity, int enrolledStudents) {
        this(courseID, courseName, courseType, lecturer, day, startTime, endTime, capacity, enrolledStudents, "");
    }

    public Course(int courseID, String courseName, String courseType, String lecturer,
                  String day, String startTime, String endTime, int capacity, int enrolledStudents, String semester) {
        this.courseID = courseID;
        this.courseName = courseName;
        this.courseType = courseType;
        this.lecturer = lecturer;
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
        this.capacity = capacity;
        this.enrolledStudents = enrolledStudents;
        this.semester = semester;
    }

    public int getCourseID() {
        return courseID;
    }

    public void setCourseID(int courseID) {
        this.courseID = courseID;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getCourseType() {
        return courseType;
    }

    public void setCourseType(String courseType) {
        this.courseType = courseType;
    }

    public String getLecturer() {
        return lecturer;
    }

    public void setLecturer(String lecturer) {
        this.lecturer = lecturer;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getEnrolledStudents() {
        return enrolledStudents;
    }

    public void setEnrolledStudents(int enrolledStudents) {
        this.enrolledStudents = enrolledStudents;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public boolean hasAvailableSeats() {
        return enrolledStudents < capacity;
    }

    public int getAvailableSeats() {
        return capacity - enrolledStudents;
    }

    public LocalTime getStartLocalTime() {
        return LocalTime.parse(startTime);
    }

    public LocalTime getEndLocalTime() {
        return LocalTime.parse(endTime);
    }

    public boolean overlapsWith(Course other) {
        if (other == null || day == null || !day.equals(other.day)) {
            return false;
        }

        return getStartLocalTime().isBefore(other.getEndLocalTime()) &&
                other.getStartLocalTime().isBefore(getEndLocalTime());
    }

    @Override
    public String toString() {
        return courseName + " (" + courseType + ")";
    }
}

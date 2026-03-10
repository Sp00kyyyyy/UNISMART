package com.example.java_main_proj.model;

public class EnrollmentResult {
    private String studentId;
    private String studentName;
    private String year;
    private int requestedCourses;
    private int enrolledCourses;
    private String status;
    private String coursesList;

    public EnrollmentResult(String studentId, String studentName, String year,
                            int requestedCourses, int enrolledCourses, String status, String coursesList) {
        this.studentId = studentId;
        this.studentName = studentName;
        this.year = year;
        this.requestedCourses = requestedCourses;
        this.enrolledCourses = enrolledCourses;
        this.status = status;
        this.coursesList = coursesList;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public int getRequestedCourses() {
        return requestedCourses;
    }

    public void setRequestedCourses(int requestedCourses) {
        this.requestedCourses = requestedCourses;
    }

    public int getEnrolledCourses() {
        return enrolledCourses;
    }

    public void setEnrolledCourses(int enrolledCourses) {
        this.enrolledCourses = enrolledCourses;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCoursesList() {
        return coursesList;
    }

    public void setCoursesList(String coursesList) {
        this.coursesList = coursesList;
    }
}

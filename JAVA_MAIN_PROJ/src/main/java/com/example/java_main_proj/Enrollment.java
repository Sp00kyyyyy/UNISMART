package com.example.java_main_proj;

import java.time.LocalDateTime;

public class Enrollment {
    private int enrollmentID;
    private int studentID;
    private int courseID;
    private LocalDateTime enrollmentDate;
    private String status;

    public Enrollment() {}

    public Enrollment(int enrollmentID, int studentID, int courseID,
                      LocalDateTime enrollmentDate, String status) {
        this.enrollmentID = enrollmentID;
        this.studentID = studentID;
        this.courseID = courseID;
        this.enrollmentDate = enrollmentDate;
        this.status = status;
    }

    public int getEnrollmentID() { return enrollmentID; }
    public void setEnrollmentID(int enrollmentID) { this.enrollmentID = enrollmentID; }

    public int getStudentID() { return studentID; }
    public void setStudentID(int studentID) { this.studentID = studentID; }

    public int getCourseID() { return courseID; }
    public void setCourseID(int courseID) { this.courseID = courseID; }

    public LocalDateTime getEnrollmentDate() { return enrollmentDate; }
    public void setEnrollmentDate(LocalDateTime enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

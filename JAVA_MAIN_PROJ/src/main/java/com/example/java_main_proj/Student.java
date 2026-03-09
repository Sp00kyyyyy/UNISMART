package com.example.java_main_proj;

import java.util.ArrayList;
import java.util.List;

public class Student {
    private int studentID;
    private String fullName;
    private String idNumber;
    private int year;
    private String track;
    private int priorityLevel;
    private int seniority;
    private double gpa;
    private String timePreference;
    private String preferredDays;
    private int maxMandatoryCourses;
    private List<CoursePreference> preferences = new ArrayList<>();

    public Student() {
    }

    public Student(int studentID, String fullName, String idNumber, int year,
                   String track, int priorityLevel, int seniority, double gpa) {
        this.studentID = studentID;
        this.fullName = fullName;
        this.idNumber = idNumber;
        this.year = year;
        this.track = track;
        this.priorityLevel = priorityLevel;
        this.seniority = seniority;
        this.gpa = gpa;
    }

    public int getStudentID() {
        return studentID;
    }

    public void setStudentID(int studentID) {
        this.studentID = studentID;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getIdNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getTrack() {
        return track;
    }

    public void setTrack(String track) {
        this.track = track;
    }

    public int getPriorityLevel() {
        return priorityLevel;
    }

    public void setPriorityLevel(int priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    public int getSeniority() {
        return seniority;
    }

    public void setSeniority(int seniority) {
        this.seniority = seniority;
    }

    public double getGpa() {
        return gpa;
    }

    public void setGpa(double gpa) {
        this.gpa = gpa;
    }

    public String getTimePreference() {
        return timePreference;
    }

    public void setTimePreference(String timePreference) {
        this.timePreference = timePreference;
    }

    public String getPreferredDays() {
        return preferredDays;
    }

    public void setPreferredDays(String preferredDays) {
        this.preferredDays = preferredDays;
    }

    public int getMaxMandatoryCourses() {
        return maxMandatoryCourses;
    }

    public void setMaxMandatoryCourses(int maxMandatoryCourses) {
        this.maxMandatoryCourses = maxMandatoryCourses;
    }

    public List<CoursePreference> getPreferences() {
        return preferences;
    }

    public void setPreferences(List<CoursePreference> preferences) {
        this.preferences = new ArrayList<>(preferences);
    }

    public boolean prefersDay(String day) {
        if (preferredDays == null || preferredDays.isBlank()) {
            return false;
        }

        for (String preferredDay : preferredDays.split(",")) {
            if (preferredDay.trim().equals(day)) {
                return true;
            }
        }
        return false;
    }

    public boolean prefersCourseTime(Course course) {
        if (timePreference == null || timePreference.isBlank()) {
            return true;
        }

        int startHour = course.getStartLocalTime().getHour();
        if (timePreference.contains("רקוב")) {
            return startHour < 14;
        }
        if (timePreference.contains("ברע")) {
            return startHour >= 14;
        }
        return true;
    }
}

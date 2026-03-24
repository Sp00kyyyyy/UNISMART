package com.example.java_main_proj.model;

import java.util.ArrayList;
import java.util.List;

/**
 * מודל הסטודנט.
 * מכיל גם נתוני זיהוי וגם נתונים שמשפיעים על החלטות האלגוריתם,
 * כמו קדימות, ותק, ממוצע והעדפות.
 */
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

    /**
     * יוצר אובייקט סטודנט ריק לצורך טעינה הדרגתית מן המסד.
     */
    public Student() {
    }

    /**
     * יוצר סטודנט עם נתוני הזיהוי והעדיפות המרכזיים.
     *
     * @param studentID מזהה פנימי של הסטודנט
     * @param fullName שם מלא
     * @param idNumber מספר תעודת זהות
     * @param year שנת לימוד
     * @param track מסלול לימודים
     * @param priorityLevel רמת עדיפות מנהלית
     * @param seniority ותק אקדמי
     * @param gpa ממוצע ציונים
     */
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

    /**
     * מחזיר את המזהה הפנימי של הסטודנט.
     *
     * @return המזהה הפנימי של הסטודנט
     */
    public int getStudentID() {
        return studentID;
    }

    /**
     * מעדכן את המזהה הפנימי של הסטודנט.
     *
     * @param studentID מזהה הסטודנט החדש
     */
    public void setStudentID(int studentID) {
        this.studentID = studentID;
    }

    /**
     * מחזיר את השם המלא של הסטודנט.
     *
     * @return השם המלא של הסטודנט
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * מעדכן את השם המלא של הסטודנט.
     *
     * @param fullName השם החדש
     */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    /**
     * מחזיר את מספר תעודת הזהות של הסטודנט.
     *
     * @return מספר תעודת הזהות של הסטודנט
     */
    public String getIdNumber() {
        return idNumber;
    }

    /**
     * מעדכן את מספר תעודת הזהות של הסטודנט.
     *
     * @param idNumber מספר הזהות החדש
     */
    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    /**
     * מחזיר את שנת הלימוד של הסטודנט.
     *
     * @return שנת הלימוד של הסטודנט
     */
    public int getYear() {
        return year;
    }

    /**
     * מעדכן את שנת הלימוד של הסטודנט.
     *
     * @param year שנת הלימוד החדשה
     */
    public void setYear(int year) {
        this.year = year;
    }

    /**
     * מחזיר את מסלול הלימודים של הסטודנט.
     *
     * @return מסלול הלימודים של הסטודנט
     */
    public String getTrack() {
        return track;
    }

    /**
     * מעדכן את מסלול הלימודים של הסטודנט.
     *
     * @param track המסלול החדש
     */
    public void setTrack(String track) {
        this.track = track;
    }

    /**
     * מחזיר את רמת העדיפות של הסטודנט.
     *
     * @return רמת העדיפות המנהלית של הסטודנט
     */
    public int getPriorityLevel() {
        return priorityLevel;
    }

    /**
     * מעדכן את רמת העדיפות של הסטודנט.
     *
     * @param priorityLevel רמת העדיפות החדשה
     */
    public void setPriorityLevel(int priorityLevel) {
        this.priorityLevel = priorityLevel;
    }

    /**
     * מחזיר את הוותק האקדמי של הסטודנט.
     *
     * @return הוותק האקדמי של הסטודנט
     */
    public int getSeniority() {
        return seniority;
    }

    /**
     * מעדכן את הוותק של הסטודנט.
     *
     * @param seniority ערך הוותק החדש
     */
    public void setSeniority(int seniority) {
        this.seniority = seniority;
    }

    /**
     * מחזיר את ממוצע הציונים של הסטודנט.
     *
     * @return ממוצע הציונים של הסטודנט
     */
    public double getGpa() {
        return gpa;
    }

    /**
     * מעדכן את ממוצע הציונים של הסטודנט.
     *
     * @param gpa ממוצע הציונים החדש
     */
    public void setGpa(double gpa) {
        this.gpa = gpa;
    }

    /**
     * מחזיר את העדפת הזמן של הסטודנט.
     *
     * @return העדפת הזמן של הסטודנט
     */
    public String getTimePreference() {
        return timePreference;
    }

    /**
     * מעדכן את העדפת הזמן של הסטודנט.
     *
     * @param timePreference העדפת הזמן החדשה
     */
    public void setTimePreference(String timePreference) {
        this.timePreference = timePreference;
    }

    /**
     * מחזיר את רשימת הימים המועדפים של הסטודנט.
     *
     * @return רשימת הימים המועדפים של הסטודנט כמחרוזת
     */
    public String getPreferredDays() {
        return preferredDays;
    }

    /**
     * מעדכן את רשימת הימים המועדפים של הסטודנט.
     *
     * @param preferredDays הימים המועדפים בפורמט טקסטואלי
     */
    public void setPreferredDays(String preferredDays) {
        this.preferredDays = preferredDays;
    }

    /**
     * מחזיר את מגבלת קורסי החובה של הסטודנט.
     *
     * @return מספר מקסימלי של קורסי חובה שמותר לשבץ לסטודנט
     */
    public int getMaxMandatoryCourses() {
        return maxMandatoryCourses;
    }

    /**
     * מעדכן את מגבלת קורסי החובה של הסטודנט.
     *
     * @param maxMandatoryCourses מספר מקסימלי חדש
     */
    public void setMaxMandatoryCourses(int maxMandatoryCourses) {
        this.maxMandatoryCourses = maxMandatoryCourses;
    }

    /**
     * מחזיר את רשימת העדפות הקורסים של הסטודנט.
     *
     * @return רשימת העדפות הקורסים של הסטודנט
     */
    public List<CoursePreference> getPreferences() {
        return preferences;
    }

    /**
     * מחליף את רשימת העדפות הקורסים של הסטודנט.
     *
     * @param preferences רשימת ההעדפות החדשה
     */
    public void setPreferences(List<CoursePreference> preferences) {
        this.preferences = new ArrayList<>(preferences);
    }

    /**
     * בודק האם יום מסוים נמצא ברשימת הימים המועדפים של הסטודנט.
     *
     * @param day היום הנבדק
     * @return {@code true} אם היום מועדף על הסטודנט, אחרת {@code false}
     */
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

    /**
     * בודק האם שעת ההתחלה של הקורס מתאימה להעדפת הזמן של הסטודנט.
     *
     * @param course הקורס הנבדק
     * @return {@code true} אם שעת הקורס מתאימה להעדפת הזמן, אחרת {@code false}
     */
    public boolean prefersCourseTime(Course course) {
        if (timePreference == null || timePreference.isBlank()) {
            return true;
        }

        int startHour = course.getStartLocalTime().getHour();
        if (timePreference.contains("בוקר")) {
            return startHour < 14;
        }
        if (timePreference.contains("ערב")) {
            return startHour >= 14;
        }
        return true;
    }
}

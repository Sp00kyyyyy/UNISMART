package com.example.java_main_proj.model;

import java.time.LocalTime;

/**
 * מודל קורס כפי שהוא נדרש לתהליך השיבוץ:
 * זיהוי, פרטי זמן, קיבולת ונתוני תצוגה.
 */
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

    /**
     * יוצר אובייקט קורס ריק לצורך טעינה הדרגתית מן המסד.
     */
    public Course() {
    }

    /**
     * יוצר קורס ללא שדה סמסטר מפורש.
     *
     * @param courseID מזהה הקורס
     * @param courseName שם הקורס
     * @param courseType סוג הקורס
     * @param lecturer שם המרצה
     * @param day יום ההוראה
     * @param startTime שעת התחלה
     * @param endTime שעת סיום
     * @param capacity קיבולת
     * @param enrolledStudents מספר נרשמים קיים
     */
    public Course(int courseID, String courseName, String courseType, String lecturer,
                  String day, String startTime, String endTime, int capacity, int enrolledStudents) {
        this(courseID, courseName, courseType, lecturer, day, startTime, endTime, capacity, enrolledStudents, "");
    }

    /**
     * יוצר קורס מלא עם כל הנתונים הדרושים לשיבוץ.
     *
     * @param courseID מזהה הקורס
     * @param courseName שם הקורס
     * @param courseType סוג הקורס
     * @param lecturer שם המרצה
     * @param day יום ההוראה
     * @param startTime שעת התחלה
     * @param endTime שעת סיום
     * @param capacity קיבולת מרבית
     * @param enrolledStudents מספר נרשמים קיים
     * @param semester הסמסטר שבו הקורס מוצע
     */
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

    /**
     * מחזיר את מזהה הקורס.
     *
     * @return מזהה הקורס
     */
    public int getCourseID() {
        return courseID;
    }

    /**
     * מעדכן את מזהה הקורס.
     *
     * @param courseID מזהה הקורס החדש
     */
    public void setCourseID(int courseID) {
        this.courseID = courseID;
    }

    /**
     * מחזיר את שם הקורס.
     *
     * @return שם הקורס
     */
    public String getCourseName() {
        return courseName;
    }

    /**
     * מעדכן את שם הקורס.
     *
     * @param courseName שם הקורס החדש
     */
    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    /**
     * מחזיר את סוג הקורס.
     *
     * @return סוג הקורס
     */
    public String getCourseType() {
        return courseType;
    }

    /**
     * מעדכן את סוג הקורס.
     *
     * @param courseType סוג הקורס החדש
     */
    public void setCourseType(String courseType) {
        this.courseType = courseType;
    }

    /**
     * מחזיר את שם המרצה.
     *
     * @return שם המרצה
     */
    public String getLecturer() {
        return lecturer;
    }

    /**
     * מעדכן את שם המרצה.
     *
     * @param lecturer שם המרצה החדש
     */
    public void setLecturer(String lecturer) {
        this.lecturer = lecturer;
    }

    /**
     * מחזיר את יום ההוראה של הקורס.
     *
     * @return יום ההוראה של הקורס
     */
    public String getDay() {
        return day;
    }

    /**
     * מעדכן את יום ההוראה של הקורס.
     *
     * @param day יום ההוראה החדש
     */
    public void setDay(String day) {
        this.day = day;
    }

    /**
     * מחזיר את שעת ההתחלה של הקורס.
     *
     * @return שעת ההתחלה בפורמט טקסטואלי
     */
    public String getStartTime() {
        return startTime;
    }

    /**
     * מעדכן את שעת ההתחלה של הקורס.
     *
     * @param startTime שעת ההתחלה החדשה
     */
    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    /**
     * מחזיר את שעת הסיום של הקורס.
     *
     * @return שעת הסיום בפורמט טקסטואלי
     */
    public String getEndTime() {
        return endTime;
    }

    /**
     * מעדכן את שעת הסיום של הקורס.
     *
     * @param endTime שעת הסיום החדשה
     */
    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    /**
     * מחזיר את קיבולת הקורס.
     *
     * @return קיבולת הקורס
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * מעדכן את קיבולת הקורס.
     *
     * @param capacity הקיבולת החדשה
     */
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    /**
     * מחזיר את מספר הנרשמים הנוכחי בקורס.
     *
     * @return מספר הסטודנטים הרשומים כעת
     */
    public int getEnrolledStudents() {
        return enrolledStudents;
    }

    /**
     * מעדכן את מספר הנרשמים הנוכחי בקורס.
     *
     * @param enrolledStudents מספר הנרשמים החדש
     */
    public void setEnrolledStudents(int enrolledStudents) {
        this.enrolledStudents = enrolledStudents;
    }

    /**
     * מחזיר את הסמסטר שבו הקורס מוצע.
     *
     * @return הסמסטר שבו הקורס מוצע
     */
    public String getSemester() {
        return semester;
    }

    /**
     * מעדכן את הסמסטר של הקורס.
     *
     * @param semester הסמסטר החדש
     */
    public void setSemester(String semester) {
        this.semester = semester;
    }

    /**
     * מציין אם נותרו מקומות פנויים בקורס.
     *
     * @return {@code true} אם נותרו מקומות פנויים בקורס
     */
    public boolean hasAvailableSeats() {
        return enrolledStudents < capacity;
    }

    /**
     * מחזיר את מספר המקומות הפנויים שנותרו בקורס.
     *
     * @return מספר המקומות הפנויים שנותרו בקורס
     */
    public int getAvailableSeats() {
        return capacity - enrolledStudents;
    }

    /**
     * מחזיר את שעת ההתחלה כאובייקט זמן.
     *
     * @return שעת ההתחלה כאובייקט {@link LocalTime}
     */
    public LocalTime getStartLocalTime() {
        return LocalTime.parse(startTime);
    }

    /**
     * מחזיר את שעת הסיום כאובייקט זמן.
     *
     * @return שעת הסיום כאובייקט {@link LocalTime}
     */
    public LocalTime getEndLocalTime() {
        return LocalTime.parse(endTime);
    }

    /**
     * בודק האם שני קורסים מתנגשים בזמן באותו יום.
     *
     * @param other קורס אחר להשוואה
     * @return {@code true} אם קיימת חפיפה בזמן, אחרת {@code false}
     */
    public boolean overlapsWith(Course other) {
        if (other == null || day == null || !day.equals(other.day)) {
            return false;
        }

        return getStartLocalTime().isBefore(other.getEndLocalTime()) &&
                other.getStartLocalTime().isBefore(getEndLocalTime());
    }

    /**
     * מחזיר ייצוג טקסטואלי קצר של הקורס.
     *
     * @return ייצוג טקסטואלי קצר של הקורס לצורכי תצוגה
     */
    @Override
    public String toString() {
        return courseName + " (" + courseType + ")";
    }
}

package com.example.java_main_proj.model;

/**
 * שורת תוצאה לתצוגה במסך התוצאות לאחר ריצת שיבוץ.
 */
public class EnrollmentResult {
    private String studentId;
    private String studentName;
    private String year;
    private int requestedCourses;
    private int enrolledCourses;
    private String status;
    private String coursesList;

    /**
     * יוצר שורת תוצאה להצגה במסך התוצאות.
     *
     * @param studentId מספר מזהה תצוגתי של הסטודנט
     * @param studentName שם הסטודנט
     * @param year שנת הלימוד לתצוגה
     * @param requestedCourses מספר הקורסים שביקש
     * @param enrolledCourses מספר הקורסים שקיבל
     * @param status סטטוס השיבוץ
     * @param coursesList רשימת הקורסים ששובצו
     */
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

    /**
     * מחזיר את מזהה התצוגה של הסטודנט.
     *
     * @return מזהה התצוגה של הסטודנט
     */
    public String getStudentId() {
        return studentId;
    }

    /**
     * מעדכן את מזהה התצוגה של הסטודנט.
     *
     * @param studentId המזהה החדש
     */
    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    /**
     * מחזיר את שם הסטודנט.
     *
     * @return שם הסטודנט
     */
    public String getStudentName() {
        return studentName;
    }

    /**
     * מעדכן את שם הסטודנט.
     *
     * @param studentName השם החדש
     */
    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    /**
     * מחזיר את שנת הלימוד לתצוגה.
     *
     * @return שנת הלימוד לתצוגה
     */
    public String getYear() {
        return year;
    }

    /**
     * מעדכן את שנת הלימוד לתצוגה.
     *
     * @param year השנה החדשה
     */
    public void setYear(String year) {
        this.year = year;
    }

    /**
     * מחזיר את מספר הקורסים שביקש הסטודנט.
     *
     * @return מספר הקורסים שביקש הסטודנט
     */
    public int getRequestedCourses() {
        return requestedCourses;
    }

    /**
     * מעדכן את מספר הקורסים שביקש הסטודנט.
     *
     * @param requestedCourses המספר החדש
     */
    public void setRequestedCourses(int requestedCourses) {
        this.requestedCourses = requestedCourses;
    }

    /**
     * מחזיר את מספר הקורסים שאליהם שובץ הסטודנט.
     *
     * @return מספר הקורסים שאליהם שובץ הסטודנט
     */
    public int getEnrolledCourses() {
        return enrolledCourses;
    }

    /**
     * מעדכן את מספר הקורסים ששובצו לסטודנט.
     *
     * @param enrolledCourses המספר החדש
     */
    public void setEnrolledCourses(int enrolledCourses) {
        this.enrolledCourses = enrolledCourses;
    }

    /**
     * מחזיר את סטטוס השיבוץ של הסטודנט.
     *
     * @return סטטוס השיבוץ של הסטודנט
     */
    public String getStatus() {
        return status;
    }

    /**
     * מעדכן את סטטוס השיבוץ של הסטודנט.
     *
     * @param status הסטטוס החדש
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * מחזיר את רשימת הקורסים ששובצו לסטודנט.
     *
     * @return רשימת הקורסים ששובצו לסטודנט כמחרוזת
     */
    public String getCoursesList() {
        return coursesList;
    }

    /**
     * מעדכן את רשימת הקורסים המשובצים לתצוגה.
     *
     * @param coursesList רשימת הקורסים החדשה
     */
    public void setCoursesList(String coursesList) {
        this.coursesList = coursesList;
    }
}

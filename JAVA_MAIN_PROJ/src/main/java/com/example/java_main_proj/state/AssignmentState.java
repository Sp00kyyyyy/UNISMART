package com.example.java_main_proj.state;

import com.example.java_main_proj.model.Course;
import com.example.java_main_proj.model.EnrollmentDecision;
import com.example.java_main_proj.model.Student;
import com.example.java_main_proj.service.SchedulePlanningService;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * אוגר מצב ריצה של השיבוץ.
 * במקום לחשב מחדש בכל צעד, המחלקה מחזיקה מבני עזר שמאפשרים לבדוק זמינות,
 * חפיפות וקיבולת בצורה מהירה בזמן הרצת האלגוריתם.
 */
public final class AssignmentState {
    // כמה מקומות עדיין פנויים בכל קורס.
    private final Map<Integer, Integer> remainingSeatsByCourse = new HashMap<>();
    // לכל סטודנט נשמר מיפוי הקורסים שכבר שובצו לו.
    private final Map<Integer, Map<Integer, AssignmentChoice>> assignmentsByStudent = new HashMap<>();
    // אינדקס לפי סטודנט ויום כדי לבדוק חפיפות רק מול אותו יום.
    private final Map<Integer, Map<String, List<AssignmentChoice>>> assignmentsByStudentDay = new HashMap<>();
    // כמה קורסי חובה כבר שובצו לכל סטודנט.
    private final Map<Integer, Integer> mandatoryAssignmentsByStudent = new HashMap<>();
    // לכל קורס נשמר מי כרגע יושב בו, כדי לאפשר דחיקה וחיפוש מהיר.
    private final Map<Integer, Map<Integer, AssignmentChoice>> assignmentsByCourse = new HashMap<>();

    /**
     * מאתחל את מצב הריצה לפי מספר המקומות הפנויים בכל קורס.
     *
     * @param courses רשימת הקורסים שממנה נגזר המצב ההתחלתי
     */
    public AssignmentState(Collection<Course> courses) {
        // בהתחלה רק מכינים כמה מקומות פנויים יש בכל קורס.
        for (Course course : courses) {
            // המצב מתחיל עם הקיבולת הפנויה האמיתית של כל קורס.
            remainingSeatsByCourse.put(course.getCourseID(), Math.max(0, course.getCapacity() - course.getEnrolledStudents()));
        }
    }

    /**
     * בודק האם נותר מקום פנוי בקורס מסוים.
     *
     * מחזיר האם קיים מקום פנוי בקורס הנתון.
     *
     * @param courseId מזהה הקורס
     * @return {@code true} אם יש מקום פנוי בקורס
     */
    public boolean hasSeat(int courseId) {
        // בדיקת מקום פנוי היא O(1) בזכות המפה הישירה.
        return remainingSeatsByCourse.getOrDefault(courseId, 0) > 0;
    }

    /**
     * בודק האם סטודנט כבר שובץ לקורס מסוים.
     *
     * מחזיר האם הסטודנט כבר שובץ לקורס הנתון.
     *
     * @param studentId מזהה הסטודנט
     * @param courseId מזהה הקורס
     * @return {@code true} אם הסטודנט כבר משובץ לקורס
     */
    public boolean isAssigned(int studentId, int courseId) {
        // בודק במהירות אם הסטודנט כבר משובץ לקורס הנתון.
        return assignmentsByStudent.getOrDefault(studentId, Map.of()).containsKey(courseId);
    }

    /**
     * מוסיף שיבוץ חדש ומעדכן את כל האינדקסים הנלווים.
     *
     * @param student הסטודנט המשובץ
     * @param request הבקשה שאושרה
     */
    public void assign(Student student, SchedulePlanningService.RequestChoice request) {
        // מוסיף שיבוץ חדש ומעדכן את כל המפות שצריכות לדעת עליו.
        // אובייקט אחד מאחד את כל פרטי השיבוץ כדי לא להחזיק אותם בכמה מבנים נפרדים.
        AssignmentChoice choice = new AssignmentChoice(student, request.course(), request, request.score(), request.accessPriority());
        // מעדכן את אינדקס השיבוצים הראשי של הסטודנט.
        assignmentsByStudent.computeIfAbsent(student.getStudentID(), ignored -> new LinkedHashMap<>())
                .put(request.course().getCourseID(), choice);
        // מעדכן גם את אינדקס היום כדי לאפשר בדיקות חפיפה מהירות.
        assignmentsByStudentDay
                .computeIfAbsent(student.getStudentID(), ignored -> new HashMap<>())
                .computeIfAbsent(request.course().getDay(), ignored -> new ArrayList<>())
                .add(choice);
        // האינדקס לפי קורס משמש במיוחד בשלב ה-displacement.
        assignmentsByCourse.computeIfAbsent(request.course().getCourseID(), ignored -> new LinkedHashMap<>())
                .put(student.getStudentID(), choice);
        if (request.mandatory()) {
            // ספירת החובה מתעדכנת רק עבור בקשות שסומנו כחובה.
            mandatoryAssignmentsByStudent.merge(student.getStudentID(), 1, Integer::sum);
        }
        // לאחר שיבוץ מוצלח מפחיתים מקום פנוי אחד מהקורס.
        remainingSeatsByCourse.computeIfPresent(request.course().getCourseID(), (ignored, seats) -> seats - 1);
    }

    /**
     * מבטל שיבוץ קיים ומחזיר את המצב למצב עקבי.
     *
     * @param studentId מזהה הסטודנט
     * @param courseId מזהה הקורס
     */
    public void unassign(int studentId, int courseId) {
        // מוחק שיבוץ קיים ומחזיר מקום פנוי לקורס.
        // מסיר תחילה מהאינדקס הראשי; אם לא נמצא שיבוץ אין מה לעדכן.
        AssignmentChoice removed = assignmentsByStudent.getOrDefault(studentId, Map.of()).remove(courseId);
        if (removed == null) {
            return;
        }

        // מסיר גם מאינדקס היום כדי שבדיקות החפיפה יישארו עקביות.
        Map<String, List<AssignmentChoice>> dayAssignments = assignmentsByStudentDay.getOrDefault(studentId, Map.of());
        List<AssignmentChoice> assignmentsOnDay = dayAssignments.getOrDefault(removed.course().getDay(), List.of());
        assignmentsOnDay.removeIf(choice -> choice.course().getCourseID() == courseId);
        if (assignmentsOnDay.isEmpty() && dayAssignments.containsKey(removed.course().getDay())) {
            // אם לא נשארו קורסים באותו יום, מנקים גם את המפתח של היום.
            dayAssignments.remove(removed.course().getDay());
        }

        // מסיר את הסטודנט גם מרשימת המשובצים של הקורס.
        assignmentsByCourse.getOrDefault(courseId, Map.of()).remove(studentId);
        if (removed.mandatory()) {
            // לא מאפשרים לספירה לרדת מתחת לאפס גם אם יש חוסר עקביות חיצוני.
            mandatoryAssignmentsByStudent.computeIfPresent(studentId, (ignored, count) -> Math.max(0, count - 1));
        }
        // פינוי שיבוץ מחזיר מקום פנוי אחד לקורס.
        remainingSeatsByCourse.computeIfPresent(courseId, (ignored, seats) -> seats + 1);
    }

    /**
     * מחזיר את כל שיבוצי הסטודנט ביום מסוים.
     *
     * מחזיר את רשימת השיבוצים של הסטודנט ביום הנתון.
     *
     * @param studentId מזהה הסטודנט
     * @param day היום הרצוי
     * @return אוסף השיבוצים של הסטודנט באותו יום
     */
    public Collection<AssignmentChoice> assignmentsForStudentOnDay(int studentId, String day) {
        // מחזיר רק את השיבוצים שיכולים להתנגש בזמן, במקום את כל מערכת השעות.
        return assignmentsByStudentDay.getOrDefault(studentId, Map.of()).getOrDefault(day, List.of());
    }

    /**
     * מחזיר את כל המשובצים לקורס מסוים.
     *
     * מחזיר את כלל המשובצים לקורס הנתון.
     *
     * @param courseId מזהה הקורס
     * @return אוסף המשובצים בקורס
     */
    public Collection<AssignmentChoice> assignmentsForCourse(int courseId) {
        // משמש בעיקר כשצריך לדעת את מי ניתן לדחוק מקורס מלא.
        return assignmentsByCourse.getOrDefault(courseId, Map.of()).values();
    }

    /**
     * מחזיר כמה קורסי חובה כבר שובצו לסטודנט.
     *
     * מחזיר את מספר קורסי החובה שכבר שובצו לסטודנט.
     *
     * @param studentId מזהה הסטודנט
     * @return מספר קורסי החובה המשובצים
     */
    public int mandatoryAssignmentCount(int studentId) {
        // קריאה ישירה לספירת קורסי החובה שכבר שובצו.
        return mandatoryAssignmentsByStudent.getOrDefault(studentId, 0);
    }

    /**
     * מחזיר את מספר השיבוצים הכולל במצב הריצה.
     *
     * @return מספר השיבוצים הכולל במצב הריצה הנוכחי
     */
    public int totalAssignments() {
        // סכימה של גודל המפות לכל סטודנט נותנת את מספר השיבוצים הכולל.
        return assignmentsByStudent.values().stream().mapToInt(Map::size).sum();
    }

    /**
     * ממיר את מצב הריצה לאוסף החלטות שניתן לשמור במסד הנתונים.
     *
     * @param academicYear שנת הלימודים של ההרצה
     * @param semester הסמסטר של ההרצה
     * @return רשימת החלטות שיבוץ לשמירה במסד
     */
    public List<EnrollmentDecision> toDecisions(String academicYear, String semester) {
        // בסוף ההרצה ממירים את המצב הפנימי לרשימה שאפשר לשמור במסד.
        List<EnrollmentDecision> decisions = new ArrayList<>();
        for (Map<Integer, AssignmentChoice> assignmentMap : assignmentsByStudent.values()) {
            for (AssignmentChoice assignment : assignmentMap.values()) {
                // כל שיבוץ פנימי מתורגם לרשומת החלטה אחת לשמירה במסד.
                decisions.add(new EnrollmentDecision(
                        assignment.student().getStudentID(),
                        assignment.course().getCourseID(),
                        academicYear,
                        semester,
                        assignment.score(),
                        assignment.request().rank(),
                        assignment.mandatory()
                ));
            }
        }
        // מיון סופי מייצר פלט יציב וצפוי להצגה ולבדיקות.
        decisions.sort(Comparator
                .comparingInt(EnrollmentDecision::getStudentId)
                .thenComparingInt(EnrollmentDecision::getRequestedRank));
        return decisions;
    }

    /**
     * מייצג שיבוץ בודד כפי שהוא נשמר במצב הריצה.
     *
     * @param student הסטודנט ששובץ
     * @param course הקורס שאליו שובץ
     * @param request הבקשה המקורית שאושרה
     * @param score ציון האיכות של השיבוץ
     * @param accessPriority עדיפות הגישה של הבקשה
     */
    public record AssignmentChoice(
            Student student,
            Course course,
            SchedulePlanningService.RequestChoice request,
            double score,
            double accessPriority
    ) {
        /**
         * קיצור נוח לגישה למידע האם השיבוץ הגיע מדרישת חובה.
         *
         * מחזיר האם השיבוץ הנוכחי הגיע מבקשת חובה.
         *
         * @return {@code true} אם הבקשה שסיפקה את השיבוץ היא בקשת חובה
         */
        public boolean mandatory() {
            return request.mandatory();
        }
    }
}

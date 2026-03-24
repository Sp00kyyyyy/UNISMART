package com.example.java_main_proj.service;

import com.example.java_main_proj.model.ConstraintRule;
import com.example.java_main_proj.model.Course;
import com.example.java_main_proj.model.CoursePreference;
import com.example.java_main_proj.model.CourseRequirement;
import com.example.java_main_proj.model.Student;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * שכבת ההכנה הלוגית של האלגוריתם.
 * המחלקה מתרגמת את נתוני המסד לבקשות מדורגות ולמדדי עדיפות.
 */
public final class SchedulePlanningService {
    /**
     * בונה עבור כל סטודנט את קבוצת קורסי החובה הרלוונטיים לו לפי מסלול ושנה.
     *
     * @param students רשימת הסטודנטים
     * @param requirements רשימת חוקי החובה מן המסד
     * @param coursesById מפת קורסים לפי מזהה
     * @return מיפוי מכל סטודנט לקבוצת קורסי החובה שלו
     */
    public Map<Integer, Set<Integer>> buildMandatoryCoursesByStudent(
            List<Student> students,
            List<CourseRequirement> requirements,
            Map<Integer, Course> coursesById
    ) {
        // מקבץ פעם אחת את קורסי החובה לפי צירוף של מסלול ושנה כדי לא לחשב זאת מחדש לכל סטודנט.
        Map<TrackYearKey, Set<Integer>> mandatoryCoursesByTrackYear = requirements.stream()
                .filter(CourseRequirement::isMandatory)
                .filter(requirement -> coursesById.containsKey(requirement.getCourseId()))
                .collect(Collectors.groupingBy(
                        requirement -> new TrackYearKey(requirement.getTrack(), requirement.getYear()),
                        Collectors.mapping(CourseRequirement::getCourseId, Collectors.toSet())
                ));
        // התוצאה הסופית: מיפוי ישיר מכל סטודנט לקבוצת קורסי החובה שלו.
        Map<Integer, Set<Integer>> mandatoryCoursesByStudent = new HashMap<>();

        for (Student student : students) {
            // אם אין התאמה למסלול ולשנה, הסטודנט מקבל קבוצה ריקה במקום null.
            mandatoryCoursesByStudent.put(
                    student.getStudentID(),
                    mandatoryCoursesByTrackYear.getOrDefault(new TrackYearKey(student.getTrack(), student.getYear()), Set.of())
            );
        }

        return mandatoryCoursesByStudent;
    }

    /**
     * מאחד העדפות אישיות וקורסי חובה לרשימת בקשות אחת לכל סטודנט.
     *
     * @param students רשימת הסטודנטים
     * @param coursesById מפת הקורסים המוצעים לפי מזהה
     * @param mandatoryCoursesByStudent קורסי החובה לכל סטודנט
     * @param constraints משקלי אילוצים וכללי ניקוד
     * @return מפת בקשות מוכנות לעבודה עבור כל סטודנט
     */
    public Map<Integer, StudentRequests> buildRequestsByStudent(
            List<Student> students,
            Map<Integer, Course> coursesById,
            Map<Integer, Set<Integer>> mandatoryCoursesByStudent,
            Map<String, ConstraintRule> constraints
    ) {
        // טוען את משקלי הניקוד מהמסד, או ערכי ברירת מחדל אם אין כלל מתאים.
        WeightProfile weights = WeightProfile.from(constraints);
        Map<Integer, StudentRequests> requestsByStudent = new HashMap<>();

        for (Student student : students) {
            // LinkedHashMap שומר את סדר ההכנסה, ולכן העדפות המשתמש נשמרות כמו שהוזנו.
            Map<Integer, Integer> rankedCourses = new LinkedHashMap<>();
            Set<Integer> mandatoryCourses = mandatoryCoursesByStudent.getOrDefault(student.getStudentID(), Set.of());
            for (CoursePreference preference : student.getPreferences()) {
                // שומר רק קורסים שבאמת מוצעים בסמסטר, וללא כפילויות.
                if (coursesById.containsKey(preference.getCourseId())) {
                    rankedCourses.putIfAbsent(preference.getCourseId(), preference.getPreferenceRank());
                }
            }

            for (Integer mandatoryCourseId : mandatoryCourses) {
                // אם קורס החובה לא הופיע בהעדפות האישיות, מוסיפים אותו לסוף הרשימה.
                if (coursesById.containsKey(mandatoryCourseId)) {
                    rankedCourses.putIfAbsent(mandatoryCourseId, rankedCourses.size() + 1);
                }
            }

            List<RequestChoice> requests = new ArrayList<>();
            int mandatoryRequestCount = 0;
            for (Map.Entry<Integer, Integer> entry : rankedCourses.entrySet()) {
                Course course = coursesById.get(entry.getKey());
                if (course != null) {
                    // מסמן האם הבקשה הזו נחשבת חובה עבור הסטודנט הספציפי.
                    boolean mandatory = mandatoryCourses.contains(course.getCourseID());
                    if (mandatory) {
                        mandatoryRequestCount++;
                    }
                    // RequestChoice מרכז במקום אחד את כל הנתונים שהאלגוריתם צריך בזמן אמת.
                    requests.add(new RequestChoice(
                            course,
                            entry.getValue(),
                            mandatory,
                            scoreRequest(student, course, entry.getValue(), mandatory, weights),
                            accessPriority(student, mandatory)
                    ));
                }
            }

            // סדר הבקשות קובע את אופן הסריקה של הגרידי ושל החיפוש המקומי.
            requests.sort(Comparator
                    .comparing(RequestChoice::mandatory).reversed()
                    .thenComparingInt(RequestChoice::rank)
                    .thenComparing(Comparator.comparingDouble(RequestChoice::score).reversed()));
            // העתקה בלתי ניתנת לשינוי מגנה על הסדר שחושב כאן מפני שינוי חיצוני בהמשך.
            requestsByStudent.put(student.getStudentID(), new StudentRequests(List.copyOf(requests), mandatoryRequestCount));
        }

        return requestsByStudent;
    }

    /**
     * מגדיר את סדר הטיפול בסטודנטים לפני תחילת השיבוץ.
     *
     * @param mandatoryCoursesByStudent קורסי החובה לכל סטודנט
     * @param requestsByStudent הבקשות המוכנות של כל סטודנט
     * @return משווה שקובע את סדר הטיפול בסטודנטים
     */
    public Comparator<Student> studentComparator(
            Map<Integer, Set<Integer>> mandatoryCoursesByStudent,
            Map<Integer, StudentRequests> requestsByStudent
    ) {
        return Comparator
                // קודם מטפלים בסטודנטים שיש להם יותר קורסי חובה פתוחים לטיפול.
                .comparingInt((Student student) -> pendingMandatoryCount(student, mandatoryCoursesByStudent, requestsByStudent))
                .reversed()
                // אחר כך רמת העדיפות המנהלית.
                .thenComparingInt(Student::getPriorityLevel)
                // סטודנטים ותיקים יותר מקבלים קדימות.
                .thenComparing(Comparator.comparingInt(Student::getSeniority).reversed())
                // ממוצע גבוה עוזר לשבור שוויון.
                .thenComparing(Comparator.comparingDouble(Student::getGpa).reversed())
                // לבסוף מזהה הסטודנט נותן סדר דטרמיניסטי קבוע.
                .thenComparingInt(Student::getStudentID);
    }

    /**
     * מחשב כמה בקשות חובה פתוחות נשארו לסטודנט.
     *
     * @param student הסטודנט הנבדק
     * @param mandatoryCoursesByStudent קורסי החובה לכל סטודנט
     * @param requestsByStudent מפת הבקשות לכל סטודנט
     * @return מספר בקשות החובה שנכנסו לרשימת הבקשות של הסטודנט
     */
    private int pendingMandatoryCount(
            Student student,
            Map<Integer, Set<Integer>> mandatoryCoursesByStudent,
            Map<Integer, StudentRequests> requestsByStudent
    ) {
        Set<Integer> mandatory = mandatoryCoursesByStudent.getOrDefault(student.getStudentID(), Set.of());
        if (mandatory.isEmpty()) {
            return 0;
        }
        // בפועל נספרות רק בקשות החובה שנכנסו לרשימת הבקשות החוקיות של הסטודנט.
        return requestsByStudent.getOrDefault(student.getStudentID(), StudentRequests.EMPTY).mandatoryRequestCount();
    }

    /**
     * ציון האיכות של בקשה מסוימת לפי העדפות, חובה ונתוני הסטודנט.
     *
     * @param student הסטודנט שעבורו מחושב הציון
     * @param course הקורס המבוקש
     * @param preferenceRank דירוג ההעדפה המקורי
     * @param mandatory האם הקורס הוא קורס חובה עבור הסטודנט
     * @param weights פרופיל המשקלים של ההרצה
     * @return ציון האיכות של הבקשה
     */
    private double scoreRequest(
            Student student,
            Course course,
            int preferenceRank,
            boolean mandatory,
            WeightProfile weights
    ) {
        // הופך דירוג נמוך (למשל 1) לציון גבוה יותר כך שהעדפה ראשונה תקבל יותר ניקוד.
        int invertedRank = Math.max(1, 6 - preferenceRank);
        // זהו ציון האיכות הכולל של הבקשה, שמשמש בבחירת מהלכי שיפור.
        double score = invertedRank * weights.coursePreferenceWeight();
        // התאמה ליום מועדף מוסיפה ניקוד.
        if (student.prefersDay(course.getDay())) {
            score += weights.dayWeight();
        }
        // התאמה לשעת לימוד מועדפת מוסיפה ניקוד נוסף.
        if (student.prefersCourseTime(course)) {
            score += weights.timeWeight();
        }
        // קורס חובה מקבל בונוס משמעותי כדי שלא יידחק בקלות.
        if (mandatory) {
            score += weights.mandatoryWeight();
        }

        // נתוני הסטודנט עצמם מוסיפים עוד שכבה של עדיפות.
        score += Math.max(0, 5 - student.getPriorityLevel()) * 6.0;
        score += student.getSeniority() * 2.0;
        score += student.getGpa();
        return score;
    }

    /**
     * עדיפות גישה משמשת להכרעת תחרות על מקום בקורס.
     *
     * @param student הסטודנט המתחרה על מקום בקורס
     * @param mandatory האם הבקשה היא בקשת חובה
     * @return ציון עדיפות לתחרות על מקום
     */
    private double accessPriority(Student student, boolean mandatory) {
        // accessPriority נועד להכריע תחרות על מושב, ולכן הוא "חד" יותר מה-score הכללי.
        double score = (4 - student.getPriorityLevel()) * 100.0;
        score += student.getSeniority() * 10.0;
        score += student.getGpa() * 5.0;
        if (mandatory) {
            // תוספת זו גורמת לכך שבקשות חובה יגברו לעיתים קרובות על בקשות רשות.
            score += 75.0;
        }
        return score;
    }

    /**
     * אוסף משקלי הניקוד שמשמשים לחישוב ציון בקשה.
     *
     * @param coursePreferenceWeight משקל דירוג העדפה
     * @param dayWeight משקל התאמה ליום מועדף
     * @param timeWeight משקל התאמה לשעה מועדפת
     * @param mandatoryWeight משקל בקשת חובה
     */
    public record WeightProfile(int coursePreferenceWeight, int dayWeight, int timeWeight, int mandatoryWeight) {
        /**
         * טוען משקלים מהמסד, עם ערכי ברירת מחדל כאשר אין כלל מתאים.
         *
         * @param constraints מפת האילוצים שנטענה מן המסד
         * @return פרופיל משקלים מלא לחישוב ציונים
         */
        private static WeightProfile from(Map<String, ConstraintRule> constraints) {
            return new WeightProfile(
                    // כל אחד מהערכים נטען לפי שם אילוץ קבוע, כדי שיהיה אפשר לכוונן התנהגות בלי לשנות קוד.
                    constraintWeight(constraints, "COURSE_PREFERENCE_RANK", 24),
                    constraintWeight(constraints, "PREFERRED_DAYS", 14),
                    constraintWeight(constraints, "TIME_PREFERENCE", 18),
                    constraintWeight(constraints, "MANDATORY_PRIORITY", 75)
            );
        }

        /**
         * מאפשר לשנות את משקל האילוץ דרך מסד הנתונים בלי לשנות קוד.
         *
         * @param constraints מפת האילוצים שנטענה
         * @param name שם האילוץ המבוקש
         * @param defaultValue ערך ברירת מחדל אם האילוץ לא קיים
         * @return משקל האילוץ בפועל
         */
        private static int constraintWeight(Map<String, ConstraintRule> constraints, String name, int defaultValue) {
            ConstraintRule rule = constraints.get(name);
            return rule == null ? defaultValue : rule.getWeight();
        }
    }

    /**
     * מפתח לוגי עבור מסלול+שנה.
     */
    public record TrackYearKey(String track, int year) {
    }

    /**
     * מעטפת לבקשות של סטודנט יחד עם מידע עזר על בקשות חובה.
     */
    public record StudentRequests(List<RequestChoice> requests, int mandatoryRequestCount) {
        static final StudentRequests EMPTY = new StudentRequests(List.of(), 0);

        /**
         * @return מספר הבקשות הכולל של הסטודנט
         */
        int size() {
            return requests.size();
        }
    }

    /**
     * אובייקט מעבר פנימי שמייצג קורס מבוקש יחד עם כל המידע שהאלגוריתם צריך.
     */
    public record RequestChoice(Course course, int rank, boolean mandatory, double score, double accessPriority) {
    }
}

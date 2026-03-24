package com.example.java_main_proj.service;

import com.example.java_main_proj.model.ConstraintRule;
import com.example.java_main_proj.model.Course;
import com.example.java_main_proj.model.CourseRequirement;
import com.example.java_main_proj.model.EnrollmentDecision;
import com.example.java_main_proj.model.EnrollmentResult;
import com.example.java_main_proj.model.EnrollmentRunReport;
import com.example.java_main_proj.model.Student;
import com.example.java_main_proj.repository.SchedulingDataRepository;
import com.example.java_main_proj.state.AssignmentState;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * מנוע השיבוץ המרכזי של המערכת.
 * האלגוריתם משלב שיבוץ חמדני ראשוני ולאחר מכן חיפוש שיפורים מקומי.
 */
public class HybridEnrollmentService {
    // שלוש תוצאות אפשריות לסטודנט: קיבל הכול, קיבל חלק, או לא קיבל בכלל.
    private static final String FULL_STATUS = "\u05D4\u05E6\u05DC\u05D7\u05D4 \u05DE\u05DC\u05D0\u05D4";
    private static final String PARTIAL_STATUS = "\u05E9\u05D9\u05D1\u05D5\u05E5 \u05D7\u05DC\u05E7\u05D9";

    private final SchedulingDataRepository repository;
    private final SchedulePlanningService planningService;

    /**
     * יוצר מנוע שיבוץ עם רכיבי ברירת המחדל של הפרויקט.
     */
    public HybridEnrollmentService() {
        this(new SchedulingDataRepository());
    }

    /**
     * יוצר מנוע שיבוץ עם שכבת נתונים נתונה.
     *
     * @param repository שכבת הנתונים שתשמש את המנוע
     */
    public HybridEnrollmentService(SchedulingDataRepository repository) {
        this(repository, new SchedulePlanningService());
    }

    /**
     * יוצר מנוע שיבוץ עם שכבת נתונים ושכבת תכנון שסופקו מבחוץ.
     *
     * @param repository שכבת הנתונים שתשמש את המנוע
     * @param planningService שכבת התכנון והכנת הבקשות
     */
    HybridEnrollmentService(SchedulingDataRepository repository, SchedulePlanningService planningService) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.planningService = Objects.requireNonNull(planningService, "planningService");
    }

    /**
     * תרחיש שיבוץ מלא: טעינת נתונים, שיבוץ, שיפור ושמירה למסד.
     *
     * @param academicYear שנת הלימודים של ההרצה
     * @param semester הסמסטר של ההרצה
     * @return דוח ריצה מסכם הכולל נתונים סטטיסטיים ולוג
     */
    public EnrollmentRunReport runEnrollment(String academicYear, String semester) {
        // זו הפונקציה הראשית: טוענת מידע, מריצה שיבוץ, שומרת תוצאות ומחזירה דוח.
        List<String> logLines = new ArrayList<>();
        // טוען את כל כללי האילוצים והמשקלים שמכוונים את דירוג הבקשות.
        Map<String, ConstraintRule> constraints = repository.loadConstraints();
        // טוען את כל הסטודנטים שיש לשבץ עבור ההרצה הנוכחית.
        List<Student> students = repository.loadStudents();
        // טוען רק את הקורסים המוצעים בסמסטר המבוקש.
        List<Course> offeredCourses = repository.loadCourses(semester);
        // מאפס את מונה הנרשמים כדי להתחיל את ההרצה ממצב נקי בזיכרון.
        offeredCourses.forEach(course -> course.setEnrolledStudents(0));
        // יוצר גישה מהירה לקורס לפי מזהה במקום חיפוש חוזר ברשימה.
        Map<Integer, Course> coursesById = offeredCourses.stream()
                .collect(Collectors.toMap(Course::getCourseID, course -> course));
        // טוען את דרישות החובה לפי מסלול ושנה.
        List<CourseRequirement> requirements = repository.loadCourseRequirements();

        // בונה לכל סטודנט את קבוצת קורסי החובה הרלוונטיים אליו.
        Map<Integer, Set<Integer>> mandatoryCoursesByStudent =
                planningService.buildMandatoryCoursesByStudent(students, requirements, coursesById);
        // מאחד לכל סטודנט את כל הבקשות המדורגות, כולל קורסי חובה שנוספו אוטומטית.
        Map<Integer, SchedulePlanningService.StudentRequests> requestsByStudent =
                planningService.buildRequestsByStudent(students, coursesById, mandatoryCoursesByStudent, constraints);
        // סופר רק סטודנטים שבפועל יש להם לפחות בקשה אחת לטיפול.
        int activeStudents = (int) requestsByStudent.values().stream()
                .filter(studentRequests -> !studentRequests.requests().isEmpty())
                .count();

        // מעתיק את הרשימה כדי למיין מבלי לשנות את סדר הטעינה המקורי.
        List<Student> orderedStudents = new ArrayList<>(students);
        // ממיין לפי עדיפות טיפול כך שהאלגוריתם יתחיל מהמקרים החשובים יותר.
        orderedStudents.sort(planningService.studentComparator(mandatoryCoursesByStudent, requestsByStudent));

        // מצב הריצה מרכז את כל האינדקסים הנדרשים לבדיקות מהירות בזמן השיבוץ.
        AssignmentState state = new AssignmentState(offeredCourses);
        // סופר כמה בקשות מדורגות קיימות בסך הכול לצורך מדדים ולוג.
        int requestedCourses = requestsByStudent.values().stream()
                .mapToInt(SchedulePlanningService.StudentRequests::size)
                .sum();

        logLines.add("Guideway sync complete.");
        logLines.add("Loaded " + students.size() + " students.");
        logLines.add("Loaded " + offeredCourses.size() + " courses for " + semester + ".");
        logLines.add("Processing " + requestedCourses + " ranked requests.");

        // שלב ראשון: עובר על הסטודנטים לפי סדר העדיפות ומנסה לשבץ כל בקשה חוקית.
        for (Student student : orderedStudents) {
            assignGreedy(
                    student,
                    requestsByStudent.getOrDefault(student.getStudentID(), SchedulePlanningService.StudentRequests.EMPTY).requests(),
                    state
            );
        }

        // שומר את מצב הבסיס כדי למדוד כמה תרם שלב השיפור המקומי.
        int initialAssignments = state.totalAssignments();
        // שלב שני: מנסה לשפר את התוצאה בעזרת הוספות והזזות מקומיות.
        int improvements = runLocalImprovement(orderedStudents, requestsByStudent, state);
        // זהו מצב השיבוץ הסופי לאחר כל סבבי השיפור.
        int finalAssignments = state.totalAssignments();

        // ממיר את המצב הפנימי לרשומות החלטה המתאימות לשמירה במסד.
        List<EnrollmentDecision> decisions = state.toDecisions(academicYear, semester);
        // מחליף את תוצאות ההרצה הקודמות בתוצאות החדשות של אותה שנה וסמסטר.
        repository.replaceEnrollmentRun(academicYear, semester, decisions);

        // טוען מחדש את התוצאות כפי שהן נשמרו בפועל כדי להפיק סיכום עקבי.
        List<EnrollmentResult> results = repository.loadEnrollmentResults(academicYear, semester);
        int fullAssignments = 0;
        int partialAssignments = 0;
        int unassignedStudents = 0;
        for (EnrollmentResult result : results) {
            // מפרק את התוצאה לקטגוריות שהדשבורד והדוח משתמשים בהן.
            if (FULL_STATUS.equals(result.getStatus())) {
                fullAssignments++;
            } else if (PARTIAL_STATUS.equals(result.getStatus())) {
                partialAssignments++;
            } else {
                unassignedStudents++;
            }
        }

        logLines.add("Greedy pass assigned " + initialAssignments + " courses.");
        logLines.add("Local improvement accepted " + improvements + " changes.");
        logLines.add("Final assignment count: " + finalAssignments + ".");
        logLines.add("Full schedules: " + fullAssignments + ", partial: " + partialAssignments + ", unassigned: " + unassignedStudents + ".");

        return new EnrollmentRunReport(
                academicYear,
                semester,
                activeStudents,
                requestedCourses,
                finalAssignments,
                improvements,
                fullAssignments,
                partialAssignments,
                unassignedStudents,
                logLines
        );
    }

    /**
     * מעבר חמדני ראשון: בוחר קורסים חוקיים לפי הסדר המדורג של הבקשות.
     *
     * @param student הסטודנט המטופל
     * @param requests רשימת הבקשות של הסטודנט
     * @param state מצב הריצה הנוכחי
     */
    private void assignGreedy(
            Student student,
            List<SchedulePlanningService.RequestChoice> requests,
            AssignmentState state
    ) {
        // כאן מתחיל השיבוץ הראשוני: עוברים על הבקשות לפי הסדר ומכניסים מה שאפשר.
        for (SchedulePlanningService.RequestChoice request : requests) {
            // לא מבצעים שיבוץ כפול של אותו סטודנט לאותו קורס.
            if (!state.isAssigned(student.getStudentID(), request.course().getCourseID())
                    // בדיקת החוקיות כוללת מקום פנוי, מגבלת חובה והתנגשות זמנים.
                    && isFeasible(student, request, state)) {
                // אם הבקשה חוקית בשלב זה, החמדן מקבל אותה מיד.
                state.assign(student, request);
            }
        }
    }

    /**
     * שלב שיפור מקומי עם מספר קטן של מעברים כדי לשפר את תוצאת השיבוץ.
     *
     * @param students רשימת הסטודנטים לפי סדר הטיפול
     * @param requestsByStudent מפת הבקשות לכל סטודנט
     * @param state מצב הריצה הנוכחי
     * @return מספר השיפורים שאושרו בפועל
     */
    private int runLocalImprovement(
            List<Student> students,
            Map<Integer, SchedulePlanningService.StudentRequests> requestsByStudent,
            AssignmentState state
    ) {
        // אחרי השיבוץ הראשוני מנסים לשפר אותו עוד קצת.
        int improvements = 0;
        boolean changed;
        int pass = 0;

        do {
            // בכל מעבר חדש נניח תחילה שלא בוצע שיפור.
            changed = false;
            // מונה המעברים מגביל את זמן הריצה של השיפור המקומי.
            pass++;

            for (Student student : students) {
                for (SchedulePlanningService.RequestChoice request
                        : requestsByStudent.getOrDefault(student.getStudentID(), SchedulePlanningService.StudentRequests.EMPTY).requests()) {
                    // אין טעם לטפל שוב בבקשה שכבר שובצה.
                    if (!state.isAssigned(student.getStudentID(), request.course().getCourseID())) {
                        // שיפור מסוג ראשון: הוספה ישירה של בקשה שלא שובצה אם יש מקום והיא חוקית.
                        if (state.hasSeat(request.course().getCourseID()) && isFeasible(student, request, state)) {
                            state.assign(student, request);
                            improvements++;
                            changed = true;
                        // שיפור מסוג שני: ניסיון לפנות מקום בעזרת הזזה של סטודנט אחר.
                        } else if (tryDisplacement(student, request, requestsByStudent, state)) {
                            improvements++;
                            changed = true;
                        }
                    }
                }
            }
        } while (changed && pass < 3);

        return improvements;
    }

    /**
     * מנסה לפנות מקום לסטודנט מבקש על ידי הזזת סטודנט קיים לקורס חלופי.
     *
     * @param requester הסטודנט שמבקש את הקורס
     * @param requestedCourse הבקשה הרצויה שלא שובצה
     * @param requestsByStudent מפת הבקשות לכל סטודנט
     * @param state מצב הריצה הנוכחי
     * @return {@code true} אם מהלך ההזזה הצליח
     */
    private boolean tryDisplacement(
            Student requester,
            SchedulePlanningService.RequestChoice requestedCourse,
            Map<Integer, SchedulePlanningService.StudentRequests> requestsByStudent,
            AssignmentState state
    ) {
        // כל הסטודנטים שכבר יושבים בקורס המבוקש הם מועמדים פוטנציאליים להזזה.
        List<AssignmentState.AssignmentChoice> currentAssignees =
                new ArrayList<>(state.assignmentsForCourse(requestedCourse.course().getCourseID()));
        // בוחרים קודם בעלי עדיפות גישה נמוכה יותר, כי קל יותר להצדיק החלפה שלהם.
        currentAssignees.sort(Comparator
                .comparingDouble(AssignmentState.AssignmentChoice::accessPriority)
                .thenComparingInt(choice -> choice.student().getStudentID()));

        for (AssignmentState.AssignmentChoice assignee : currentAssignees) {
            // מבצעים דחיקה רק אם למבקש החדש יש עדיפות גישה גבוהה יותר.
            if (requestedCourse.accessPriority() > assignee.accessPriority()) {
                // מנסים למצוא למסולק קורס חלופי חוקי כדי לא לפגוע בו סתם.
                SchedulePlanningService.RequestChoice alternative = findBestAlternativeRequest(
                        assignee.student(),
                        requestsByStudent.getOrDefault(
                                assignee.student().getStudentID(),
                                SchedulePlanningService.StudentRequests.EMPTY
                        ).requests(),
                        state,
                        // הקורס המבוקש מוחרג כדי שלא "נמצא" בדיוק את הקורס שממנו דחקנו אותו.
                        requestedCourse.course().getCourseID()
                );
                if (alternative != null) {
                    // שיבוץ חובה עדיף על פני שיבוץ לא-חובה גם אם הציון הכולל לא השתפר.
                    boolean mandatoryUpgrade = requestedCourse.mandatory() && !assignee.mandatory();
                    // אחרת בודקים אם צירוף שני השיבוצים החדשים נותן רווח כולל גבוה יותר.
                    boolean betterTotalScore = requestedCourse.score() + alternative.score() > assignee.score();
                    if (mandatoryUpgrade || betterTotalScore) {
                        // מפנים את המקום בקורס המבוקש.
                        state.unassign(assignee.student().getStudentID(), requestedCourse.course().getCourseID());
                        // מעבירים את הסטודנט הישן לאלטרנטיבה שמצאנו.
                        state.assign(assignee.student(), alternative);
                        // רק עכשיו בודקים אם המבקש החדש אכן יכול להיכנס בלי ליצור התנגשות.
                        if (isFeasible(requester, requestedCourse, state)) {
                            state.assign(requester, requestedCourse);
                            return true;
                        }

                        // אם המהלך נכשל, מחזירים את המצב בדיוק לקדמותו.
                        state.unassign(assignee.student().getStudentID(), alternative.course().getCourseID());
                        state.assign(assignee.student(), assignee.request());
                    }
                }
            }
        }

        return false;
    }

    /**
     * מחפש קורס חלופי חוקי שניתן להעביר אליו סטודנט קיים.
     */
    // מחפשת לסטודנט קורס אחר שאפשר להעביר אותו אליו בלי לשבור את הכללים.
    /**
     * מחפש לסטודנט חלופה טובה וחוקית לקורס שממנו מנסים להזיז אותו.
     *
     * @param student הסטודנט שעבורו מחפשים חלופה
     * @param requests רשימת הבקשות שלו
     * @param state מצב הריצה הנוכחי
     * @param excludedCourseId מזהה קורס שיש להחריג מן החיפוש
     * @return בקשת החלופה שנבחרה, או {@code null} אם לא נמצאה חלופה מתאימה
     */
    private SchedulePlanningService.RequestChoice findBestAlternativeRequest(
            Student student,
            List<SchedulePlanningService.RequestChoice> requests,
            AssignmentState state,
            int excludedCourseId
    ) {
        for (SchedulePlanningService.RequestChoice request : requests) {
            if (request.course().getCourseID() != excludedCourseId
                    // לא בוחרים קורס שכבר שובץ לאותו סטודנט.
                    && !state.isAssigned(student.getStudentID(), request.course().getCourseID())
                    // האלטרנטיבה חייבת להיות עם מקום פנוי.
                    && state.hasSeat(request.course().getCourseID())
                    // וגם לעמוד בכל בדיקות החוקיות הרגילות.
                    && isFeasible(student, request, state)) {
                // הבקשות כבר ממוינות, לכן הבקשה החוקית הראשונה היא גם הטובה ביותר לפי הסדר שנקבע.
                return request;
            }
        }
        return null;
    }

    /**
     * בודק האם השיבוץ המבוקש שומר על קיבולת, מגבלות חובה והיעדר חפיפה.
     */
    // בדיקה בסיסית: יש מקום? אין חפיפה? לא עברנו את מגבלת החובה?
    /**
     * בודק אם בקשה מסוימת חוקית במצב הריצה הנוכחי.
     *
     * @param student הסטודנט שעבורו נבדקת הבקשה
     * @param request הבקשה הנבדקת
     * @param state מצב הריצה הנוכחי
     * @return {@code true} אם הבקשה עומדת בכל האילוצים הקשים
     */
    private boolean isFeasible(Student student, SchedulePlanningService.RequestChoice request, AssignmentState state) {
        Course course = request.course();
        // בלי מקום פנוי אין טעם להמשיך לבדוק תנאים נוספים.
        if (!state.hasSeat(course.getCourseID())) {
            return false;
        }

        // מונע חריגה מהמכסה האישית של קורסי חובה.
        if (request.mandatory() && state.mandatoryAssignmentCount(student.getStudentID()) >= student.getMaxMandatoryCourses()) {
            return false;
        }

        // סורק רק את השיבוצים של אותו יום, ולכן בדיקת החפיפה יעילה יחסית.
        for (AssignmentState.AssignmentChoice assignment : state.assignmentsForStudentOnDay(student.getStudentID(), course.getDay())) {
            if (assignment.course().overlapsWith(course)) {
                return false;
            }
        }

        return true;
    }
}

package com.example.java_main_proj.repository;

import com.example.java_main_proj.model.ConstraintRule;
import com.example.java_main_proj.model.Course;
import com.example.java_main_proj.model.CourseRequirement;
import com.example.java_main_proj.model.EnrollmentDecision;
import com.example.java_main_proj.model.EnrollmentResult;
import com.example.java_main_proj.model.Student;
import java.util.List;
import java.util.Map;

/**
 * Facade מעל שכבות ה-repository הפנימיות.
 * נותן לשכבות העליונות נקודת גישה אחת לכל נתוני המערכת.
 */
public class SchedulingDataRepository {
    private final DatabaseCatalogReader catalogReader;
    private final EnrollmentRunRepository enrollmentRunRepository;
    private final EnrollmentResultsRepository enrollmentResultsRepository;

    /**
     * יוצר Repository ראשי עם ברירות המחדל של שכבת הנתונים.
     */
    public SchedulingDataRepository() {
        // יוצר ברירת מחדל של רכיבי הקריאה והכתיבה למסד.
        this(new DatabaseCatalogReader(), new EnrollmentRunRepository());
    }

    /**
     * יוצר Repository ראשי עם רכיבי נתונים שסופקו מבחוץ.
     *
     * @param catalogReader קורא הקטלוג של נתוני הבסיס
     * @param enrollmentRunRepository רכיב שמירת ההרצות
     */
    SchedulingDataRepository(DatabaseCatalogReader catalogReader, EnrollmentRunRepository enrollmentRunRepository) {
        // המחלקה הזו משמשת facade ולכן מחזיקה את תתי-המחלקות המתמחות.
        this.catalogReader = catalogReader;
        this.enrollmentRunRepository = enrollmentRunRepository;
        this.enrollmentResultsRepository = new EnrollmentResultsRepository(catalogReader);
    }

    /**
     * טוען סטודנטים כולל ההעדפות שלהם.
     *
     * @return רשימת הסטודנטים מן המסד
     */
    public List<Student> loadStudents() {
        return catalogReader.loadStudents();
    }

    /**
     * טוען קורסים לפי סמסטר.
     *
     * @param semester הסמסטר המבוקש
     * @return רשימת הקורסים של הסמסטר
     */
    public List<Course> loadCourses(String semester) {
        return catalogReader.loadCourses(semester);
    }

    /**
     * טוען חוקי חובה של קורסים לפי מסלול ושנה.
     *
     * @return רשימת חוקי החובה
     */
    public List<CourseRequirement> loadCourseRequirements() {
        return catalogReader.loadCourseRequirements();
    }

    /**
     * טוען את הגדרות האילוצים והמשקלים.
     *
     * @return מפת אילוצים לפי שם האילוץ
     */
    public Map<String, ConstraintRule> loadConstraints() {
        return catalogReader.loadConstraints();
    }

    /**
     * שומר למסד את תוצאות ריצת השיבוץ.
     *
     * @param academicYear שנת הלימודים של ההרצה
     * @param semester הסמסטר של ההרצה
     * @param decisions החלטות השיבוץ לשמירה
     */
    public void replaceEnrollmentRun(String academicYear, String semester, List<EnrollmentDecision> decisions) {
        enrollmentRunRepository.replaceEnrollmentRun(academicYear, semester, decisions);
    }

    /**
     * טוען תוצאות מוכנות לתצוגה בממשק.
     *
     * @param academicYear שנת הלימודים הרצויה
     * @param semester הסמסטר הרצוי
     * @return רשימת תוצאות מעובדות להצגה
     */
    public List<EnrollmentResult> loadEnrollmentResults(String academicYear, String semester) {
        // תצוגת התוצאות דורשת גם את חוקי החובה כדי לחשב כמה הסטודנט ביקש בפועל.
        return enrollmentResultsRepository.loadEnrollmentResults(academicYear, semester, loadCourseRequirements());
    }

    /**
     * טוען שנות לימוד שקיימות עבורן תוצאות שמורות.
     *
     * @return רשימת שנות לימוד זמינות
     */
    public List<String> loadAcademicYearsWithResults() {
        return enrollmentRunRepository.loadAcademicYearsWithResults();
    }

    /**
     * טוען את הסמסטרים הזמינים להצגת תוצאות.
     *
     * @return רשימת סמסטרים זמינים
     */
    public List<String> loadSemestersWithResults() {
        return enrollmentRunRepository.loadSemestersWithResults();
    }
}

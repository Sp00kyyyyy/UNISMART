package com.example.java_main_proj.controller;

import com.example.java_main_proj.model.EnrollmentRunReport;
import com.example.java_main_proj.service.HybridEnrollmentService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

/**
 * בקר מסך ההרצה של האלגוריתם.
 * אחראי לאסוף פרמטרים מהמשתמש, להריץ את האלגוריתם ברקע ולהציג דוח ריצה.
 */
public class EnrollmentController {
    private static final String SEMESTER_A = "סמסטר א'";
    private static final String SEMESTER_B = "סמסטר ב'";

    @FXML private ComboBox<String> yearComboBox;
    @FXML private ComboBox<String> semesterComboBox;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;
    @FXML private TextArea logArea;
    @FXML private Label statusLabel;

    private final HybridEnrollmentService hybridEnrollmentService = new HybridEnrollmentService();

    /**
     * יוצר בקר למסך הרצת השיבוץ.
     */
    public EnrollmentController() {
    }

    /**
     * מאתחל את מסך ההרצה ומגדיר את ערכי ברירת המחדל.
     */
    @FXML
    public void initialize() {
        // מכין את תיבות הבחירה כך שהמסך יעלה עם ערכי ברירת מחדל תקינים.
        setupComboBoxes();
    }

    /**
     * קובע ערכי ברירת מחדל לשנה ולסמסטר לפני ההרצה.
     */
    private void setupComboBoxes() {
        // השנים והסמסטרים ידועים מראש ולכן מוגדרים ישירות בממשק.
        yearComboBox.getItems().addAll("2025-2026", "2026-2027", "2027-2028");
        semesterComboBox.getItems().addAll(SEMESTER_A, SEMESTER_B);

        // מונע מצב שבו המסך נפתח ללא בחירה התחלתית.
        yearComboBox.setValue("2025-2026");
        semesterComboBox.setValue(SEMESTER_A);
    }

    /**
     * מפעיל את האלגוריתם בתוך Task נפרד כדי לא לחסום את ה-UI.
     */
    @FXML
    private void startEnrollment() {
        // לא מתחילים הרצה בלי שנת לימודים וסמסטר תקינים.
        if (yearComboBox.getValue() == null || semesterComboBox.getValue() == null) {
            showAlert("שגיאה", "יש לבחור שנת לימודים וסמסטר לפני ההרצה.");
            return;
        }

        String academicYear = yearComboBox.getValue();
        String semester = semesterComboBox.getValue();
        // מאפסים את תצוגת ההתקדמות לקראת ריצה חדשה.
        logArea.clear();
        progressBar.setProgress(-1);
        progressLabel.setText("מריץ...");
        statusLabel.setText("מריץ שיבוץ היברידי לפי ההנחיות...");

        // Task מריץ את האלגוריתם מחוץ ל-thread של JavaFX.
        Task<EnrollmentRunReport> task = new Task<>() {
            /**
             * מפעיל את אלגוריתם השיבוץ על גבי ה-thread של המשימה.
             *
             * מריץ את אלגוריתם השיבוץ על גבי ה-thread של המשימה.
             *
             * @return דוח הריצה שהופק על ידי האלגוריתם
             */
            @Override
            protected EnrollmentRunReport call() {
                return hybridEnrollmentService.runEnrollment(academicYear, semester);
            }
        };

        task.setOnSucceeded(event -> {
            // במקרה הצלחה מציגים למשתמש את דוח הריצה שנוצר.
            EnrollmentRunReport report = task.getValue();
            progressBar.setProgress(1);
            progressLabel.setText("100%");
            for (String line : report.getLogLines()) {
                // כל שורה מייצגת שלב או סיכום שנאסף במהלך ההרצה.
                logArea.appendText(line + System.lineSeparator());
            }
            statusLabel.setText("השיבוץ הושלם עבור " + report.getSemester() + " (" + report.getAcademicYear() + ").");
        });

        task.setOnFailed(event -> {
            // במקרה כשל מחזירים את המסך למצב ברור למשתמש.
            progressBar.setProgress(0);
            progressLabel.setText("0%");
            statusLabel.setText("השיבוץ נכשל.");
            Throwable error = task.getException();
            logArea.appendText("שגיאה: " + (error == null ? "Unknown error" : error.getMessage()) + System.lineSeparator());
        });

        // ה-thread מוגדר כ-daemon כדי שלא יחסום סגירת יישום.
        Thread worker = new Thread(task, "hybrid-enrollment-task");
        worker.setDaemon(true);
        worker.start();
    }

    /**
     * מנקה את יומן ההרצה ומחזיר את המסך למצב מוכן להרצה נוספת.
     */
    @FXML
    private void clearLog() {
        // מאפס את יומן ההרצה ואת חיווי ההתקדמות.
        logArea.clear();
        progressBar.setProgress(0);
        progressLabel.setText("0%");
        statusLabel.setText("מוכן להרצת שיבוץ.");
    }

    /**
     * סוגר את חלון הרצת השיבוץ.
     */
    @FXML
    private void closeWindow() {
        // סוגר רק את חלון ההרצה.
        Stage stage = (Stage) logArea.getScene().getWindow();
        stage.close();
    }

    /**
     * מציג שגיאות קלט למשתמש.
     *
     * @param title כותרת ההודעה
     * @param content תוכן ההודעה
     */
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

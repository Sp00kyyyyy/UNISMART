package com.example.java_main_proj.controller;

import com.example.java_main_proj.db.DatabaseConnection;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;

/**
 * הבקר של המסך הראשי.
 * תפקידו לנווט בין מסכי המערכת ולהציג סטטוס בסיסי של חיבור למסד.
 */
public class MainDashboardController {

    @FXML
    private Label statusLabel;

    /**
     * מאתחל את מסך הבית ובודק את מצב החיבור למסד הנתונים.
     */
    @FXML
    public void initialize() {
        // בפתיחת המסך הראשי בודקים מיד אם מסד הנתונים זמין.
        checkDatabaseConnection();
    }

    /**
     * מעדכן במסך הראשי האם מסד הנתונים זמין כרגע.
     */
    private void checkDatabaseConnection() {
        // מעדכן את תווית הסטטוס הראשית לפי מצב החיבור למסד.
        if (DatabaseConnection.testConnection()) {
            statusLabel.setText("מצב: מחובר למסד הנתונים");
            statusLabel.setStyle("-fx-text-fill: green; -fx-font-size: 14px;");
        } else {
            statusLabel.setText("מצב: שגיאה בחיבור למסד הנתונים");
            statusLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px;");
        }
    }

    /**
     * פותח את חלון ניהול הסטודנטים.
     */
    @FXML
    private void showStudents() {
        try {
            // טוען את מסך הסטודנטים מתוך קובץ ה-FXML המתאים.
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/java_main_proj/student-view.fxml"));
            Parent root = loader.load();

            // פותח חלון חדש כדי לא להחליף את הדשבורד הראשי.
            Stage stage = new Stage();
            stage.setTitle("ניהול סטודנטים");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception exception) {
            showAlert("שגיאה", "לא ניתן לפתוח את חלון ניהול הסטודנטים: " + exception.getMessage());
            exception.printStackTrace();
        }
    }

    /**
     * פותח את חלון ניהול הקורסים.
     */
    @FXML
    private void showCourses() {
        try {
            // טוען את מסך הקורסים ומציג אותו בחלון נפרד.
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/java_main_proj/course-view.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("ניהול קורסים");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception exception) {
            showAlert("שגיאה", "לא ניתן לפתוח את חלון ניהול הקורסים: " + exception.getMessage());
            exception.printStackTrace();
        }
    }

    /**
     * פותח את חלון הרצת השיבוץ.
     */
    @FXML
    private void showEnrollment() {
        try {
            // מסך זה מיועד להפעלת האלגוריתם בפועל.
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/java_main_proj/enrollment-view.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("ביצוע שיבוץ");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception exception) {
            showAlert("שגיאה", "לא ניתן לפתוח את חלון ביצוע השיבוץ: " + exception.getMessage());
            exception.printStackTrace();
        }
    }

    /**
     * פותח את חלון התוצאות.
     */
    @FXML
    private void showResults() {
        try {
            // מסך התוצאות מציג את פלט ההרצות שכבר נשמר במסד.
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/java_main_proj/results-view.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("הצגת תוצאות");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception exception) {
            showAlert("שגיאה", "לא ניתן לפתוח את חלון התוצאות: " + exception.getMessage());
            exception.printStackTrace();
        }
    }

    /**
     * מציג חלון אודות קצר על המערכת.
     */
    @FXML
    private void showAbout() {
        showAlert("אודות המערכת",
                "UniSmart - מערכת שיבוץ סטודנטים\n" +
                        "פותח על ידי: יהונתן רפאלי\n" +
                        "גרסה: 1.0");
    }

    /**
     * סוגר את היישום ומנתק את החיבור למסד הנתונים.
     */
    @FXML
    private void handleExit() {
        // סוגר קודם את החיבור למסד ורק אחר כך מסיים את היישום.
        DatabaseConnection.closeConnection();
        System.exit(0);
    }

    /**
     * תבנית אחידה להצגת הודעות למשתמש.
     *
     * @param title כותרת החלון
     * @param content תוכן ההודעה
     */
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

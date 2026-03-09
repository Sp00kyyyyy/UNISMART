package com.example.java_main_proj;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class HelloController {

    @FXML
    private Label statusLabel;

    @FXML
    public void initialize() {
        checkDatabaseConnection();
    }

    private void checkDatabaseConnection() {
        if (DatabaseConnection.testConnection()) {
            statusLabel.setText("מצב: מחובר למסד הנתונים");
            statusLabel.setStyle("-fx-text-fill: green; -fx-font-size: 14px;");
        } else {
            statusLabel.setText("מצב: שגיאה בחיבור למסד הנתונים");
            statusLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px;");
        }
    }

    @FXML
    private void showStudents() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/java_main_proj/student-view.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("ניהול סטודנטים");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception exception) {
            showAlert("שגיאה", "לא ניתן לפתוח את חלון ניהול הסטודנטים: " + exception.getMessage());
            exception.printStackTrace();
        }
    }

    @FXML
    private void showCourses() {
        try {
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

    @FXML
    private void showEnrollment() {
        try {
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

    @FXML
    private void showResults() {
        try {
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

    @FXML
    private void showAbout() {
        showAlert("אודות המערכת",
                "UniSmart - מערכת שיבוץ סטודנטים\n" +
                        "פותח על ידי: יהונתן רפאלי\n" +
                        "גרסה: 1.0");
    }

    @FXML
    private void handleExit() {
        DatabaseConnection.closeConnection();
        System.exit(0);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

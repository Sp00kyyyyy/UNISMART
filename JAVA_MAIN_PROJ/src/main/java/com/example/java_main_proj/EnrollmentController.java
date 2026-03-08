package com.example.java_main_proj;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class EnrollmentController {

    @FXML private ComboBox<String> yearComboBox;
    @FXML private ComboBox<String> semesterComboBox;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;
    @FXML private TextArea logArea;
    @FXML private Label statusLabel;

    private final HybridEnrollmentService hybridEnrollmentService = new HybridEnrollmentService();

    @FXML
    public void initialize() {
        setupComboBoxes();
    }

    private void setupComboBoxes() {
        yearComboBox.getItems().addAll("2025-2026", "2026-2027", "2027-2028");
        semesterComboBox.getItems().addAll("סמסטר א'", "סמסטר ב'", "סמסטר קיץ");

        yearComboBox.setValue("2025-2026");
        semesterComboBox.setValue("סמסטר א'");
    }

    @FXML
    private void startEnrollment() {
        if (yearComboBox.getValue() == null || semesterComboBox.getValue() == null) {
            showAlert("שגיאה", "יש לבחור שנת לימודים וסמסטר לפני ההרצה.");
            return;
        }

        String academicYear = yearComboBox.getValue();
        String semester = semesterComboBox.getValue();
        logArea.clear();
        progressBar.setProgress(-1);
        progressLabel.setText("מריץ...");
        statusLabel.setText("מריץ שיבוץ היברידי לפי ההנחיות...");

        Task<EnrollmentRunReport> task = new Task<>() {
            @Override
            protected EnrollmentRunReport call() {
                return hybridEnrollmentService.runEnrollment(academicYear, semester);
            }
        };

        task.setOnSucceeded(event -> {
            EnrollmentRunReport report = task.getValue();
            progressBar.setProgress(1);
            progressLabel.setText("100%");
            for (String line : report.getLogLines()) {
                logArea.appendText(line + System.lineSeparator());
            }
            statusLabel.setText("השיבוץ הושלם עבור " + report.getSemester() + " (" + report.getAcademicYear() + ").");
        });

        task.setOnFailed(event -> {
            progressBar.setProgress(0);
            progressLabel.setText("0%");
            statusLabel.setText("השיבוץ נכשל.");
            Throwable error = task.getException();
            logArea.appendText("שגיאה: " + (error == null ? "Unknown error" : error.getMessage()) + System.lineSeparator());
        });

        Thread worker = new Thread(task, "hybrid-enrollment-task");
        worker.setDaemon(true);
        worker.start();
    }

    @FXML
    private void stopEnrollment() {
        logArea.appendText("העצירה הידנית זמינה למסך בלבד. להרצה חדשה בחרו נתונים והריצו שוב." + System.lineSeparator());
        progressBar.setProgress(0);
        progressLabel.setText("0%");
        statusLabel.setText("המסך אופס.");
    }

    @FXML
    private void clearLog() {
        logArea.clear();
        progressBar.setProgress(0);
        progressLabel.setText("0%");
        statusLabel.setText("מוכן להרצת שיבוץ.");
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) logArea.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

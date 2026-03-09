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
            statusLabel.setText("םינותנה דסמל רבוחמ :בצמ");
            statusLabel.setStyle("-fx-text-fill: green; -fx-font-size: 14px;");
        } else {
            statusLabel.setText("םינותנה דסמל רוביחב האיגש :בצמ");
            statusLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px;");
        }
    }

    @FXML
    private void showStudents() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/java_main_proj/student-view.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("םיטנדוטס לוהינ");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception exception) {
            showAlert("האיגש", " :םיטנדוטסה לוהינ ןולח תא חותפל ןתינ אל" + exception.getMessage());
            exception.printStackTrace();
        }
    }

    @FXML
    private void showCourses() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/java_main_proj/course-view.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("םיסרוק לוהינ");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception exception) {
            showAlert("האיגש", " :םיסרוקה לוהינ ןולח תא חותפל ןתינ אל" + exception.getMessage());
            exception.printStackTrace();
        }
    }

    @FXML
    private void showEnrollment() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/java_main_proj/enrollment-view.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("ץוביש עוציב");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception exception) {
            showAlert("האיגש", " :ץובישה עוציב ןולח תא חותפל ןתינ אל" + exception.getMessage());
            exception.printStackTrace();
        }
    }

    @FXML
    private void showResults() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/java_main_proj/results-view.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("תואצות תגצה");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (Exception exception) {
            showAlert("האיגש", " :תואצותה ןולח תא חותפל ןתינ אל" + exception.getMessage());
            exception.printStackTrace();
        }
    }

    @FXML
    private void showAbout() {
        showAlert("תכרעמה תודוא",
                "UniSmart - םיטנדוטס ץוביש תכרעמ\n" +
                        "ילאפר ןתנוהי :ידי לע חתופ\n" +
                        "0.1 :הסרג");
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

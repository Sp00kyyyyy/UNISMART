package com.example.java_main_proj;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.util.List;

public class ResultsController {
    private static final String FULL_STATUS = "הצלחה מלאה";
    private static final String PARTIAL_STATUS = "שיבוץ חלקי";
    private static final String EMPTY_STATUS = "ללא שיבוץ";

    @FXML private ComboBox<String> yearFilterCombo;
    @FXML private ComboBox<String> semesterFilterCombo;
    @FXML private TableView<EnrollmentResult> resultsTable;
    @FXML private TableColumn<EnrollmentResult, String> studentIdCol;
    @FXML private TableColumn<EnrollmentResult, String> studentNameCol;
    @FXML private TableColumn<EnrollmentResult, String> yearCol;
    @FXML private TableColumn<EnrollmentResult, Integer> requestedCol;
    @FXML private TableColumn<EnrollmentResult, Integer> enrolledCol;
    @FXML private TableColumn<EnrollmentResult, String> statusCol;
    @FXML private TableColumn<EnrollmentResult, String> coursesListCol;
    @FXML private Label totalStudentsLabel;
    @FXML private Label successLabel;
    @FXML private Label partialLabel;
    @FXML private Label statusLabel;

    private final ObservableList<EnrollmentResult> allResults = FXCollections.observableArrayList();
    private final GuidewayRepository repository = new GuidewayRepository();

    @FXML
    public void initialize() {
        setupTable();
        setupFilters();
        refreshResults();
    }

    private void setupTable() {
        studentIdCol.setCellValueFactory(new PropertyValueFactory<>("studentId"));
        studentNameCol.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        yearCol.setCellValueFactory(new PropertyValueFactory<>("year"));
        requestedCol.setCellValueFactory(new PropertyValueFactory<>("requestedCourses"));
        enrolledCol.setCellValueFactory(new PropertyValueFactory<>("enrolledCourses"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        coursesListCol.setCellValueFactory(new PropertyValueFactory<>("coursesList"));

        statusCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                setText(item);
                if (item.equals(FULL_STATUS)) {
                    setStyle("-fx-text-fill: #388E3C; -fx-font-weight: bold;");
                } else if (item.equals(PARTIAL_STATUS)) {
                    setStyle("-fx-text-fill: #F57C00; -fx-font-weight: bold;");
                } else if (item.equals(EMPTY_STATUS)) {
                    setStyle("-fx-text-fill: #C62828; -fx-font-weight: bold;");
                } else {
                    setStyle("");
                }
            }
        });
    }

    private void setupFilters() {
        yearFilterCombo.getItems().setAll(repository.loadAcademicYearsWithResults());
        semesterFilterCombo.getItems().setAll(repository.loadSemestersWithResults());
        if (!yearFilterCombo.getItems().isEmpty()) {
            yearFilterCombo.setValue(yearFilterCombo.getItems().get(0));
        }
        if (!semesterFilterCombo.getItems().isEmpty()) {
            semesterFilterCombo.setValue(semesterFilterCombo.getItems().get(0));
        }
    }

    private void updateStatistics() {
        totalStudentsLabel.setText(String.valueOf(allResults.size()));

        long success = allResults.stream().filter(result -> result.getStatus().equals(FULL_STATUS)).count();
        long partial = allResults.stream().filter(result -> result.getStatus().equals(PARTIAL_STATUS)).count();

        successLabel.setText(String.valueOf(success));
        partialLabel.setText(String.valueOf(partial));
    }

    @FXML
    private void applyFilter() {
        refreshResults();
    }

    @FXML
    private void clearFilter() {
        yearFilterCombo.setValue(null);
        semesterFilterCombo.setValue(null);
        allResults.clear();
        resultsTable.setItems(allResults);
        updateStatistics();
        statusLabel.setText("המסננים נוקו.");
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) resultsTable.getScene().getWindow();
        stage.close();
    }

    private void refreshResults() {
        String academicYear = yearFilterCombo.getValue();
        String semester = semesterFilterCombo.getValue();
        if (academicYear == null || semester == null) {
            allResults.clear();
            resultsTable.setItems(allResults);
            updateStatistics();
            statusLabel.setText("יש לבחור שנת לימודים וסמסטר להצגת תוצאות.");
            return;
        }

        List<EnrollmentResult> results = repository.loadEnrollmentResults(academicYear, semester);
        allResults.setAll(results);
        resultsTable.setItems(allResults);
        updateStatistics();
        statusLabel.setText("הוצגו " + results.size() + " תוצאות עבור " + semester + " (" + academicYear + ").");
    }
}

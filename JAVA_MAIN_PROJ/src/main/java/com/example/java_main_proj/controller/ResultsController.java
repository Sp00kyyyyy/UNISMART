package com.example.java_main_proj.controller;

import com.example.java_main_proj.model.EnrollmentResult;
import com.example.java_main_proj.repository.SchedulingDataRepository;
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

/**
 * בקר מסך התוצאות.
 * מציג את פלט ריצות השיבוץ ומרכז נתונים סטטיסטיים לסיכום מצב ההרשמות.
 */
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
    private final SchedulingDataRepository repository = new SchedulingDataRepository();

    @FXML
    public void initialize() {
        // מאתחל את מבנה הטבלה, המסננים והנתונים ההתחלתיים.
        setupTable();
        setupFilters();
        refreshResults();
    }

    /**
     * מגדיר את עמודות הטבלה ואת הצביעה של עמודת הסטטוס.
     */
    private void setupTable() {
        // מחבר בין עמודות הטבלה לשדות של מודל התוצאה.
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
                    // איפוס תא ריק מונע זליגת טקסט ועיצוב בין שורות.
                    setText(null);
                    setStyle("");
                    return;
                }

                setText(item);
                // צבע שונה לכל סטטוס משפר קריאות מיידית של מצב השיבוץ.
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

    /**
     * טוען אפשרויות סינון ומחבר אותן לרענון אוטומטי של המסך.
     */
    private void setupFilters() {
        // טוען את הערכים האפשריים לפילטרים מתוך הרצות קיימות במסד.
        yearFilterCombo.getItems().setAll(repository.loadAcademicYearsWithResults());
        semesterFilterCombo.getItems().setAll(repository.loadSemestersWithResults());
        if (!yearFilterCombo.getItems().isEmpty()) {
            yearFilterCombo.setValue(yearFilterCombo.getItems().get(0));
        }
        if (!semesterFilterCombo.getItems().isEmpty()) {
            semesterFilterCombo.setValue(semesterFilterCombo.getItems().get(0));
        }

        // כל שינוי בפילטרים מפעיל רענון אוטומטי של הדוח.
        yearFilterCombo.setOnAction(event -> refreshResults());
        semesterFilterCombo.setOnAction(event -> refreshResults());
    }

    /**
     * מחשב נתוני סיכום שמוצגים מעל הטבלה.
     */
    private void updateStatistics() {
        // מספר הסטודנטים שמוצגים כרגע בטבלה.
        totalStudentsLabel.setText(String.valueOf(allResults.size()));

        // סיכום של הצלחה מלאה וחלקית מתוך הרשימה הנוכחית.
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
        // איפוס מסננים מנקה גם את תוצאות הטבלה.
        yearFilterCombo.setValue(null);
        semesterFilterCombo.setValue(null);
        allResults.clear();
        resultsTable.setItems(allResults);
        updateStatistics();
        statusLabel.setText("המסננים נוקו.");
    }

    @FXML
    private void closeWindow() {
        // סוגר את חלון התוצאות בלבד.
        Stage stage = (Stage) resultsTable.getScene().getWindow();
        stage.close();
    }

    /**
     * טוען את התוצאות הרלוונטיות למסננים הנוכחיים.
     */
    private void refreshResults() {
        String academicYear = yearFilterCombo.getValue();
        String semester = semesterFilterCombo.getValue();
        if (academicYear == null || semester == null) {
            // בלי מסננים מלאים אין הרצה מוגדרת להצגה.
            allResults.clear();
            resultsTable.setItems(allResults);
            updateStatistics();
            statusLabel.setText("יש לבחור שנת לימודים וסמסטר להצגת תוצאות.");
            return;
        }

        // טוען את התוצאות המעובדות ומציג אותן בטבלה.
        List<EnrollmentResult> results = repository.loadEnrollmentResults(academicYear, semester);
        allResults.setAll(results);
        resultsTable.setItems(allResults);
        updateStatistics();
        statusLabel.setText("הוצגו " + results.size() + " תוצאות עבור " + semester + " (" + academicYear + ").");
    }
}

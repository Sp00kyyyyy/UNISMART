package com.example.java_main_proj.controller;

import com.example.java_main_proj.model.Student;
import com.example.java_main_proj.repository.SchedulingDataRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * בקר מסך הסטודנטים.
 * אחראי לטעון את הנתונים, להציגם בטבלה ולאפשר חיפוש מקומי מהיר.
 */
public class StudentController {

    @FXML private TextField searchField;
    @FXML private TableView<Student> studentsTable;
    @FXML private TableColumn<Student, Integer> idColumn;
    @FXML private TableColumn<Student, String> nameColumn;
    @FXML private TableColumn<Student, String> idNumberColumn;
    @FXML private TableColumn<Student, Integer> yearColumn;
    @FXML private TableColumn<Student, String> trackColumn;
    @FXML private TableColumn<Student, Integer> priorityColumn;
    @FXML private TableColumn<Student, Integer> seniorityColumn;
    @FXML private TableColumn<Student, Double> gpaColumn;
    @FXML private Label statusLabel;

    private final ObservableList<Student> studentsList = FXCollections.observableArrayList();
    private final SchedulingDataRepository repository = new SchedulingDataRepository();

    /**
     * מאתחל את טבלת הסטודנטים וטוען את הנתונים הראשוניים למסך.
     */
    @FXML
    public void initialize() {
        // בונה את עמודות הטבלה לפני טעינת הנתונים.
        setupTableColumns();
        // לאחר מכן טוען את רשימת הסטודנטים למסך.
        loadStudents();
    }

    /**
     * קושר בין עמודות ה-TableView לבין שדות המודל.
     */
    private void setupTableColumns() {
        // כל עמודה משויכת לשדה מתאים מתוך אובייקט Student.
        idColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getStudentID()).asObject());
        nameColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFullName()));
        idNumberColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getIdNumber()));
        yearColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getYear()).asObject());
        trackColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTrack()));
        priorityColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getPriorityLevel()).asObject());
        seniorityColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getSeniority()).asObject());
        gpaColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getGpa()).asObject());
    }

    /**
     * טוען מחדש את רשימת הסטודנטים מהמסד ומעדכן את הטבלה.
     */
    private void loadStudents() {
        // מנקה טעינה קודמת כדי למנוע כפילויות בתצוגה.
        studentsList.clear();

        try {
            // שולף את רשימת הסטודנטים המלאה משכבת הנתונים.
            List<Student> students = repository.loadStudents();
            studentsList.addAll(students);
            // הטבלה מציגה את הרשימה הראשית שממנה גם ייגזר חיפוש מקומי.
            studentsTable.setItems(studentsList);
            statusLabel.setText("נטענו " + students.size() + " סטודנטים.");
        } catch (Exception exception) {
            statusLabel.setText("שגיאה בטעינת נתוני הסטודנטים: " + exception.getMessage());
            exception.printStackTrace();
        }
    }

    /**
     * מסנן את הטבלה לפי שם או תעודת זהות על בסיס הרשימה שכבר נטענה לזיכרון.
     */
    @FXML
    private void searchStudent() {
        // מנרמל את טקסט החיפוש כדי לבצע התאמה פשוטה ולא רגישה לרישיות.
        String searchTerm = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        if (searchTerm.isBlank()) {
            // כאשר אין ביטוי חיפוש, חוזרים לתצוגה מלאה.
            studentsTable.setItems(studentsList);
            statusLabel.setText("הוצגו כל הסטודנטים.");
            return;
        }

        ObservableList<Student> filtered = FXCollections.observableArrayList(
                // הסינון מתבצע בזיכרון המקומי ולכן אינו דורש גישה נוספת למסד.
                studentsList.stream()
                        .filter(student -> student.getFullName().toLowerCase(Locale.ROOT).contains(searchTerm) ||
                                student.getIdNumber().toLowerCase(Locale.ROOT).contains(searchTerm))
                        .collect(Collectors.toList())
        );
        studentsTable.setItems(filtered);
        statusLabel.setText("נמצאו " + filtered.size() + " סטודנטים.");
    }

    /**
     * מרענן את הטבלה מנתוני המסד.
     */
    @FXML
    private void refreshTable() {
        // מבצע טעינה מחודשת מהמסד כדי להציג נתונים עדכניים.
        loadStudents();
        statusLabel.setText("רשימת הסטודנטים רועננה.");
    }

    /**
     * סוגר את חלון הסטודנטים.
     */
    @FXML
    private void closeWindow() {
        // סוגר רק את חלון הסטודנטים.
        Stage stage = (Stage) studentsTable.getScene().getWindow();
        stage.close();
    }
}

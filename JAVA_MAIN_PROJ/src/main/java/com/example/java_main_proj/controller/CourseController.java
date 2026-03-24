package com.example.java_main_proj.controller;

import com.example.java_main_proj.model.Course;
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
 * בקר מסך הקורסים.
 * מציג את רשימת הקורסים ומאפשר חיפוש לפי שם קורס או מרצה.
 */
public class CourseController {

    @FXML private TextField searchField;
    @FXML private TableView<Course> coursesTable;
    @FXML private TableColumn<Course, Integer> idColumn;
    @FXML private TableColumn<Course, String> nameColumn;
    @FXML private TableColumn<Course, String> typeColumn;
    @FXML private TableColumn<Course, String> lecturerColumn;
    @FXML private TableColumn<Course, String> dayColumn;
    @FXML private TableColumn<Course, String> startTimeColumn;
    @FXML private TableColumn<Course, String> endTimeColumn;
    @FXML private TableColumn<Course, Integer> capacityColumn;
    @FXML private TableColumn<Course, Integer> enrolledColumn;
    @FXML private Label statusLabel;

    private final ObservableList<Course> coursesList = FXCollections.observableArrayList();
    private final SchedulingDataRepository repository = new SchedulingDataRepository();

    /**
     * מאתחל את טבלת הקורסים וטוען את הנתונים הראשוניים למסך.
     */
    @FXML
    public void initialize() {
        // מגדיר את מבנה הטבלה לפני טעינת הנתונים.
        setupTableColumns();
        // אחר כך טוען את הקורסים מהמסד.
        loadCourses();
    }

    /**
     * קישור עמודות הטבלה לשדות קורס.
     */
    private void setupTableColumns() {
        // כל עמודה משויכת לשדה מתאים במודל Course.
        idColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getCourseID()).asObject());
        nameColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCourseName()));
        typeColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getCourseType()));
        lecturerColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getLecturer()));
        dayColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getDay()));
        startTimeColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getStartTime()));
        endTimeColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getEndTime()));
        capacityColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getCapacity()).asObject());
        enrolledColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getEnrolledStudents()).asObject());
    }

    /**
     * טוען את הקורסים מהמסד ומרענן את הטבלה.
     */
    private void loadCourses() {
        // מנקה את הרשימה המקומית לפני רענון.
        coursesList.clear();

        try {
            // null אומר לשכבת הנתונים להחזיר את כל הקורסים.
            List<Course> courses = repository.loadCourses(null);
            coursesList.addAll(courses);
            coursesTable.setItems(coursesList);
            statusLabel.setText("נטענו " + courses.size() + " קורסים.");
        } catch (Exception exception) {
            statusLabel.setText("שגיאה בטעינת הקורסים: " + exception.getMessage());
            exception.printStackTrace();
        }
    }

    /**
     * חיפוש מקומי ברשימת הקורסים שכבר נטענה.
     */
    @FXML
    private void searchCourse() {
        // מנרמל את טקסט החיפוש לשימוש אחיד.
        String searchTerm = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        if (searchTerm.isBlank()) {
            coursesTable.setItems(coursesList);
            statusLabel.setText("הוצגו כל הקורסים.");
            return;
        }

        ObservableList<Course> filtered = FXCollections.observableArrayList(
                // ההתאמה נעשית לפי שם קורס או שם מרצה על הרשימה שכבר נטענה.
                coursesList.stream()
                        .filter(course -> course.getCourseName().toLowerCase(Locale.ROOT).contains(searchTerm) ||
                                course.getLecturer().toLowerCase(Locale.ROOT).contains(searchTerm))
                        .collect(Collectors.toList())
        );
        coursesTable.setItems(filtered);
        statusLabel.setText("נמצאו " + filtered.size() + " קורסים מתאימים.");
    }

    /**
     * מרענן את טבלת הקורסים מנתוני המסד.
     */
    @FXML
    private void refreshTable() {
        // רענון אמיתי מהמסד ולא רק שחזור של הרשימה הקיימת.
        loadCourses();
        statusLabel.setText("רשימת הקורסים רועננה.");
    }

    /**
     * סוגר את חלון הקורסים.
     */
    @FXML
    private void closeWindow() {
        // סוגר את חלון הקורסים.
        Stage stage = (Stage) coursesTable.getScene().getWindow();
        stage.close();
    }
}

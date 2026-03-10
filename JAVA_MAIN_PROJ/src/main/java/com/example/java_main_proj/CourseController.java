package com.example.java_main_proj;

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

    @FXML
    public void initialize() {
        setupTableColumns();
        loadCourses();
    }

    private void setupTableColumns() {
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

    private void loadCourses() {
        coursesList.clear();

        try {
            List<Course> courses = repository.loadCourses(null);
            coursesList.addAll(courses);
            coursesTable.setItems(coursesList);
            statusLabel.setText("נטענו " + courses.size() + " קורסים.");
        } catch (Exception exception) {
            statusLabel.setText("שגיאה בטעינת הקורסים: " + exception.getMessage());
            exception.printStackTrace();
        }
    }

    @FXML
    private void searchCourse() {
        String searchTerm = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        if (searchTerm.isBlank()) {
            coursesTable.setItems(coursesList);
            statusLabel.setText("הוצגו כל הקורסים.");
            return;
        }

        ObservableList<Course> filtered = FXCollections.observableArrayList(
                coursesList.stream()
                        .filter(course -> course.getCourseName().toLowerCase(Locale.ROOT).contains(searchTerm) ||
                                course.getLecturer().toLowerCase(Locale.ROOT).contains(searchTerm))
                        .collect(Collectors.toList())
        );
        coursesTable.setItems(filtered);
        statusLabel.setText("נמצאו " + filtered.size() + " קורסים מתאימים.");
    }

    @FXML
    private void refreshTable() {
        loadCourses();
        statusLabel.setText("רשימת הקורסים רועננה.");
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) coursesTable.getScene().getWindow();
        stage.close();
    }
}

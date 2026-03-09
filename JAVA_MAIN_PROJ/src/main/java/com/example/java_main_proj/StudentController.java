package com.example.java_main_proj;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

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
    private final GuidewayRepository repository = new GuidewayRepository();

    @FXML
    public void initialize() {
        setupTableColumns();
        loadStudents();
    }

    private void setupTableColumns() {
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

    private void loadStudents() {
        studentsList.clear();

        try {
            List<Student> students = repository.loadStudents();
            studentsList.addAll(students);
            studentsTable.setItems(studentsList);
            statusLabel.setText(students.size() + " .םיטנדוטס ונעטנ");
        } catch (Exception exception) {
            statusLabel.setText(" :םיטנדוטסה ינותנ תניעטב האיגש" + exception.getMessage());
            exception.printStackTrace();
        }
    }

    @FXML
    private void searchStudent() {
        String searchTerm = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase(Locale.ROOT);
        if (searchTerm.isBlank()) {
            studentsTable.setItems(studentsList);
            statusLabel.setText(".םיטנדוטסה לכ וגצוה");
            return;
        }

        ObservableList<Student> filtered = FXCollections.observableArrayList(
                studentsList.stream()
                        .filter(student -> student.getFullName().toLowerCase(Locale.ROOT).contains(searchTerm) ||
                                student.getIdNumber().toLowerCase(Locale.ROOT).contains(searchTerm))
                        .collect(Collectors.toList())
        );
        studentsTable.setItems(filtered);
        statusLabel.setText(filtered.size() + " .םיטנדוטס ואצמנ");
    }

    @FXML
    private void addStudent() {
        showAlert("טנדוטס תפסוה", ".םינותנה דסמל רבוח אל ןיידע הכירעה ךסמ");
    }

    @FXML
    private void editStudent() {
        Student selected = studentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("האיגש", ".הכירעל טנדוטס רוחבל שי");
            return;
        }
        showAlert("טנדוטס תכירע", ".םינותנה דסמל רבוח אל ןיידע הכירעה ךסמ");
    }

    @FXML
    private void deleteStudent() {
        Student selected = studentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("האיגש", ".הקיחמל טנדוטס רוחבל שי");
            return;
        }
        showAlert("טנדוטס תקיחמ", ".תלעפומ אל ןיידע הרישי הקיחמ");
    }

    @FXML
    private void refreshTable() {
        loadStudents();
        statusLabel.setText(".הננעור םיטנדוטסה תמישר");
    }

    @FXML
    private void closeWindow() {
        Stage stage = (Stage) studentsTable.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

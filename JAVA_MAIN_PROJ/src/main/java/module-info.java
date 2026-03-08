module com.example.java_main_proj {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;

    opens com.example.java_main_proj to javafx.fxml;
    exports com.example.java_main_proj;
}

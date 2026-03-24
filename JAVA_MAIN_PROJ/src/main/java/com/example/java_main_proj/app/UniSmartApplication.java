package com.example.java_main_proj.app;

import com.example.java_main_proj.controller.MainDashboardController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * מחלקת הכניסה של יישום JavaFX.
 * אחריותה לטעון את מסך הבית וליצור את החלון הראשי של המערכת.
 */
public class UniSmartApplication extends Application {
    /**
     * טוען את מסך הבית ומציג את החלון הראשי של היישום.
     *
     * @param stage החלון הראשי ש-JavaFX מספק ליישום
     * @throws IOException אם טעינת קובץ ה-FXML נכשלת
     */
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(MainDashboardController.class.getResource("/com/example/java_main_proj/main-dashboard-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 1000, 700);
        stage.setTitle("UniSmart - מערכת שיבוץ סטודנטים");
        stage.setScene(scene);
        stage.show();
    }

    /**
     * מפעיל את סביבת JavaFX של היישום.
     *
     * @param args ארגומנטים משורת הפקודה
     */
    public static void main(String[] args) {
        launch();
    }
}

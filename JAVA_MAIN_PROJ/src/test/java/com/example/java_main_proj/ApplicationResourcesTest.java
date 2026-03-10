package com.example.java_main_proj;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApplicationResourcesTest {

    @Test
    void packagedViewsAndDatabaseExistOnTheClasspath() {
        // Arrange
        ClassLoader classLoader = getClass().getClassLoader();

        // Act + Assert
        assertNotNull(classLoader.getResource("UniSmartDB1.accdb"));
        assertNotNull(classLoader.getResource("com/example/java_main_proj/main-dashboard-view.fxml"));
        assertNotNull(classLoader.getResource("com/example/java_main_proj/student-view.fxml"));
        assertNotNull(classLoader.getResource("com/example/java_main_proj/course-view.fxml"));
        assertNotNull(classLoader.getResource("com/example/java_main_proj/enrollment-view.fxml"));
        assertNotNull(classLoader.getResource("com/example/java_main_proj/results-view.fxml"));
    }
}

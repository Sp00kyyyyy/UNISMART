package com.example.java_main_proj;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaFxViewSmokeTest {

    @BeforeAll
    static void startJavaFxToolkit() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException ignored) {
            latch.countDown();
        }
        assertTrue(latch.await(10, TimeUnit.SECONDS), "JavaFX toolkit did not start");
    }

    @Test
    void allViewsLoadWithTheirControllers() throws Exception {
        assertViewLoads("/com/example/java_main_proj/hello-view.fxml");
        assertViewLoads("/com/example/java_main_proj/student-view.fxml");
        assertViewLoads("/com/example/java_main_proj/course-view.fxml");
        assertViewLoads("/com/example/java_main_proj/enrollment-view.fxml");
        assertViewLoads("/com/example/java_main_proj/results-view.fxml");
    }

    private void assertViewLoads(String resourcePath) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        AtomicReference<Parent> loadedRoot = new AtomicReference<>();
        AtomicReference<Object> controller = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(resourcePath));
                Parent root = loader.load();
                loadedRoot.set(root);
                controller.set(loader.getController());
                assertDoesNotThrow(() -> new Scene(root));
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS), "Timed out loading " + resourcePath);
        if (failure.get() != null) {
            throw new AssertionError("Failed to load " + resourcePath, failure.get());
        }
        assertNotNull(loadedRoot.get(), "FXML root was null for " + resourcePath);
        assertNotNull(controller.get(), "Controller was null for " + resourcePath);
    }
}

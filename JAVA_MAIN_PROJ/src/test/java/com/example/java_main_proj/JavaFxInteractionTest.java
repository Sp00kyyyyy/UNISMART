package com.example.java_main_proj;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaFxInteractionTest {
    private static final String SEMESTER_A = "\u05E1\u05DE\u05E1\u05D8\u05E8 \u05D0'";
    private static final String SEMESTER_B = "\u05E1\u05DE\u05E1\u05D8\u05E8 \u05D1'";

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

    @AfterEach
    void tearDown() {
        DatabaseConnection.closeConnection();
        System.clearProperty("unismart.db.path");
    }

    @Test
    void studentSearchFiltersTheVisibleTableRows() throws Exception {
        LoadedView<StudentController> loaded = loadView("/com/example/java_main_proj/student-view.fxml");

        runOnFxThreadAndWait(() -> {
            TextField searchField = field(loaded.controller(), "searchField", TextField.class);
            TableView<Student> table = field(loaded.controller(), "studentsTable", TableView.class);

            int initialSize = table.getItems().size();
            String uniqueQuery = table.getItems().getFirst().getIdNumber();
            searchField.setText(uniqueQuery);
            invoke(loaded.controller(), "searchStudent");

            assertTrue(initialSize > 0);
            assertFalse(table.getItems().isEmpty());
            assertTrue(table.getItems().size() < initialSize);
            assertTrue(table.getItems().stream().allMatch(student -> student.getIdNumber().equals(uniqueQuery)));
        });
    }

    @Test
    void courseSearchFiltersTheVisibleTableRows() throws Exception {
        LoadedView<CourseController> loaded = loadView("/com/example/java_main_proj/course-view.fxml");

        runOnFxThreadAndWait(() -> {
            TextField searchField = field(loaded.controller(), "searchField", TextField.class);
            TableView<Course> table = field(loaded.controller(), "coursesTable", TableView.class);

            int initialSize = table.getItems().size();
            String uniqueQuery = table.getItems().stream()
                    .map(Course::getCourseName)
                    .filter(name -> table.getItems().stream()
                            .filter(course -> course.getCourseName().toLowerCase().contains(name.toLowerCase()) ||
                                    course.getLecturer().toLowerCase().contains(name.toLowerCase()))
                            .count() == 1)
                    .findFirst()
                    .orElse(table.getItems().getFirst().getCourseName());
            searchField.setText(uniqueQuery);
            invoke(loaded.controller(), "searchCourse");

            assertTrue(initialSize > 0);
            assertFalse(table.getItems().isEmpty());
            assertTrue(table.getItems().size() < initialSize);
            assertTrue(table.getItems().stream().allMatch(course ->
                    course.getCourseName().toLowerCase().contains(uniqueQuery.toLowerCase()) ||
                            course.getLecturer().toLowerCase().contains(uniqueQuery.toLowerCase())));
        });
    }

    @Test
    void resultsRefreshWhenTheSemesterFilterChanges() throws Exception {
        useIsolatedDatabaseCopy();
        HybridEnrollmentService service = new HybridEnrollmentService();
        service.runEnrollment("2025-2026", SEMESTER_A);
        service.runEnrollment("2025-2026", SEMESTER_B);

        LoadedView<ResultsController> loaded = loadView("/com/example/java_main_proj/results-view.fxml");

        runOnFxThreadAndWait(() -> {
            ComboBox<String> yearFilter = field(loaded.controller(), "yearFilterCombo", ComboBox.class);
            ComboBox<String> semesterFilter = field(loaded.controller(), "semesterFilterCombo", ComboBox.class);
            TableView<EnrollmentResult> table = field(loaded.controller(), "resultsTable", TableView.class);
            Label statusLabel = field(loaded.controller(), "statusLabel", Label.class);

            yearFilter.setValue("2025-2026");
            semesterFilter.setValue(SEMESTER_A);
            fireAction(semesterFilter);
            List<String> semesterACourses = table.getItems().stream()
                    .map(EnrollmentResult::getCoursesList)
                    .toList();

            semesterFilter.setValue(SEMESTER_B);
            fireAction(semesterFilter);
            List<String> semesterBCourses = table.getItems().stream()
                    .map(EnrollmentResult::getCoursesList)
                    .toList();

            assertFalse(semesterACourses.isEmpty());
            assertFalse(semesterBCourses.isEmpty());
            assertNotEquals(semesterACourses, semesterBCourses);
            assertTrue(statusLabel.getText().contains(SEMESTER_B));
        });
    }

    @Test
    void enrollmentScreenDefaultsAndClearActionBehaveCorrectly() throws Exception {
        LoadedView<EnrollmentController> loaded = loadView("/com/example/java_main_proj/enrollment-view.fxml");

        runOnFxThreadAndWait(() -> {
            ComboBox<String> yearCombo = field(loaded.controller(), "yearComboBox", ComboBox.class);
            ComboBox<String> semesterCombo = field(loaded.controller(), "semesterComboBox", ComboBox.class);
            ProgressBar progressBar = field(loaded.controller(), "progressBar", ProgressBar.class);
            Label progressLabel = field(loaded.controller(), "progressLabel", Label.class);
            Label statusLabel = field(loaded.controller(), "statusLabel", Label.class);
            TextArea logArea = field(loaded.controller(), "logArea", TextArea.class);

            assertEquals("2025-2026", yearCombo.getValue());
            assertEquals(SEMESTER_A, semesterCombo.getValue());
            assertEquals(List.of(SEMESTER_A, SEMESTER_B), semesterCombo.getItems());

            logArea.setText("temporary log");
            progressBar.setProgress(0.75);
            progressLabel.setText("75%");
            invoke(loaded.controller(), "clearLog");

            assertEquals("", logArea.getText());
            assertEquals(0.0, progressBar.getProgress());
            assertEquals("0%", progressLabel.getText());
            assertFalse(statusLabel.getText().isBlank());
        });
    }

    @Test
    void enrollmentScreenCanRunTheSchedulerEndToEnd() throws Exception {
        useIsolatedDatabaseCopy();
        LoadedView<EnrollmentController> loaded = loadView("/com/example/java_main_proj/enrollment-view.fxml");

        runOnFxThreadAndWait(() -> invoke(loaded.controller(), "startEnrollment"));

        assertTrue(waitForFxCondition(() -> {
            Label progressLabel = field(loaded.controller(), "progressLabel", Label.class);
            Label statusLabel = field(loaded.controller(), "statusLabel", Label.class);
            TextArea logArea = field(loaded.controller(), "logArea", TextArea.class);
            return "100%".equals(progressLabel.getText())
                    && statusLabel.getText().contains(SEMESTER_A)
                    && !logArea.getText().isBlank();
        }, 20), "Enrollment UI flow did not complete");

        SchedulingDataRepository repository = new SchedulingDataRepository();
        List<EnrollmentResult> results = repository.loadEnrollmentResults("2025-2026", SEMESTER_A);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(result -> result.getEnrolledCourses() > 0));
    }

    private static void fireAction(ComboBox<String> comboBox) {
        if (comboBox.getOnAction() != null) {
            comboBox.getOnAction().handle(new ActionEvent(comboBox, comboBox));
        }
    }

    private static boolean waitForFxCondition(FxBooleanSupplier condition, int timeoutSeconds) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        while (System.nanoTime() < deadline) {
            AtomicReference<Boolean> result = new AtomicReference<>(false);
            runOnFxThreadAndWait(() -> result.set(condition.getAsBoolean()));
            if (Boolean.TRUE.equals(result.get())) {
                return true;
            }
            Thread.sleep(100);
        }
        return false;
    }

    private static <T> LoadedView<T> loadView(String resourcePath) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<LoadedView<T>> loadedRef = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(JavaFxInteractionTest.class.getResource(resourcePath));
                Parent root = loader.load();
                @SuppressWarnings("unchecked")
                T controller = (T) loader.getController();
                loadedRef.set(new LoadedView<>(root, controller));
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
        return loadedRef.get();
    }

    private static void runOnFxThreadAndWait(FxAssertion action) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(10, TimeUnit.SECONDS), "FX action timed out");
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(Object target, String name, Class<T> type) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(target);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to read field " + name, exception);
        }
    }

    private static void invoke(Object target, String methodName) {
        try {
            Method method = target.getClass().getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(target);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Failed to invoke " + methodName, exception);
        }
    }

    private record LoadedView<T>(Parent root, T controller) {
    }

    @FunctionalInterface
    private interface FxAssertion {
        void run() throws Exception;
    }

    @FunctionalInterface
    private interface FxBooleanSupplier {
        boolean getAsBoolean() throws Exception;
    }

    private void useIsolatedDatabaseCopy() throws IOException {
        Path sourceDatabase = Path.of("src", "main", "resources", "UniSmartDB1.accdb").toAbsolutePath();
        Path tempDir = Files.createTempDirectory("unismart-javafx-db-test");
        Path isolatedDatabase = tempDir.resolve("UniSmartDB1.accdb");
        Files.copy(sourceDatabase, isolatedDatabase, StandardCopyOption.REPLACE_EXISTING);
        DatabaseConnection.closeConnection();
        System.setProperty("unismart.db.path", isolatedDatabase.toString());
    }
}

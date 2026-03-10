package com.example.java_main_proj.repository;

import com.example.java_main_proj.model.ConstraintRule;
import com.example.java_main_proj.model.Course;
import com.example.java_main_proj.model.CourseRequirement;
import com.example.java_main_proj.model.EnrollmentDecision;
import com.example.java_main_proj.model.EnrollmentResult;
import com.example.java_main_proj.model.Student;
import java.util.List;
import java.util.Map;

public class SchedulingDataRepository {
    private final DatabaseCatalogReader catalogReader;
    private final EnrollmentRunRepository enrollmentRunRepository;
    private final EnrollmentResultsRepository enrollmentResultsRepository;

    public SchedulingDataRepository() {
        this(new DatabaseCatalogReader(), new EnrollmentRunRepository());
    }

    SchedulingDataRepository(DatabaseCatalogReader catalogReader, EnrollmentRunRepository enrollmentRunRepository) {
        this.catalogReader = catalogReader;
        this.enrollmentRunRepository = enrollmentRunRepository;
        this.enrollmentResultsRepository = new EnrollmentResultsRepository(catalogReader);
    }

    public List<Student> loadStudents() {
        return catalogReader.loadStudents();
    }

    public List<Course> loadCourses(String semester) {
        return catalogReader.loadCourses(semester);
    }

    public List<CourseRequirement> loadCourseRequirements() {
        return catalogReader.loadCourseRequirements();
    }

    public Map<String, ConstraintRule> loadConstraints() {
        return catalogReader.loadConstraints();
    }

    public void replaceEnrollmentRun(String academicYear, String semester, List<EnrollmentDecision> decisions) {
        enrollmentRunRepository.replaceEnrollmentRun(academicYear, semester, decisions);
    }

    public List<EnrollmentResult> loadEnrollmentResults(String academicYear, String semester) {
        return enrollmentResultsRepository.loadEnrollmentResults(academicYear, semester, loadCourseRequirements());
    }

    public List<String> loadAcademicYearsWithResults() {
        return enrollmentRunRepository.loadAcademicYearsWithResults();
    }

    public List<String> loadSemestersWithResults() {
        return enrollmentRunRepository.loadSemestersWithResults();
    }
}

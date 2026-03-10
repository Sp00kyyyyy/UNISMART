package com.example.java_main_proj;

import java.util.List;
import java.util.Map;

public class GuidewayRepository {
    private final GuidewayCatalogLoader catalogLoader;
    private final EnrollmentRunStore enrollmentRunStore;
    private final EnrollmentResultsLoader enrollmentResultsLoader;

    public GuidewayRepository() {
        this(new GuidewayCatalogLoader(), new EnrollmentRunStore());
    }

    GuidewayRepository(GuidewayCatalogLoader catalogLoader, EnrollmentRunStore enrollmentRunStore) {
        this.catalogLoader = catalogLoader;
        this.enrollmentRunStore = enrollmentRunStore;
        this.enrollmentResultsLoader = new EnrollmentResultsLoader(catalogLoader);
    }

    public List<Student> loadStudents() {
        return catalogLoader.loadStudents();
    }

    public List<Course> loadCourses(String semester) {
        return catalogLoader.loadCourses(semester);
    }

    public List<CourseRequirement> loadCourseRequirements() {
        return catalogLoader.loadCourseRequirements();
    }

    public Map<String, ConstraintRule> loadConstraints() {
        return catalogLoader.loadConstraints();
    }

    public void replaceEnrollmentRun(String academicYear, String semester, List<EnrollmentDecision> decisions) {
        enrollmentRunStore.replaceEnrollmentRun(academicYear, semester, decisions);
    }

    public List<EnrollmentResult> loadEnrollmentResults(String academicYear, String semester) {
        return enrollmentResultsLoader.loadEnrollmentResults(academicYear, semester, loadCourseRequirements());
    }

    public List<String> loadAcademicYearsWithResults() {
        return enrollmentRunStore.loadAcademicYearsWithResults();
    }

    public List<String> loadSemestersWithResults() {
        return enrollmentRunStore.loadSemestersWithResults();
    }
}

package com.example.java_main_proj.db;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * מנהל חיבור יחיד למסד הנתונים.
 * המחלקה גם יודעת לאתר את קובץ Access בכמה נתיבים אפשריים כדי לאפשר הרצה
 * גם מתוך סביבת פיתוח וגם מתוך חבילה רצה.
 */
public class DatabaseConnection {
    private static java.sql.Connection connection;

    /**
     * מחזיר חיבור פעיל למסד הנתונים, או יוצר אחד חדש אם עדיין לא נפתח חיבור.
     */
    public static java.sql.Connection getConnection() {
        try {
            // מאתר קודם את קובץ המסד לפי רשימת נתיבים אפשריים.
            String dbPath = resolveDatabasePath();
            File dbFile = new File(dbPath);

            if (!dbFile.exists()) {
                System.err.println("Database file was not found at: " + dbPath);
                return null;
            }

            String connectionURL = "jdbc:ucanaccess://" + dbPath + ";memory=false";
            if (connection == null || connection.isClosed()) {
                // שומר חיבור יחיד שניתן למחזור במקום לפתוח חיבור חדש בכל קריאה.
                connection = java.sql.DriverManager.getConnection(connectionURL);
            }
        } catch (java.sql.SQLException exception) {
            System.err.println("SQL error: " + exception.getMessage());
            exception.printStackTrace();
        } catch (Exception exception) {
            System.err.println("Unexpected database error: " + exception.getMessage());
            exception.printStackTrace();
        }
        return connection;
    }

    /**
     * סוגר את החיבור המרכזי למסד הנתונים כאשר היישום מסיים לעבוד.
     */
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (java.sql.SQLException exception) {
            System.err.println("Error while closing database connection: " + exception.getMessage());
        }
    }

    /**
     * בדיקת בריאות מהירה לצורך הצגה בממשק הראשי.
     */
    public static boolean testConnection() {
        try {
            // משתמש באותה לוגיקת חיבור של המערכת, אך מחזיר רק תשובת true/false.
            java.sql.Connection currentConnection = getConnection();
            return currentConnection != null && !currentConnection.isClosed();
        } catch (Exception exception) {
            return false;
        }
    }

    /**
     * בונה רשימת נתיבים אפשריים למסד ומחזיר את הנתיב הראשון שקיים בפועל.
     */
    private static String resolveDatabasePath() {
        List<Path> candidates = new ArrayList<>();
        String propertyPath = System.getProperty("unismart.db.path");
        if (propertyPath != null && !propertyPath.isBlank()) {
            // מאפשר להכתיב נתיב מפורש דרך property בזמן הרצה.
            candidates.add(Path.of(propertyPath));
        }

        String explicitPath = System.getenv("UNISMART_DB_PATH");
        if (explicitPath != null && !explicitPath.isBlank()) {
            // תומך גם בהגדרת נתיב דרך משתנה סביבה.
            candidates.add(Path.of(explicitPath));
        }

        Path workingDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        candidates.add(workingDirectory.resolve(Path.of("src", "main", "resources", "UniSmartDB1.accdb")));
        candidates.add(workingDirectory.resolve(Path.of("JAVA_MAIN_PROJ", "src", "main", "resources", "UniSmartDB1.accdb")));
        candidates.add(workingDirectory.resolve(Path.of("Documents", "UNISMART", "JAVA_MAIN_PROJ", "src", "main", "resources", "UniSmartDB1.accdb")));
        candidates.add(workingDirectory.resolve(Path.of("Documents", "UNISMART", "src", "main", "resources", "UniSmartDB1.accdb")));
        addAncestorCandidates(candidates, workingDirectory);

        URL resource = DatabaseConnection.class.getClassLoader().getResource("UniSmartDB1.accdb");
        if (resource != null && "file".equalsIgnoreCase(resource.getProtocol())) {
            try {
                // אם הקובץ נארז כ-resource, מוסיפים גם את הנתיב הזה לרשימת המועמדים.
                candidates.add(Path.of(resource.toURI()));
            } catch (URISyntaxException ignored) {
            }
        }

        for (Path candidate : candidates) {
            // מחזיר את הנתיב הראשון שקיים בפועל על הדיסק.
            if (candidate != null && candidate.toFile().exists()) {
                return candidate.toAbsolutePath().toString();
            }
        }

        return candidates.isEmpty()
                ? Path.of("UniSmartDB1.accdb").toAbsolutePath().toString()
                : candidates.get(0).toAbsolutePath().toString();
    }

    /**
     * מוסיף מועמדים יחסיים לכל אחת מהתיקיות שמעל תיקיית העבודה הנוכחית.
     */
    private static void addAncestorCandidates(List<Path> candidates, Path start) {
        Path current = start;
        while (current != null) {
            // מטפס כלפי מעלה בעץ התיקיות כדי לאפשר הרצה ממיקומי עבודה שונים.
            candidates.add(current.resolve(Path.of("src", "main", "resources", "UniSmartDB1.accdb")));
            candidates.add(current.resolve(Path.of("JAVA_MAIN_PROJ", "src", "main", "resources", "UniSmartDB1.accdb")));
            current = current.getParent();
        }
    }
}

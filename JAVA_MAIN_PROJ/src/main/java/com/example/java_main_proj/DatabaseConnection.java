package com.example.java_main_proj;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DatabaseConnection {
    private static java.sql.Connection connection;

    public static java.sql.Connection getConnection() {
        try {
            String dbPath = resolveDatabasePath();
            File dbFile = new File(dbPath);

            if (!dbFile.exists()) {
                System.err.println("Database file was not found at: " + dbPath);
                return null;
            }

            String connectionURL = "jdbc:ucanaccess://" + dbPath + ";memory=false";
            if (connection == null || connection.isClosed()) {
                connection = java.sql.DriverManager.getConnection(connectionURL);
                DatabaseBootstrap.synchronize(connection);
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

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (java.sql.SQLException exception) {
            System.err.println("Error while closing database connection: " + exception.getMessage());
        }
    }

    public static boolean testConnection() {
        try {
            java.sql.Connection currentConnection = getConnection();
            return currentConnection != null && !currentConnection.isClosed();
        } catch (Exception exception) {
            return false;
        }
    }

    private static String resolveDatabasePath() {
        List<Path> candidates = new ArrayList<>();
        String explicitPath = System.getenv("UNISMART_DB_PATH");
        if (explicitPath != null && !explicitPath.isBlank()) {
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
                candidates.add(Path.of(resource.toURI()));
            } catch (URISyntaxException ignored) {
            }
        }

        for (Path candidate : candidates) {
            if (candidate != null && candidate.toFile().exists()) {
                return candidate.toAbsolutePath().toString();
            }
        }

        return candidates.isEmpty()
                ? Path.of("UniSmartDB1.accdb").toAbsolutePath().toString()
                : candidates.get(0).toAbsolutePath().toString();
    }

    private static void addAncestorCandidates(List<Path> candidates, Path start) {
        Path current = start;
        while (current != null) {
            candidates.add(current.resolve(Path.of("src", "main", "resources", "UniSmartDB1.accdb")));
            candidates.add(current.resolve(Path.of("JAVA_MAIN_PROJ", "src", "main", "resources", "UniSmartDB1.accdb")));
            current = current.getParent();
        }
    }
}

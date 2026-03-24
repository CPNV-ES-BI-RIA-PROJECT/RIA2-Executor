package com.executor.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Service;

@Service
public class SqlExecutorService {

    private final DataSource dataSource;
    private final Path scriptDirectory;

    public SqlExecutorService(
            DataSource dataSource,
            @Value("${sql.script.directory:data/script}") String scriptDirectory) {
        this.dataSource = dataSource;
        this.scriptDirectory = Path.of(scriptDirectory).toAbsolutePath().normalize();
    }

    public void executeScript(String localPath) {
        Path scriptPath = resolveScriptPath(localPath);
        executeScriptFile(scriptPath);
        deleteScriptFile(scriptPath);
    }

    public void executeAllScripts() {
        List<Path> scriptPaths = listScriptPaths();

        for (Path scriptPath : scriptPaths) {
            executeScriptFile(scriptPath);
            deleteScriptFile(scriptPath);
        }
    }

    private List<Path> listScriptPaths() {
        if (Files.notExists(scriptDirectory)) {
            return List.of();
        }

        try (Stream<Path> paths = Files.list(scriptDirectory)) {
            List<Path> scriptPaths = new ArrayList<>();

            for (Path path : paths.sorted().toList()) {
                if (!isSqlFile(path)) {
                    continue;
                }

                if (Files.isSymbolicLink(path)) {
                    throw new IllegalArgumentException("Symbolic links are not allowed: " + path);
                }

                if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) {
                    throw new IllegalArgumentException("Invalid SQL script file: " + path);
                }

                scriptPaths.add(path);
            }

            return scriptPaths;
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to list SQL scripts in " + scriptDirectory,
                    exception);
        }
    }

    private Path resolveScriptPath(String localPath) {
        Path providedPath = Path.of(localPath).toAbsolutePath().normalize();

        if (!providedPath.startsWith(scriptDirectory)) {
            throw new IllegalArgumentException("SQL script must stay under " + scriptDirectory);
        }

        if (Files.isSymbolicLink(providedPath)) {
            throw new IllegalArgumentException("Symbolic links are not allowed: " + providedPath);
        }

        if (!Files.isRegularFile(providedPath, LinkOption.NOFOLLOW_LINKS)) {
            throw new IllegalArgumentException("Invalid SQL script file: " + providedPath);
        }

        if (!isSqlFile(providedPath)) {
            throw new IllegalArgumentException("Only .sql files are allowed: " + providedPath);
        }

        return providedPath;
    }

    private boolean isSqlFile(Path path) {
        String filename = String.valueOf(path.getFileName());
        return filename.length() >= 4
                && filename.regionMatches(true, filename.length() - 4, ".sql", 0, 4);
    }

    private void executeScriptFile(Path scriptPath) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new FileSystemResource(scriptPath.toFile()));
        populator.setContinueOnError(false);
        populator.execute(dataSource);
    }

    private void deleteScriptFile(Path scriptPath) {
        try {
            Files.delete(scriptPath);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to delete executed SQL script " + scriptPath,
                    exception);
        }
    }
}

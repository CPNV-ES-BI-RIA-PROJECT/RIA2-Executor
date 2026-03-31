package com.executor.services;

import com.executor.dto.DownloadedFileDto;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Service
public class SqlExecutorService {

    private final DataSource dataSource;
    private final Path scriptDirectory;

    public SqlExecutorService(
            DataSource dataSource,
            @org.springframework.beans.factory.annotation.Value("${sql.script.directory:data/script}") String scriptDirectory) {
        this.dataSource = dataSource;
        this.scriptDirectory = Path.of(scriptDirectory).toAbsolutePath().normalize();
    }

    public void executeScript(DownloadedFileDto download) {
        validateDownloadedScript(download);
        executeDownloadedScript(download);
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

    private void validateDownloadedScript(DownloadedFileDto download) {
        if (download == null) {
            throw new IllegalArgumentException("Downloaded script must not be null");
        }

        if (download.getFileName() == null || download.getFileName().isBlank()) {
            throw new IllegalArgumentException("Downloaded script fileName is required");
        }

        if (!isSqlFileName(download.getFileName())) {
            throw new IllegalArgumentException("Only .sql files are allowed: " + download.getFileName());
        }

        if (download.getContent() == null || download.getContent().isBlank()) {
            throw new IllegalArgumentException("Downloaded script content must not be empty");
        }
    }

    private boolean isSqlFile(Path path) {
        return isSqlFileName(String.valueOf(path.getFileName()));
    }

    private boolean isSqlFileName(String fileName) {
        return fileName.length() >= 4
                && fileName.regionMatches(true, fileName.length() - 4, ".sql", 0, 4);
    }

    private void executeDownloadedScript(DownloadedFileDto download) {
        ByteArrayResource resource = new ByteArrayResource(
                download.getContent().getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return download.getFileName();
            }
        };

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(resource);
        populator.setContinueOnError(false);
        populator.execute(dataSource);
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
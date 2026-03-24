package com.executor.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Service;

@Service
public class LocalScriptStorageService {

    private static final Path SCRIPT_DIRECTORY = Paths.get("data", "script");

    public Path save( byte[] data) throws IOException {
        Files.createDirectories(SCRIPT_DIRECTORY);

        // Formatter : yyyyMMdd_HHmmss → 20260324_153045
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

        String timestamp = LocalDateTime.now().format(formatter);

        String filename = timestamp + ".sql";

        Path target = SCRIPT_DIRECTORY.resolve(filename).normalize();

        if (!target.startsWith(SCRIPT_DIRECTORY)) {
            throw new IllegalArgumentException("Invalid file path");
        }

        Files.write(target, data);

        return target;
    }
}
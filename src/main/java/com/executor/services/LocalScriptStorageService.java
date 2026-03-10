package com.executor.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.stereotype.Service;

@Service
public class LocalScriptStorageService {

    private static final Path SCRIPT_DIRECTORY = Paths.get("data", "script");

    public Path save(String remote, byte[] data) throws IOException {
        Files.createDirectories(SCRIPT_DIRECTORY);

        String filename = extractFilename(remote);
        Path target = SCRIPT_DIRECTORY.resolve(filename).normalize();

        if (!target.startsWith(SCRIPT_DIRECTORY)) {
            throw new IllegalArgumentException("Invalid file path");
        }

        Files.write(target, data);

        return target;
    }

    private String extractFilename(String remote) {
        String raw = remote.substring(remote.lastIndexOf('/') + 1);

        if (raw.isBlank()) {
            throw new IllegalArgumentException("Filename is missing in remote path");
        }

        String sanitized = raw.replace("\\", "_")
                .replace("/", "_")
                .replace("..", "_")
                .replace("\"", "");

        if (!sanitized.endsWith(".sql")) {
            sanitized = sanitized + ".sql";
        }

        return sanitized;
    }
}
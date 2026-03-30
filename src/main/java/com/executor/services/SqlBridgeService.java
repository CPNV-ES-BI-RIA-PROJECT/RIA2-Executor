package com.executor.services;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.executor.dto.DownloadedFileDto;
import com.executor.dto.ExecutedFile;
import com.executor.dto.RemoteFileDto;
import com.executor.repositories.ExecutionHistoryRepository;
import org.springframework.stereotype.Service;

@Service
public class SqlBridgeService {

    private final ExecutionHistoryRepository executionHistoryRepository;
    private final SqlExecutorService sqlExecutorService;
    private final BucketSqlBridgeClient bucketSqlBridgeClient;

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(SqlBridgeService.class);

    public SqlBridgeService(
            ExecutionHistoryRepository executionHistoryRepository,
            SqlExecutorService sqlExecutorService, BucketSqlBridgeClient bucketSqlBridgeClient
    ) {
        this.executionHistoryRepository = executionHistoryRepository;
        this.sqlExecutorService = sqlExecutorService;
        this.bucketSqlBridgeClient = bucketSqlBridgeClient;
    }

    public ExecutedFile runAllRecentFiles() throws IOException {
        DateTimeFormatter fileFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

        String timestamp = executionHistoryRepository.getLastTimestamp();
        LocalDateTime lastTimestamp = parseLastTimestamp(timestamp, fileFormatter);

        List<RemoteFileDto> recentFiles = bucketSqlBridgeClient.list().stream()
                .filter(file -> isRecentSqlFile(file, lastTimestamp, fileFormatter))
                .sorted(Comparator.comparing(file -> extractTimestamp(file.getFileName(), fileFormatter)))
                .toList();

        int filesGet = recentFiles.size();
        int filesExecuted = 0;
        List<String> filesNotExecuted = new ArrayList<>();
        LocalDateTime lastExecutedFileTimestamp = null;

        for (RemoteFileDto file : recentFiles) {
            try {
                DownloadedFileDto download = bucketSqlBridgeClient.download(file.getFileName());
                sqlExecutorService.executeScript(download);
                filesExecuted++;

                lastExecutedFileTimestamp = extractTimestamp(file.getFileName(), fileFormatter);
            } catch (Exception exception) {
                filesNotExecuted.add(file.getFileName());
            }
        }

        if (lastExecutedFileTimestamp != null) {
            executionHistoryRepository.setLastTimestamp(lastExecutedFileTimestamp.format(fileFormatter));
        }

        return new ExecutedFile(
                filesGet,
                filesExecuted,
                filesNotExecuted
        );
    }

    private boolean isRecentSqlFile(
            RemoteFileDto file,
            LocalDateTime lastTimestamp,
            DateTimeFormatter formatter
    ) {
        String fileName = file.getFileName();

        if (fileName == null || !fileName.endsWith(".sql")) {
            return false;
        }

        try {
            LocalDateTime fileTimestamp = extractTimestamp(fileName, formatter);
            return fileTimestamp.isAfter(lastTimestamp);
        } catch (Exception exception) {
            return false;
        }
    }

    private LocalDateTime extractTimestamp(String fileName, DateTimeFormatter formatter) {
        String datePart = fileName.substring(0, fileName.length() - 4);
        return LocalDateTime.parse(datePart, formatter);
    }

    private LocalDateTime parseLastTimestamp(String timestamp, DateTimeFormatter formatter) {
        if (timestamp == null || timestamp.isBlank()) {
            return LocalDateTime.MIN;
        }

        return LocalDateTime.parse(timestamp, formatter);
    }
}

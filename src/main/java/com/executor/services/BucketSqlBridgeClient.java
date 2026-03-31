package com.executor.services;

import com.executor.dto.DownloadedFileDto;
import com.executor.dto.RemoteFileDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Service
public class BucketSqlBridgeClient {

    private final RestClient restClient;
    private final String remotePath;

    public BucketSqlBridgeClient(
            @Value("${bucket.sql-bridge.url}") String baseUrl,
            @Value("${bucket.sql-bridge.remote}") String remotePath
    ) {
        this.remotePath = remotePath;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public List<RemoteFileDto> list() {
        String uri = UriComponentsBuilder
                .fromPath("/api/v1/objects")
                .queryParam("remote", remotePath)
                .queryParam("recursive", true)
                .build()
                .toUriString();

        List<String> files = restClient.get()
                .uri(uri)
                .retrieve()
                .body(new ParameterizedTypeReference<List<String>>() {});

        if (files == null) {
            return List.of();
        }

        return files.stream()
                .map(this::toRemoteFileDto)
                .toList();
    }

    public DownloadedFileDto download(String fileName) {
        String fullRemote = buildFullRemote(fileName);

        String uri = UriComponentsBuilder
                .fromPath("/api/v1/objects/download")
                .queryParam("remote", fullRemote)
                .build()
                .toUriString();

        String content = restClient.get()
                .uri(uri)
                .accept(MediaType.TEXT_PLAIN, MediaType.APPLICATION_OCTET_STREAM)
                .retrieve()
                .body(String.class);

        return new DownloadedFileDto(fileName, content);
    }

    private RemoteFileDto toRemoteFileDto(String returnedPath) {
        String normalizedPath = returnedPath == null ? "" : returnedPath.trim();

        int lastSlashIndex = normalizedPath.lastIndexOf('/');
        String fileName = lastSlashIndex >= 0
                ? normalizedPath.substring(lastSlashIndex + 1)
                : normalizedPath;

        return new RemoteFileDto(
                normalizedPath,
                fileName,
                0L
        );
    }

    private String buildFullRemote(String fileName) {
        String normalizedRemotePath = remotePath.endsWith("/")
                ? remotePath.substring(0, remotePath.length() - 1)
                : remotePath;

        return normalizedRemotePath + "/" + fileName;
    }
}
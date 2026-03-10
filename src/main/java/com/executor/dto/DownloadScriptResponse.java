package com.executor.dto;

public record DownloadScriptResponse(
        String remote,
        String filename,
        String localPath
) {
}
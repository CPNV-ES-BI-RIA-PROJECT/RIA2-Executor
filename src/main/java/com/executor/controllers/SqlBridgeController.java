package com.executor.controllers;

import com.executor.dto.ImportFromUrlRequest;
import com.executor.services.LocalScriptStorageService;
import com.executor.services.UrlDownloadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;

public class SqlBridgeController {

    private final UrlDownloadService urlDownloadService;
    private final LocalScriptStorageService localScriptStorageService;

    public SqlBridgeController(
            UrlDownloadService urlDownloadService, LocalScriptStorageService localScriptStorageService
    ) {
        this.urlDownloadService = urlDownloadService;
        this.localScriptStorageService = localScriptStorageService;
    }

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(SqlBridgeController.class);

    @Operation(
            summary = "Import an object from a shared URL",
            description = "Downloads a file from an HTTP(S) URL, store in local applications"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Object imported"),
            @ApiResponse(responseCode = "400", description = "Invalid URL"),
            @ApiResponse(responseCode = "500", description = "Internal error")
    })
    @PostMapping(value = "/import", consumes = MediaType.APPLICATION_JSON_VALUE, params = "remote")
    @ResponseStatus(HttpStatus.CREATED)
    public ImportResult importFromUrl(@RequestBody ImportFromUrlRequest body) {
        try {
            var downloaded = urlDownloadService.fetch(body.url());

            Path savedPath = localScriptStorageService.save(downloaded.bytes());

            return new ImportResult(savedPath.toString());

        } catch (ResponseStatusException e) {
            log.error("Import failed with status={}", e.getStatusCode(), e);
            throw e;
        } catch (IllegalArgumentException e) {
            log.error("Import bad request", e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Import unexpected error", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Import failed", e);
        }
    }

    public record ImportResult(String path) {}
}

package com.executor.controllers;

import com.executor.dto.ExecutedFile;
import com.executor.services.SqlBridgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/v1/objects")
public class SqlBridgeController {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(SqlBridgeController.class);

    private final SqlBridgeService sqlBridgeService;

    public SqlBridgeController(SqlBridgeService sqlBridgeService) {
        this.sqlBridgeService = sqlBridgeService;
    }

    @Operation(
            summary = "Execute recent sql files",
            description = "Check if new files are in the bucket and execute them on the data warehouse"
    )
    @ApiResponses({
            // à compléter
    })
    @PostMapping(
            value = "/recent-files/executions",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ExecutedFile> recentFilesExecutions() throws IOException {
        ExecutedFile result = sqlBridgeService.runAllRecentFiles();

        log.info(
                "Recent files execution finished. filesGet={}, filesExecuted={}, filesNotExecuted={}",
                result.filesGet(),
                result.filesExecuted(),
                result.filesNotExecuted()
        );

        return ResponseEntity.ok(result);
    }
}

package com.executor.controllers;

import com.executor.services.SqlExecutorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1")
public class SqlScriptExecutorController {

    private final SqlExecutorService sqlExecutorService;

    public SqlScriptExecutorController(SqlExecutorService sqlExecutorService) {
        this.sqlExecutorService = sqlExecutorService;
    }

    @PostMapping(value = "/execute-script", params = "localPath")
    public ResponseEntity<String> executeScript(@RequestParam String localPath) {
        sqlExecutorService.executeScript(localPath);
        return ResponseEntity.ok("Script SQL exécuté avec succès");
    }
}

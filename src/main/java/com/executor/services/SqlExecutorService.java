package com.executor.services;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Service;

@Service
public class SqlExecutorService {

    private final DataSource dataSource;

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String jdbcUser;

    public SqlExecutorService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void executeScript(String scriptPath) {
        Path path = Path.of(scriptPath).toAbsolutePath().normalize();

        System.out.println("Executing script: " + path);
        System.out.println("Exists: " + Files.exists(path));
        System.out.println("JDBC URL: " + jdbcUrl);
        System.out.println("JDBC USER: " + jdbcUser);

        if (!Files.exists(path)) {
            throw new IllegalArgumentException("SQL script not found: " + path);
        }

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new FileSystemResource(path));
        populator.setContinueOnError(false);
        populator.execute(dataSource);
    }
}
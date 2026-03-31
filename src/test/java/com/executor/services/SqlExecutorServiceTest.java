package com.executor.services;

import com.executor.dto.DownloadedFileDto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqlExecutorServiceTest {

    @TempDir
    Path tempDirectory;

    @Test
    void executeScriptRunsDownloadedSqlContentSuccessfully() throws Exception {
        SqlExecutorService service = new SqlExecutorService(
                mockDataSource(false, false),
                tempDirectory.toString());

        service.executeScript(new DownloadedFileDto(
                "01-first.sql",
                "INSERT INTO sample VALUES (1);"));
    }

    @Test
    void executeScriptPropagatesFailureWhenExecutionFails() throws Exception {
        SqlExecutorService service = new SqlExecutorService(
                mockDataSource(true, false),
                tempDirectory.toString());

        assertThrows(RuntimeException.class, () -> service.executeScript(new DownloadedFileDto(
                "01-first.sql",
                "INSERT INTO sample VALUES (1);")));
    }

    @Test
    void executeAllScriptsDeletesEachFileImmediatelyAfterSuccess() throws Exception {
        Path scriptDirectory = Files.createDirectories(tempDirectory.resolve("script"));
        Path firstScript = Files.writeString(
                scriptDirectory.resolve("01-first.sql"),
                "INSERT INTO sample VALUES (1);");
        Path secondScript = Files.writeString(
                scriptDirectory.resolve("02-second.sql"),
                "INSERT INTO sample VALUES (2);");
        Path thirdScript = Files.writeString(
                scriptDirectory.resolve("03-third.sql"),
                "INSERT INTO sample VALUES (3);");

        SqlExecutorService service = new SqlExecutorService(
                mockDataSource(false, true),
                scriptDirectory.toString());

        assertThrows(RuntimeException.class, service::executeAllScripts);
        assertFalse(Files.exists(firstScript));
        assertTrue(Files.exists(secondScript));
        assertTrue(Files.exists(thirdScript));
    }

    @Test
    void executeAllScriptsRejectsSymbolicLinks() throws Exception {
        Path scriptDirectory = Files.createDirectories(tempDirectory.resolve("script"));
        Path externalScript = Files.writeString(
                tempDirectory.resolve("outside.sql"),
                "INSERT INTO sample VALUES (99);");
        Files.createSymbolicLink(scriptDirectory.resolve("01-link.sql"), externalScript);

        SqlExecutorService service = new SqlExecutorService(
                mockDataSource(false, false),
                scriptDirectory.toString());

        assertThrows(IllegalArgumentException.class, service::executeAllScripts);
    }

    @Test
    void executeScriptRejectsNonSqlFileName() throws Exception {
        SqlExecutorService service = new SqlExecutorService(
                mockDataSource(false, false),
                tempDirectory.toString());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.executeScript(new DownloadedFileDto(
                        "outside.txt",
                        "INSERT INTO sample VALUES (99);")));
    }

    private DataSource mockDataSource(boolean failFirstStatement, boolean failSecondStatement)
            throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        Statement firstStatement = mock(Statement.class);
        Statement secondStatement = mock(Statement.class);
        Statement thirdStatement = mock(Statement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        when(connection.createStatement()).thenReturn(firstStatement, secondStatement, thirdStatement);
        when(firstStatement.getWarnings()).thenReturn(null);
        when(secondStatement.getWarnings()).thenReturn(null);
        when(thirdStatement.getWarnings()).thenReturn(null);

        if (failFirstStatement) {
            doThrow(new SQLException("boom-first"))
                    .when(firstStatement)
                    .execute(anyString());
        } else {
            when(firstStatement.execute(anyString())).thenReturn(true);
        }

        if (failSecondStatement) {
            doThrow(new SQLException("boom-second"))
                    .when(secondStatement)
                    .execute(anyString());
        } else {
            when(secondStatement.execute(anyString())).thenReturn(true);
        }

        when(thirdStatement.execute(anyString())).thenReturn(true);

        return dataSource;
    }
}

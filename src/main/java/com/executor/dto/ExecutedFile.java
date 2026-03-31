package com.executor.dto;

import java.util.List;

public record ExecutedFile(
        int filesGet,
        int filesExecuted,
        List<String> filesNotExecuted
) {
}
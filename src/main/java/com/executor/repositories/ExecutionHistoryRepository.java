package com.executor.repositories;

import org.springframework.stereotype.Repository;

@Repository
public class ExecutionHistoryRepository {

    private String lastTimestamp = "19700101_000000";

    public String getLastTimestamp() {
        return lastTimestamp;
    }

    public void setLastTimestamp(String newTimestamp) {
        this.lastTimestamp = newTimestamp;
    }
}
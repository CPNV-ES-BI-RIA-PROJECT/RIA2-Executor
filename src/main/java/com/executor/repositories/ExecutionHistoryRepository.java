package com.executor.repositories;

import org.springframework.stereotype.Repository;

@Repository
public class ExecutionHistoryRepository {

    private String lastTimestamp = "00000000000";

    public String getLastTimestamp() {
        return lastTimestamp;
    }

    public void setLastTimestamp(String newTimestamp) {
        this.lastTimestamp = newTimestamp;
    }
}
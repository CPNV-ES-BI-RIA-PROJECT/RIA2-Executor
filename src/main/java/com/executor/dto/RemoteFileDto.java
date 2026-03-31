package com.executor.dto;

public class RemoteFileDto {

    private String key;
    private String fileName;
    private long size;

    public RemoteFileDto(String key, String fileName, long size) {
        this.key = key;
        this.fileName = fileName;
        this.size = size;
    }

    public String getFileName() {
        return fileName;
    }

}

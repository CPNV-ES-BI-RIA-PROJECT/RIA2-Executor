package com.executor.dto;

public class RemoteFileDto {

    private String key;
    private String fileName;
    private long size;

    public RemoteFileDto() {
    }

    public RemoteFileDto(String key, String fileName, long size) {
        this.key = key;
        this.fileName = fileName;
        this.size = size;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}

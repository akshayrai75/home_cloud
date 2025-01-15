package com.awesomeapps.homeCloud.models;

public class FileDto {
    private String name;
    private String type; // file or directory
    private long size;
    private String creationDate;

    public FileDto(String name, String type, long size, String creationDate) {
        this.name = name;
        this.type = type;
        this.size = size;
        this.creationDate = creationDate;
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }
}


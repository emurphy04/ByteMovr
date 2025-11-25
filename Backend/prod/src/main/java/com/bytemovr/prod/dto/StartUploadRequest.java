package com.bytemovr.prod.dto;

public class StartUploadRequest {
    private String fileName;
    private Long sizeBytes;
    private String contentType;

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
}


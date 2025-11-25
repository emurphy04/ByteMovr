package com.bytemovr.prod.dto;

public class StartUploadResponse {
    private String id;
    private String uploadId;
    private long partSizeBytes;
    private String expiresAt;
    private String downloadId;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUploadId() { return uploadId; }
    public void setUploadId(String uploadId) { this.uploadId = uploadId; }

    public long getPartSizeBytes() { return partSizeBytes; }
    public void setPartSizeBytes(long partSizeBytes) { this.partSizeBytes = partSizeBytes; }

    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }

    public String getDownloadId() { return downloadId; }
    public void setDownloadId(String downloadId) { this.downloadId = downloadId; }
}


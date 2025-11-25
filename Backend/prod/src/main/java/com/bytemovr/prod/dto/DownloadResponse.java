package com.bytemovr.prod.dto;

public class DownloadResponse {
    private String downloadUrl;
    private String expiresAt;

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

    public String getExpiresAt() { return expiresAt; }
    public void setExpiresAt(String expiresAt) { this.expiresAt = expiresAt; }
}


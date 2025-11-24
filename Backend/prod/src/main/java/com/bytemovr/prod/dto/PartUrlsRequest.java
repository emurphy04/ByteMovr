package com.bytemovr.prod.dto;

import java.util.List;

public class PartUrlsRequest {
    private String uploadId;
    private List<Integer> parts;

    public String getUploadId() { return uploadId; }
    public void setUploadId(String uploadId) { this.uploadId = uploadId; }

    public List<Integer> getParts() { return parts; }
    public void setParts(List<Integer> parts) { this.parts = parts; }
}


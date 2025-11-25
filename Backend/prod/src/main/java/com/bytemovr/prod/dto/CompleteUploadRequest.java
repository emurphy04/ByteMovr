package com.bytemovr.prod.dto;

import java.util.List;

public class CompleteUploadRequest {

    public static class Part {
        private int partNumber;
        private String etag;

        public int getPartNumber() { return partNumber; }
        public void setPartNumber(int partNumber) { this.partNumber = partNumber; }

        public String getEtag() { return etag; }
        public void setEtag(String etag) { this.etag = etag; }
    }

    private String uploadId;
    private List<Part> parts;

    public String getUploadId() { return uploadId; }
    public void setUploadId(String uploadId) { this.uploadId = uploadId; }

    public List<Part> getParts() { return parts; }
    public void setParts(List<Part> parts) { this.parts = parts; }
}


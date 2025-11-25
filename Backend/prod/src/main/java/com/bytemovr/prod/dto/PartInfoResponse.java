package com.bytemovr.prod.dto;


public class PartInfoResponse {
    private int partNumber;
    private String uploadUrl;

    public int getPartNumber() { return partNumber; }
    public void setPartNumber(int partNumber) { this.partNumber = partNumber; }

    public String getUploadUrl() { return uploadUrl; }
    public void setUploadUrl(String uploadUrl) { this.uploadUrl = uploadUrl; }
}


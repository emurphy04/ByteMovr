package com.bytemovr.prod.dto;

import java.util.List;

public class PartUrlsResponse {
    private List<PartInfoResponse> parts;

    public List<PartInfoResponse> getParts() { return parts; }
    public void setParts(List<PartInfoResponse> parts) { this.parts = parts; }
}

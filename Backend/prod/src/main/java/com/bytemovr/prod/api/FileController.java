package com.bytemovr.prod.api;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.UpdateItemSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CompleteMultipartUploadRequest;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadRequest;
import com.amazonaws.services.s3.model.InitiateMultipartUploadResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PartETag;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.bytemovr.prod.dto.CompleteUploadRequest;
import com.bytemovr.prod.dto.CompleteUploadResponse;
import com.bytemovr.prod.dto.PartInfoResponse;
import com.bytemovr.prod.dto.PartUrlsRequest;
import com.bytemovr.prod.dto.PartUrlsResponse;
import com.bytemovr.prod.dto.StartUploadRequest;
import com.bytemovr.prod.dto.StartUploadResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
public class FileController {

    private final AmazonS3 s3;
    private final Table table;

    private final String bucket;
    private final int downloadTtlHours;
    private final int downloadsAllowed;

    public FileController(
            AmazonS3 s3,
            DynamoDB dynamoDB,
            @Value("${bytemovr.table}") String tableName,
            @Value("${bytemovr.bucket}") String bucket,
            @Value("${bytemovr.downloadTtlHours:24}") int downloadTtlHours,
            @Value("${bytemovr.downloadsAllowed:1}") int downloadsAllowed
    ) {
        this.s3 = s3;
        this.table = dynamoDB.getTable(tableName);
        this.bucket = bucket;
        this.downloadTtlHours = downloadTtlHours;
        this.downloadsAllowed = downloadsAllowed;
    }

    // ---------------------------------------------------------
    // 1) START MULTIPART UPLOAD  (POST /api/files)
    // ---------------------------------------------------------

    @PostMapping("/api/files")
    public StartUploadResponse startUpload(@RequestBody StartUploadRequest req) {

        String fileName = req.getFileName() != null ? req.getFileName() : "file";
        String contentType = req.getContentType() != null ? req.getContentType() : "application/octet-stream";
        long sizeBytes = req.getSizeBytes() != null ? req.getSizeBytes() : 0L;

        String id = UUID.randomUUID().toString();
        String s3Key = "uploads/" + id;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);

        InitiateMultipartUploadRequest initReq =
                new InitiateMultipartUploadRequest(bucket, s3Key)
                        .withObjectMetadata(metadata);

        InitiateMultipartUploadResult initRes = s3.initiateMultipartUpload(initReq);
        String uploadId = initRes.getUploadId();

        Instant now = Instant.now();
        Instant expiresAt = now.plus(downloadTtlHours, ChronoUnit.HOURS);

        Item item = new Item()
                .withPrimaryKey("id", id)
                .withString("s3Key", s3Key)
                .withString("uploadId", uploadId)
                .withString("status", "UPLOADING")
                .withString("fileName", fileName)
                .withString("contentType", contentType)
                .withNumber("sizeBytes", sizeBytes)
                .withNumber("downloadsRemaining", downloadsAllowed)
                .withString("createdAt", now.toString())
                .withString("expiresAt", expiresAt.toString());

        table.putItem(item);

        long partSizeBytes = 100L * 1024 * 1024; // 100MB parts

        StartUploadResponse resp = new StartUploadResponse();
        resp.setId(id);
        resp.setUploadId(uploadId);
        resp.setPartSizeBytes(partSizeBytes);
        resp.setExpiresAt(expiresAt.toString());
        resp.setDownloadId(id); // for now, same as id

        return resp;
    }

    // ---------------------------------------------------------
    // 2) GET PRESIGNED URLS FOR PARTS  (POST /api/files/{id}/parts)
    // ---------------------------------------------------------

    @PostMapping("/api/files/{id}/parts")
    public PartUrlsResponse createPartUrls(@PathVariable String id,
                                           @RequestBody PartUrlsRequest req) {
        Item item = table.getItem("id", id);
        if (item == null) {
            throw new RuntimeException("File not found");
        }

        String s3Key = item.getString("s3Key");
        String storedUploadId = item.getString("uploadId");

        if (req.getUploadId() == null || !req.getUploadId().equals(storedUploadId)) {
            throw new RuntimeException("Invalid uploadId");
        }

        List<Integer> partNumbers = req.getParts();
        if (partNumbers == null || partNumbers.isEmpty()) {
            throw new RuntimeException("No parts requested");
        }

        Date expiration = Date.from(Instant.now().plus(1, ChronoUnit.HOURS));
        List<PartInfoResponse> partInfos = new ArrayList<>();

        for (Integer partNumber : partNumbers) {
            GeneratePresignedUrlRequest presignReq =
                    new GeneratePresignedUrlRequest(bucket, s3Key)
                            .withMethod(HttpMethod.PUT)
                            .withExpiration(expiration);

            presignReq.addRequestParameter("partNumber", String.valueOf(partNumber));
            presignReq.addRequestParameter("uploadId", req.getUploadId());

            URL url = s3.generatePresignedUrl(presignReq);

            PartInfoResponse p = new PartInfoResponse();
            p.setPartNumber(partNumber);
            p.setUploadUrl(url.toString());
            partInfos.add(p);
        }

        PartUrlsResponse resp = new PartUrlsResponse();
        resp.setParts(partInfos);
        return resp;
    }

    // ---------------------------------------------------------
    // 3) COMPLETE MULTIPART UPLOAD  (POST /api/files/{id}/complete)
    // ---------------------------------------------------------

    @PostMapping("/api/files/{id}/complete")
    public CompleteUploadResponse completeUpload(@PathVariable String id,
                                                 @RequestBody CompleteUploadRequest req) {
        Item item = table.getItem("id", id);
        if (item == null) {
            throw new RuntimeException("File not found");
        }

        String s3Key = item.getString("s3Key");
        String storedUploadId = item.getString("uploadId");

        if (req.getUploadId() == null || !req.getUploadId().equals(storedUploadId)) {
            throw new RuntimeException("Invalid uploadId");
        }

        List<PartETag> partETags = new ArrayList<>();
        for (CompleteUploadRequest.Part part : req.getParts()) {
            partETags.add(new PartETag(part.getPartNumber(), part.getEtag()));
        }

        CompleteMultipartUploadRequest compReq =
                new CompleteMultipartUploadRequest(bucket, s3Key, req.getUploadId(), partETags);

        s3.completeMultipartUpload(compReq);

        UpdateItemSpec update = new UpdateItemSpec()
                .withPrimaryKey("id", id)
                .withUpdateExpression("set #st = :ready")
                .withNameMap(new HashMap<String, String>() {{
                    put("#st", "status");
                }})
                .withValueMap(new ValueMap().withString(":ready", "READY"));

        table.updateItem(update);

        CompleteUploadResponse resp = new CompleteUploadResponse();
        resp.setId(id);
        resp.setDownloadId(id);
        return resp;
    }

    // ---------------------------------------------------------
    // 4) DIRECT DOWNLOAD ENDPOINT  (GET /d/{id})
    //    - Streams file bytes from S3
    //    - Enforces one-time download + expiry
    //    - No JSON, no fetch required: just navigate to /d/{id}
    // ---------------------------------------------------------

    @GetMapping("/d/{id}")
    public ResponseEntity<StreamingResponseBody> downloadFile(@PathVariable String id) {

        Item item = table.getItem("id", id);
        if (item == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        String status = item.getString("status");
        int downloadsRemaining = item.getInt("downloadsRemaining");
        String expiresAtStr = item.getString("expiresAt");

        Instant now = Instant.now();
        Instant expiresAt = Instant.parse(expiresAtStr);

        if (!"READY".equals(status) || downloadsRemaining <= 0 || now.isAfter(expiresAt)) {
            return ResponseEntity.status(HttpStatus.GONE).build();
        }

        String s3Key = item.getString("s3Key");
        String fileName = item.getString("fileName");
        String contentType = item.getString("contentType");

        S3Object s3Object = s3.getObject(bucket, s3Key);
        S3ObjectInputStream s3InputStream = s3Object.getObjectContent();

        int newRemaining = downloadsRemaining - 1;
        String newStatus = newRemaining <= 0 ? "CONSUMED" : "READY";

        UpdateItemSpec update = new UpdateItemSpec()
                .withPrimaryKey("id", id)
                .withUpdateExpression("set downloadsRemaining = :dr, #st = :st")
                .withNameMap(new HashMap<String, String>() {{
                    put("#st", "status");
                }})
                .withValueMap(new ValueMap()
                        .withNumber(":dr", newRemaining)
                        .withString(":st", newStatus));

        table.updateItem(update);

        StreamingResponseBody body = outputStream -> {
            try (InputStream in = s3InputStream) {
                byte[] buffer = new byte[8192];
                int n;
                while ((n = in.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, n);
                }
            }
        };

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE));
        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(fileName != null ? fileName : "file", StandardCharsets.UTF_8)
                        .build()
        );

        return new ResponseEntity<>(body, headers, HttpStatus.OK);
    }
}

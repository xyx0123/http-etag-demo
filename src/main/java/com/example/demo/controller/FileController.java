package com.example.demo.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Files;

/**
 * test
 */
@RestController
@RequestMapping("/api")
public class FileController {

    // ZIP file path
    private static final String FILE_NAME = "test.zip";

    @GetMapping("/file")
    @Operation(summary = "Download ZIP file", description = "Returns ZIP file content and supports ETag caching. If the client provides If-None-Match and it matches, returns 304 Not Modified.")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "ZIP file download successful",
                    content = @Content(
                            mediaType = "application/zip",
                            schema = @Schema(type = "string", format = "binary")
                    ),
                    headers = @Header(name = "ETag", description = "File version identifier for caching control")
            ),
            @ApiResponse(
                    responseCode = "304",
                    description = "File not modified; client cache is valid"
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "File not found"
            )
    })
    public ResponseEntity<byte[]> downloadFile(
            @Parameter(description = "Client-provided ETag used to determine if the file has changed", required = false)
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch
    ) throws Exception {

        ClassPathResource resource = new ClassPathResource(FILE_NAME);

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        File file = resource.getFile();

        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        // Generate ETag (file length + last modified timestamp)
        String etag = generateETag(file);

        // Check client cache
        if (matches(ifNoneMatch, etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag)
                    .build();
        }

        // Read file bytes
        byte[] data = Files.readAllBytes(file.toPath());

        return ResponseEntity.ok()
                .eTag(etag)
                .contentType(MediaType.APPLICATION_OCTET_STREAM) // ZIP file type
                .body(data);
    }

    /**
     * Generate strong ETag (not quoted)
     */
    private String generateETag(File file) {
        return file.length() + "-" + file.lastModified();
    }

    /**
     * Check If-None-Match header against ETag
     */
    private boolean matches(String ifNoneMatch, String etag) {
        if (ifNoneMatch == null || ifNoneMatch.isBlank()) {
            return false;
        }

        String[] clientEtags = ifNoneMatch.split(",");
        for (String client : clientEtags) {
            String normalized = client.trim();
            if (normalized.startsWith("\"") && normalized.endsWith("\"")) {
                normalized = normalized.substring(1, normalized.length() - 1);
            }
            if (normalized.equals(etag)) {
                return true;
            }
        }
        return false;
    }
}

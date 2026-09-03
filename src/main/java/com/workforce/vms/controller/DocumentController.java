package com.workforce.vms.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.nio.file.Files;

/** Worker document / attachment download. */
@RestController
public class DocumentController {

    /** Base directory where worker attachments (contracts, I-9s, etc.) live. */
    private static final String DOCS_ROOT = "/app/documents";

    /**
     * PATH TRAVERSAL.
     * GET /api/documents/download?file=contract-1001.txt
     * The filename is joined to the docs root with no sanitization, so
     * "../../etc/passwd" escapes the intended directory.
     */
    @GetMapping("/api/documents/download")
    public ResponseEntity<byte[]> download(@RequestParam("file") String file) throws Exception {
        // DEMO-VULN: Path Traversal (CWE-22). Unsanitized filename joined to a base dir.
        File target = new File(DOCS_ROOT + "/" + file);
        if (!target.exists() || target.isDirectory()) {
            return ResponseEntity.notFound().build();
        }
        byte[] data = Files.readAllBytes(target.toPath());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + target.getName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }
}

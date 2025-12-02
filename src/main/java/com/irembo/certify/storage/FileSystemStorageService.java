package com.irembo.certify.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class FileSystemStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileSystemStorageService.class);

    private final Path rootPath;

    public FileSystemStorageService(@Value("${certify.storage.base-path:storage}") String basePath) {
        this.rootPath = Paths.get(basePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootPath);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create storage directory " + this.rootPath, e);
        }
    }

    /**
     * Persist the certificate PDF to disk and return a tenant-relative storage path,
     * e.g. {@code tenantId/certificateId.pdf}. The returned value is what we store
     * on the {@code Certificate} entity.
     */
    public String save(UUID tenantId, UUID certificateId, byte[] pdfBytes) {
        try {
            Path tenantDir = rootPath.resolve(tenantId.toString());
            Files.createDirectories(tenantDir);

            Path file = tenantDir.resolve(certificateId.toString() + ".pdf");
            Files.write(file, pdfBytes);

            String relativePath = rootPath.relativize(file).toString().replace('\\', '/');
            log.debug("Stored certificate PDF at {}", file);
            return relativePath;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store certificate PDF", e);
        }
    }

    public byte[] load(String storagePath) {
        try {
            Path file = rootPath.resolve(storagePath).normalize();
            if (!Files.exists(file)) {
                throw new IllegalStateException("Stored certificate not found at " + file);
            }
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read stored certificate PDF", e);
        }
    }
}

package com.superdoc.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileService {

    private final Path uploadPath;

    public FileService(@Value("${app.upload.path:uploads}") String uploadPath) {
        this.uploadPath = Paths.get(uploadPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    public String saveFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Validate file size (5MB max)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds 5MB");
        }

        // Validate file type (images only)
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }

        // Generate unique filename
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String uniqueFileName = UUID.randomUUID().toString() + extension;

        // Save file
        Path targetLocation = this.uploadPath.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        return uniqueFileName;
    }

    public Path getFile(String fileName) {
        Path filePath = this.uploadPath.resolve(fileName).normalize();
        
        // Security check: ensure file is within upload directory
        if (!filePath.startsWith(this.uploadPath)) {
            throw new IllegalArgumentException("Invalid file path");
        }
        
        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException("File not found");
        }
        
        return filePath;
    }

    public void deleteFile(String fileName) throws IOException {
        Path filePath = this.uploadPath.resolve(fileName).normalize();
        
        // Security check
        if (!filePath.startsWith(this.uploadPath)) {
            throw new IllegalArgumentException("Invalid file path");
        }
        
        if (Files.exists(filePath)) {
            Files.delete(filePath);
        }
    }
}


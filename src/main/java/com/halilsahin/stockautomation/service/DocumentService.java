package com.halilsahin.stockautomation.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class DocumentService {
    private static final String UPLOAD_DIR = "src/main/resources/images/";

    public String saveDocument(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("Dosya boş!");
        }

        File uploadDir = new File(UPLOAD_DIR);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }

        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(UPLOAD_DIR, fileName);
        Files.write(filePath, file.getBytes());

        return filePath.toString();
    }
}

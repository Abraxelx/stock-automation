package com.halilsahin.stockautomation.service;

import com.halilsahin.stockautomation.entity.Document;
import com.halilsahin.stockautomation.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class DocumentService {
    private final DocumentRepository documentRepository;
    private static final String UPLOAD_DIR = "src/main/resources/static/images/";

    public DocumentService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

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

        return "/images/" + fileName;
    }

    public Document findById(Long id) {
        return documentRepository.findById(id).orElse(null);

    }

    public void deleteDocument(Long id) {
        Document document = findById(id);
        if (document != null) {
            try {
                // Dosyayı fiziksel olarak sil
                Path filePath = Paths.get("src/main/resources/static" + document.getFilePath());
                Files.deleteIfExists(filePath);
                
                // Veritabanından sil
                documentRepository.deleteById(id);
            } catch (IOException e) {
                throw new RuntimeException("Belge silinirken hata oluştu", e);
            }
        }
    }
}

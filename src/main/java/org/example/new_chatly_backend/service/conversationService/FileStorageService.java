package org.example.new_chatly_backend.service.conversationService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    public String upload(MultipartFile file, String requestBaseUrl) {
        try {
            String original = file.getOriginalFilename() == null
                    ? "image"
                    : Path.of(file.getOriginalFilename()).getFileName().toString();

            String sanitized = original.replaceAll("[^a-zA-Z0-9._-]", "");

            String uniqueName = System.currentTimeMillis() + "_"
                    + UUID.randomUUID().toString().substring(0, 8)
                    + "_" + sanitized;

            Path profileFolder = Paths.get(uploadDir).resolve("profile");
            Files.createDirectories(profileFolder);

            Path destination = profileFolder.resolve(uniqueName);
            Files.copy(file.getInputStream(), destination);

            String baseUrl = requestBaseUrl.replaceAll("/$", "");
            return baseUrl + "/uploads/profile/" + uniqueName;

        } catch (Exception e) {
            throw new RuntimeException("File upload failed: " + e.getMessage());
        }
    }
}

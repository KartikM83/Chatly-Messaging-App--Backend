package org.example.new_chatly_backend.service.profileSetupService;

import lombok.RequiredArgsConstructor;
import org.example.new_chatly_backend.dto.profileSetupDTO.ProfileSetupDTO;
import org.example.new_chatly_backend.entity.userEntity.UserEntity;
import org.example.new_chatly_backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileSetupServiceImpl implements ProfileSetupService {

    private final UserRepository userRepo;

    /**
     * Base folder where uploads are stored. Default "uploads" if not configured.
     * final file path will be: {uploadDir}/profile/{filename}
     */
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /**
     * Max upload size in bytes (default 5MB)
     */
    @Value("${app.upload.max-size-bytes:5242880}")
    private long maxFileSize;

    @Override
    public ProfileSetupDTO saveProfile(ProfileSetupDTO dto, MultipartFile file, String requestBaseUrl) throws IOException {
        if (dto == null) throw new IllegalArgumentException("ProfileSetupDTO cannot be null");

        String phone = dto.getPhoneNumber();
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("phoneNumber is required (from token or DTO)");
        }

        UserEntity user = userRepo.findByPhoneNumber(phone)
                .orElseGet(() -> {
                    UserEntity newUser = new UserEntity();
                    newUser.setPhoneNumber(phone);
                    return newUser;
                });

        // Update textual fields
        if (dto.getName() != null) user.setName(dto.getName());
        if (dto.getBio() != null) user.setBio(dto.getBio());

        // If file present -> validate & store
        if (file != null && !file.isEmpty()) {

            // validate content type - only images allowed
            String contentType = file.getContentType();
            if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
                throw new IllegalArgumentException("Only image files are allowed");
            }

            // size check
            if (file.getSize() > maxFileSize) {
                throw new IllegalArgumentException("File too large. Max allowed bytes: " + maxFileSize);
            }

            // sanitize original filename and create unique name
            String original = file.getOriginalFilename() == null ? "image" : Path.of(file.getOriginalFilename()).getFileName().toString();
            String sanitized = original.replaceAll("[^a-zA-Z0-9._-]", "");
            String uniqueName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + "_" + sanitized;

            Path profileFolder = Paths.get(uploadDir).resolve("profile");
            Files.createDirectories(profileFolder);

            Path destination = profileFolder.resolve(uniqueName);
            try (var in = file.getInputStream()) {
                Files.copy(in, destination);
            }

            // Build accessible URL: requestBaseUrl + /uploads/profile/{filename}
            String baseUrl = requestBaseUrl != null ? requestBaseUrl.replaceAll("/$", "") : "";
            String imageUrl = baseUrl + "/uploads/profile/" + uniqueName;

            // Save URL to entity
            user.setProfileImage(imageUrl);
        }

        // persist
        UserEntity saved = userRepo.save(user);

        // prepare response DTO
        ProfileSetupDTO response = new ProfileSetupDTO();
        response.setId(saved.getId());
        response.setPhoneNumber(saved.getPhoneNumber());
        response.setName(saved.getName());
        response.setBio(saved.getBio());
        response.setProfileImage(saved.getProfileImage());

        return response;
    }
}

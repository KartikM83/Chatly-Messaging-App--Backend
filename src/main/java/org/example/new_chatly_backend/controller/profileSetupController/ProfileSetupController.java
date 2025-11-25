package org.example.new_chatly_backend.controller.profileSetupController;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.new_chatly_backend.dto.profileSetupDTO.ProfileSetupDTO;
import org.example.new_chatly_backend.service.profileSetupService.ProfileSetupService;
import org.example.new_chatly_backend.utility.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ProfileSetupController {

    private final ProfileSetupService profileSetupService;
    private final HttpServletRequest servletRequest;

    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();             // http
        String serverName = request.getServerName();     // localhost or domain
        int serverPort = request.getServerPort();        // 8080
        String contextPath = request.getContextPath();   // usually empty

        String port = "";
        if (!((scheme.equals("http") && serverPort == 80) || (scheme.equals("https") && serverPort == 443))) {
            port = ":" + serverPort;
        }

        return scheme + "://" + serverName + port + contextPath;
    }

    @PostMapping(value = "/profile-setup", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadProfile(
            @RequestPart(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "bio", required = false) String bio,
            HttpServletRequest request
    ) {
        try {
            // 1) Try phone set by JwtAuthenticationFilter (preferred)
            String phoneFromFilter = (String) request.getAttribute("phoneNumber");

            // 2) If not present, try to extract from Authorization header safely
            if (phoneFromFilter == null || phoneFromFilter.isBlank()) {
                String authHeader = servletRequest.getHeader("Authorization");
                if (authHeader != null) {
                    authHeader = authHeader.trim();
                    if (authHeader.toLowerCase().startsWith("bearer ")) {
                        String token = authHeader.substring(7).trim(); // remove "Bearer "
                        try {
                            phoneFromFilter = JwtUtil.extractPhoneNumber(token);
                        } catch (Exception e) {
                            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                    .body(Map.of("error", "Invalid Authorization token", "details", e.getMessage()));
                        }
                    }
                }
            }

            // 3) Validate we have a phone number now
            String phoneToUse = phoneFromFilter;
            if (phoneToUse == null || phoneToUse.isBlank()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Missing phoneNumber (token or request attribute)"));
            }

            // 4) Build DTO and call service
            ProfileSetupDTO dto = new ProfileSetupDTO();
            dto.setPhoneNumber(phoneToUse);
            dto.setName(name);
            dto.setBio(bio);

            String baseUrl = getBaseUrl(request);
            ProfileSetupDTO saved = profileSetupService.saveProfile(dto, file, baseUrl);

            return ResponseEntity.ok(Map.of(
                    "message", "Profile updated",
                    "profile", saved
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (IOException ioEx) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Failed to save file", "details", ioEx.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "Something went wrong", "details", ex.getMessage()));
        }
    }
}

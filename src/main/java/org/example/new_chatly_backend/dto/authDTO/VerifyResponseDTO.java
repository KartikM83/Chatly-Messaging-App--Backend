package org.example.new_chatly_backend.dto.authDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
public class VerifyResponseDTO {
    private String accessToken;
    private String refreshToken;
    private boolean isNew;
    private String id;
    private String phoneNumber;
    private String name;
    private String bio;
    private String profileImage;
}
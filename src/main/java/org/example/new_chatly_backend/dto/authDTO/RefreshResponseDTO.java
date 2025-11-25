package org.example.new_chatly_backend.dto.authDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RefreshResponseDTO {
    private String accessToken;
    private String refreshToken;
    private long expiresIn; // in seconds
}


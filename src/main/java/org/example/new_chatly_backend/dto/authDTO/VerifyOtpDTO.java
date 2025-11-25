package org.example.new_chatly_backend.dto.authDTO;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class VerifyOtpDTO {

    private String phoneNumber;
    @NotBlank
    private String otp;
}

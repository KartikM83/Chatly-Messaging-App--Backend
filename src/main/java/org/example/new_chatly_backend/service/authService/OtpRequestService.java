package org.example.new_chatly_backend.service.authService;

import org.example.new_chatly_backend.dto.authDTO.OtpRequestDTO;
import org.example.new_chatly_backend.dto.authDTO.VerifyOtpDTO;
import org.example.new_chatly_backend.dto.authDTO.VerifyResponseDTO;

public interface OtpRequestService {

    public OtpRequestDTO sendOtp(OtpRequestDTO request);
    public VerifyResponseDTO verifyOtp(VerifyOtpDTO request);
    public boolean canSend(OtpRequestDTO request);
}

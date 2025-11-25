package org.example.new_chatly_backend.controller.authController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.new_chatly_backend.dto.authDTO.*;
import org.example.new_chatly_backend.entity.userEntity.UserEntity;
import org.example.new_chatly_backend.repository.UserRepository;
import org.example.new_chatly_backend.service.authService.OtpRequestServiceImpl;
import org.example.new_chatly_backend.utility.JwtUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {


    private final OtpRequestServiceImpl otpRequestService;
    private final UserRepository userRepo;
    private final StringRedisTemplate redisTemplate;

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody RefreshRequestDTO request) {
        String userId = redisTemplate.opsForValue().get("refresh:" + request.getRefreshToken());
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid or expired refresh token");
        }

        UserEntity user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newAccessToken = JwtUtil.generateToken(user.getPhoneNumber(), user.getId());
        String newRefreshToken = UUID.randomUUID().toString();

        redisTemplate.delete("refresh:" + request.getRefreshToken());
        redisTemplate.opsForValue().set("refresh:" + newRefreshToken, user.getId().toString(), 7, TimeUnit.DAYS);

        long expiresIn = 3600;
        RefreshResponseDTO response = new RefreshResponseDTO(newAccessToken, newRefreshToken, expiresIn);

        return ResponseEntity.ok(response);
    }


    @PostMapping("/send-otp")
    public ResponseEntity<?> sendOtp(@Valid @RequestBody OtpRequestDTO request){
        if(!otpRequestService.canSend(request)){
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body("Too many OTP requests. Try again later.");
        }
        OtpRequestDTO otp = otpRequestService.sendOtp(request);
        return ResponseEntity.ok(otp);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody VerifyOtpDTO request) {
        VerifyResponseDTO response = otpRequestService.verifyOtp(request);

        if (response == null) {
            // structured error response
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "errorMessage", "Invalid or expired OTP",
                            "errorCode", 400,
                            "timeStamp", java.time.LocalDateTime.now().toString()
                    ));
        }

        // ✅ Build maps safely (nulls allowed)
        Map<String, Object> userData = new java.util.HashMap<>();
        userData.put("id", response.getId());
        userData.put("phoneNumber", response.getPhoneNumber());
        userData.put("name", response.getName());
        userData.put("bio", response.getBio());
        userData.put("profileImage", response.getProfileImage());

        Map<String, Object> success = new java.util.HashMap<>();
        success.put("accessToken", response.getAccessToken());
        success.put("refreshToken", response.getRefreshToken());
        success.put("isNew", response.isNew());
        success.put("user", userData);

        return ResponseEntity.ok(success);

    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody LogoutRequestDTO request) {
        String accessToken = request.getAccessToken();

        // Option 1: If you store refresh tokens in Redis, delete them
        // Example: if access token contains userId, you can remove corresponding refresh token
        // String userId = JwtUtil.extractUserId(accessToken);
        // redisTemplate.delete("refresh:" + refreshTokenOfUser);

        // Option 2: Optional: store invalidated access tokens in Redis (for blacklist)
        // redisTemplate.opsForValue().set("blacklist:" + accessToken, "true", 1, TimeUnit.HOURS);

        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}

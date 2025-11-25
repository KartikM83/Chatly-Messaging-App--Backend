package org.example.new_chatly_backend.service.authService;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.new_chatly_backend.dto.authDTO.OtpRequestDTO;
import org.example.new_chatly_backend.dto.authDTO.VerifyOtpDTO;
import org.example.new_chatly_backend.dto.authDTO.VerifyResponseDTO;
import org.example.new_chatly_backend.entity.userEntity.UserEntity;
import org.example.new_chatly_backend.repository.UserRepository;
import org.example.new_chatly_backend.utility.JwtUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional
public class OtpRequestServiceImpl implements OtpRequestService {

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom random = new SecureRandom();
    private final UserRepository userRepo;


    @Override
    public OtpRequestDTO sendOtp(OtpRequestDTO request) {

        String otp = String.format("%04d", random.nextInt(10000));
        String key = "otp:" + request.getPhoneNumber();
        redisTemplate.opsForValue().set(key, otp, Duration.ofMinutes(3));
        System.out.println("Your Otp" + otp);
        request.setOtp(otp);
        return request;

    }



    @Override

    public VerifyResponseDTO verifyOtp(VerifyOtpDTO request) {
        String key = "otp:" + request.getPhoneNumber();
        String savedOtp = redisTemplate.opsForValue().get(key);

        if (savedOtp != null && savedOtp.equals(request.getOtp())) {
            redisTemplate.delete(key);

            boolean userAlreadyExists = userRepo.existsByPhoneNumber(request.getPhoneNumber());
            // find or create user
            UserEntity user = userRepo.findByPhoneNumber(request.getPhoneNumber())
                    .orElseGet(() -> {
                        UserEntity u = new UserEntity();
                        u.setPhoneNumber(request.getPhoneNumber());
                        return userRepo.saveAndFlush(u);
                    });

            // decide if user is new (based on whether they have a name)
            boolean isNew = !userAlreadyExists;


            String accessToken = JwtUtil.generateToken(user.getPhoneNumber(), user.getId());
            String refreshToken = UUID.randomUUID().toString();
            String refreshKey = "refresh:" + refreshToken;
            redisTemplate.opsForValue().set(refreshKey, user.getId(), 7, TimeUnit.DAYS);



            return new VerifyResponseDTO(
                    accessToken,
                    refreshToken,
                    isNew,
                    user.getId(),
                    user.getPhoneNumber(),
                    user.getName(),
                    user.getBio(),
                    user.getProfileImage()
            );

        }

        return null; // OTP invalid
    }


    @Override
    public boolean canSend(OtpRequestDTO request) {
        String rateKey = "rateKey" + request.getPhoneNumber();
        Long count = redisTemplate.opsForValue().increment(rateKey);

        if (count == 1) {
            redisTemplate.expire(rateKey, Duration.ofMinutes(10));
        }
        return count <= 5;
    }
}
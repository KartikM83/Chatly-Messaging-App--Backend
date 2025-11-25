package org.example.new_chatly_backend.utility;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private static final String SECRET = System.getenv().getOrDefault(
            "JWT_SECRET",
            "my_super_long_super_secure_secret_key_1234567890_ABCDEFGH"
    );

    private static final String REFRESH_SECRET = "refresh_secret_key_that_is_very_long_and_secure_1234567890";

    private static final Key SECRET_KEY = Keys.hmacShaKeyFor(SECRET.getBytes());
    private static final Key REFRESH_KEY = Keys.hmacShaKeyFor(REFRESH_SECRET.getBytes());

    private static final long EXPIRATION_TIME = 1000L * 60 * 60 * 24; // 24 hrs
    private static final long REFRESH_TOKEN_EXPIRATION = 7 * 24 * 3600_000; // 7 days

    public static String generateToken(String phoneNumber, String userId) {
        return Jwts.builder()
                .setSubject(phoneNumber)
                .claim("userId", userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    public static boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public static String extractPhoneNumber(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public static String extractUserId(String token) {
        Object id = Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("userId");

        // Always return it as a String
        return id != null ? id.toString() : null;
    }

    public static String generateRefreshToken(String phoneNumber, String userId) {
        return Jwts.builder()
                .setSubject(phoneNumber)
                .claim("userId", userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION))
                .signWith(REFRESH_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    public static String extractUserIdFromRequest(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new RuntimeException("Missing Authorization header");
        }

        String token = header.substring(7);
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();

        Object userId = claims.get("userId");
        return userId != null ? userId.toString() : null;
    }

}

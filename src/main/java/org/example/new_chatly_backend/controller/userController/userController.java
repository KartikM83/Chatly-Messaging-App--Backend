package org.example.new_chatly_backend.controller.userController;

import lombok.RequiredArgsConstructor;
import org.apache.catalina.User;
import org.example.new_chatly_backend.dto.userDTO.*;
import org.example.new_chatly_backend.service.userService.UserServiceImpl;
import org.example.new_chatly_backend.utility.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class userController {

        private final JwtUtil jwtUtil;
        private final UserServiceImpl userService;

        @GetMapping("/me")
        public ResponseEntity<UserResponseDTO> getProfile(@RequestHeader("Authorization") String authHeader){
                String phoneNumber = JwtUtil.extractPhoneNumber(authHeader.substring(7));
                return ResponseEntity.ok(userService.getCurrentUser(phoneNumber));
        }

        @PutMapping("/me")
        public ResponseEntity<UserResponseDTO> updateProfile(
                @RequestHeader("Authorization") String authHeader,
                @RequestBody UserUpdateRequestDTO request ){
                String phoneNumber = JwtUtil.extractPhoneNumber(authHeader.substring(7));
                return ResponseEntity.ok(userService.updateCurrentUser(phoneNumber,request));
        }

        @GetMapping("/sync-contacts")
        public ResponseEntity<ContactSyncResponseDTO> syncContacts(@RequestHeader("Authorization") String authHeader){

                String phoneNumber = JwtUtil.extractPhoneNumber(authHeader.substring(7));
                return ResponseEntity.ok(userService.syncContact(phoneNumber));

        }

        @GetMapping("/{userId}")
        public ResponseEntity<UserResponseDTO> getUserById(
                @RequestHeader("Authorization") String authHeader,
                @PathVariable String userId){

                JwtUtil.extractPhoneNumber(authHeader.substring(7));
                return ResponseEntity.ok(userService.getUserById(userId));
        }



}

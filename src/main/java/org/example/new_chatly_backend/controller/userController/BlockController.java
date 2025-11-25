package org.example.new_chatly_backend.controller.userController;

import lombok.RequiredArgsConstructor;
import org.example.new_chatly_backend.dto.userDTO.BlockRequestDTO;
import org.example.new_chatly_backend.dto.userDTO.BlockResponseDTO;
import org.example.new_chatly_backend.service.userService.BlockServiceImpl;
import org.example.new_chatly_backend.utility.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BlockController {

    private final BlockServiceImpl blockService;

    @PostMapping("/block")
    public ResponseEntity<BlockResponseDTO> blockUser(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody BlockRequestDTO request){

        String blockerId = JwtUtil.extractUserId(authHeader.substring(7));
        return ResponseEntity.ok(blockService.blockUser(blockerId,request));

    }

    @PostMapping("/unblock")
    public ResponseEntity<BlockResponseDTO> unblockUser(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody BlockRequestDTO request){

        String blockerId = JwtUtil.extractUserId(authHeader.substring(7));
        return ResponseEntity.ok(blockService.unBlockUser(blockerId,request));


    }
}

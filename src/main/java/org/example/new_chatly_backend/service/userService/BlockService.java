package org.example.new_chatly_backend.service.userService;

import org.example.new_chatly_backend.dto.userDTO.BlockRequestDTO;
import org.example.new_chatly_backend.dto.userDTO.BlockResponseDTO;
import org.springframework.stereotype.Repository;

@Repository
public interface BlockService {
    public BlockResponseDTO blockUser(String blockerId, BlockRequestDTO request);

    public BlockResponseDTO unBlockUser(String blockerId, BlockRequestDTO request);
}

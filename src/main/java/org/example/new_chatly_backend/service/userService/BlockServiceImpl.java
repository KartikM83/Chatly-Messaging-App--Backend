package org.example.new_chatly_backend.service.userService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.example.new_chatly_backend.dto.userDTO.BlockRequestDTO;
import org.example.new_chatly_backend.dto.userDTO.BlockResponseDTO;
import org.example.new_chatly_backend.entity.userEntity.BlockUserEntity;
import org.example.new_chatly_backend.entity.userEntity.UserEntity;
import org.example.new_chatly_backend.exception.UserNotFoundException;
import org.example.new_chatly_backend.repository.BlockedUserRepository;
import org.example.new_chatly_backend.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BlockServiceImpl implements BlockService{


    private final UserRepository userRepo;
    private final BlockedUserRepository blockedUserRepo;
    @Override
    public BlockResponseDTO blockUser(String blockerId, BlockRequestDTO request) {

        UserEntity blocker = userRepo.findById(blockerId).orElseThrow(()->new UserNotFoundException("blocker not found"));
        UserEntity blocked = userRepo.findById(request.getTargetUserId()).orElseThrow(()->new UserNotFoundException("blocked not found"));

        System.out.println(blocker);
        System.out.println(blocked);
        if(blockedUserRepo.existsByBlockerAndBlocked(blocker,blocked)){
            throw new UserNotFoundException("User already Blocked");
        }

        BlockUserEntity blockUser = BlockUserEntity.builder()
                .blocker(blocker)
                .blocked(blocked)
                .build();
        blockedUserRepo.save(blockUser);

        return BlockResponseDTO.builder()
                .status("blocked")
                .targetUserId(blockUser.getId())
                .build();


    }

    @Override
    @Transactional
    public BlockResponseDTO unBlockUser(String blockerId, BlockRequestDTO request) {

        UserEntity blocker = userRepo.findById(blockerId).orElseThrow(()->new UserNotFoundException("blocker not found"));
        UserEntity blocked = userRepo.findById(request.getTargetUserId()).orElseThrow(()->new UserNotFoundException("blocked not found"));

        blockedUserRepo.deleteByBlockerAndBlocked(blocker,blocked);

        return BlockResponseDTO.builder()
                .status("unBlocked")
                .targetUserId(request.getTargetUserId())
                .build();

    }
}

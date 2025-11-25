package org.example.new_chatly_backend.service.userService;

import org.example.new_chatly_backend.dto.userDTO.*;

public interface UserService {

    public UserResponseDTO getCurrentUser(String phoneNumber);

    public UserResponseDTO updateCurrentUser(String phonNumber, UserUpdateRequestDTO request);

    public ContactSyncResponseDTO syncContact(String phoneNumber);

    public UserResponseDTO getUserById(String userId);
}

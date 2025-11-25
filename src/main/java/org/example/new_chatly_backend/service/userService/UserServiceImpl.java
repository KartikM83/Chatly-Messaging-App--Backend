package org.example.new_chatly_backend.service.userService;

import lombok.RequiredArgsConstructor;
import org.example.new_chatly_backend.dto.contactDTO.ContactResponseDTO;
import org.example.new_chatly_backend.dto.userDTO.*;
import org.example.new_chatly_backend.entity.contactEntity.ContactEntity;
import org.example.new_chatly_backend.entity.userEntity.UserEntity;
import org.example.new_chatly_backend.exception.UserNotFoundException;
import org.example.new_chatly_backend.repository.ContactRepository;
import org.example.new_chatly_backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepo;
    private final ContactRepository contactRepo;

    @Override
    public UserResponseDTO getCurrentUser(String phoneNumber) {
        UserEntity user = userRepo.findByPhoneNumber(phoneNumber).orElseThrow(()-> new UserNotFoundException("User not found"));
        return UserResponseDTO.builder()
                .id(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .name(user.getName())
                .profileImage(user.getProfileImage())
                .bio(user.getBio())
                .build();

    }

    @Override
    public UserResponseDTO updateCurrentUser(String phonNumber, UserUpdateRequestDTO request) {
        UserEntity user = userRepo.findByPhoneNumber(phonNumber).orElseThrow(()->new UserNotFoundException("User not found"));

        if(request.getName()!=null) user.setName(request.getName());
        if(request.getBio()!=null) user.setBio(request.getBio());
        if(request.getProfileImage()!=null) user.setProfileImage(request.getProfileImage());

        userRepo.save(user);
        return UserResponseDTO.builder()
                .id(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .name(user.getName())
                .profileImage(user.getProfileImage())
                .bio(user.getBio())
                .build();



    }

    @Override
    public ContactSyncResponseDTO syncContact(String phoneNumber) {
        // 1. Get the owner (user whose contacts are being synced)
        UserEntity owner = userRepo.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new UserNotFoundException("Owner not found"));

        // 2. Fetch all saved contacts for that owner
        List<ContactEntity> contacts = contactRepo.findByOwner(owner);

        // 3. Map each contact into ContactResponseDTO
        List<ContactResponseDTO> contactResponses = contacts.stream().map(contact -> {
            UserEntity user = userRepo.findByPhoneNumber(contact.getPhone()).orElse(null);

            return ContactResponseDTO.builder()
                    .contactId(contact.getId())
                    .userId(user != null ? user.getId() : null)
                    .name(contact.getName())
                    .bio(user != null ? user.getBio() : null)
                    .phoneNumber(contact.getPhone())
                    .profileImage(user != null ? user.getProfileImage() : null)
                    .build();
        }).toList();

        // 4. Filter those contacts who are registered on Chatly (matched users)
        List<ContactMatchDTO> matches = contactResponses.stream()
                .filter(c -> c.getUserId() != null) // only registered users
                .map(c -> new ContactMatchDTO(c.getUserId(), c.getName(), c.getPhoneNumber(),c.getBio(),c.getProfileImage()))
                .toList();

        List<ContactMatchDTO> unmatches = contactResponses.stream()
                .filter(c->c.getUserId() == null)
                .map(c->new ContactMatchDTO(c.getUserId(),c.getName(),c.getPhoneNumber(),c.getBio(),c.getProfileImage()))
                .toList();

        // 5. Build and return final response
        return ContactSyncResponseDTO.builder()
                .totalContacts(contactResponses.size())
                .matches(matches)
                .unmatches(unmatches)
                .build();
    }
   @Override
    public UserResponseDTO getUserById(String userId) {
        UserEntity user =userRepo.findById(userId).orElseThrow(()->new UserNotFoundException("User not found"));
        return UserResponseDTO.builder()
                .id(user.getId())
                .phoneNumber(user.getPhoneNumber())
                .name(user.getName())
                .profileImage(user.getProfileImage())
                .bio(user.getBio())
                .build();

    }


}

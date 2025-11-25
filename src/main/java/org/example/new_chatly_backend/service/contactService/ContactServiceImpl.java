package org.example.new_chatly_backend.service.contactService;

import lombok.RequiredArgsConstructor;
import org.example.new_chatly_backend.dto.contactDTO.ContactResponseDTO;
import org.example.new_chatly_backend.dto.userDTO.ContactRequestDTO;
import org.example.new_chatly_backend.entity.contactEntity.ContactEntity;
import org.example.new_chatly_backend.entity.userEntity.UserEntity;
import org.example.new_chatly_backend.exception.UserNotFoundException;
import org.example.new_chatly_backend.repository.ContactRepository;
import org.example.new_chatly_backend.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ContactServiceImpl implements ContactService {

    private final UserRepository userRepo;
    private final ContactRepository contactRepo;

    @Override
    public ContactResponseDTO addContact(String phoneNumber, ContactRequestDTO request) {

        // Step 1: Get owner (the logged-in user)
        UserEntity owner = userRepo.findByPhoneNumber(phoneNumber).orElseThrow(()->new UserNotFoundException("Owenr not found"));

        // Step 2: Try to find the contact user (if they are registered)
        UserEntity contactUser = userRepo.findByPhoneNumber(request.getPhone()).orElse(null);

        // Step 3: Check if contact already exists
        boolean exists = contactRepo.findByOwner(owner).stream()
                .anyMatch(c->c.getPhone().equals(request.getPhone()));

        if(exists){
            throw  new UserNotFoundException("Contact already exists");
        }

        // Step 4: Save new contact
        ContactEntity contact = ContactEntity.builder()
                .owner(owner)
                .contactUser(contactUser)
                .name(request.getName())
                .phone(request.getPhone())
                .isFavorite(false)
                .build();
        ContactEntity saved = contactRepo.save(contact);


        // Step 5: Map response
        return ContactResponseDTO.builder()
                .contactId(saved.getId())
                .userId(contactUser != null ? contactUser.getId() : null)
                .name(saved.getName())
                .phoneNumber(saved.getPhone())
                .build();

    }

    @Override
    public List<ContactResponseDTO> getContacts(String phoneNumber) {

        UserEntity owner = userRepo.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new UserNotFoundException("Owner not found"));

        List<ContactEntity> contacts = contactRepo.findByOwner(owner);

        return contacts.stream().map(contact -> {
            UserEntity user = userRepo.findByPhoneNumber(contact.getPhone())
                    .orElse(null); // use orElse(null) if some contacts may not be registered users

            return ContactResponseDTO.builder()
                    .contactId(contact.getId())
                    .userId(user != null ? user.getId() : null)
                    .name(contact.getName())
                    .bio(user != null ? user.getBio() : null)
                    .phoneNumber(contact.getPhone())
                    .profileImage(user != null ? user.getProfileImage() : null)
                    .build();
        }).toList();
    }

}

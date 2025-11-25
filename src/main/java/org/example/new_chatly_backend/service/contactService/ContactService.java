package org.example.new_chatly_backend.service.contactService;

import org.example.new_chatly_backend.dto.contactDTO.ContactResponseDTO;
import org.example.new_chatly_backend.dto.userDTO.ContactRequestDTO;

import java.util.List;

public interface ContactService {

    public ContactResponseDTO addContact(String phoneNumber,ContactRequestDTO request);


    public List<ContactResponseDTO> getContacts(String phonNumber);
}

package org.example.new_chatly_backend.controller.contactController;

import lombok.RequiredArgsConstructor;
import org.example.new_chatly_backend.dto.contactDTO.ContactResponseDTO;
import org.example.new_chatly_backend.dto.userDTO.ContactRequestDTO;
import org.example.new_chatly_backend.service.contactService.ContactServiceImpl;
import org.example.new_chatly_backend.utility.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/contacts")
public class ContactController {

    private final ContactServiceImpl contactService;

    @PostMapping
    public ResponseEntity<ContactResponseDTO> addContact(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody ContactRequestDTO request){

        String phoneNumber = JwtUtil.extractPhoneNumber(authHeader.substring(7));
        return ResponseEntity.ok(contactService.addContact(phoneNumber,request));

    }

    @GetMapping
    public ResponseEntity<Map<String, List<ContactResponseDTO>>> getContacts(
            @RequestHeader("Authorization") String authHeader) {
        String phoneNumber = JwtUtil.extractPhoneNumber(authHeader.substring(7));
        List<ContactResponseDTO> contacts = contactService.getContacts(phoneNumber);

        Map<String, List<ContactResponseDTO>> response = new HashMap<>();
        response.put("contacts", contacts);

        return ResponseEntity.ok(response);
    }

}

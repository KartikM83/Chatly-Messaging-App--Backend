package org.example.new_chatly_backend.dto.userDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.new_chatly_backend.dto.contactDTO.ContactResponseDTO;

import java.util.List;
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ContactSyncResponseDTO {
    private int totalContacts;
    private List<ContactMatchDTO> matches;
    private List<ContactMatchDTO> unmatches; // full list of user's contacts
}

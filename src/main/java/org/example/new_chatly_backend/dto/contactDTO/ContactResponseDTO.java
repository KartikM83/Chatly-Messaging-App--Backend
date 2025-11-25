package org.example.new_chatly_backend.dto.contactDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ContactResponseDTO {

    private String contactId;
    private String userId;
    private String name;
    private String phoneNumber;
    private String profileImage;
    private String bio;
}

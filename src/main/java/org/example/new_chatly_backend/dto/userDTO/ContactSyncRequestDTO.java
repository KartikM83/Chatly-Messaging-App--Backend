package org.example.new_chatly_backend.dto.userDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContactSyncRequestDTO {
    private List<ContactRequestDTO> contacts;
}

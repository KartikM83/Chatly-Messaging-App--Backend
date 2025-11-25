package org.example.new_chatly_backend.dto.userDTO;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactRequestDTO {
    private String phone;
    private String name;
}


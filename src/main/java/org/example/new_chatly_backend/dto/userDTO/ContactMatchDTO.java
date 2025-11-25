package org.example.new_chatly_backend.dto.userDTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContactMatchDTO {

    private String userId;
    private String name;
    private String phoneNumber;
    private String bio;
    private String profileImage;
}

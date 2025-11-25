package org.example.new_chatly_backend.dto.userDTO;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponseDTO {

    private String id;
    private String phoneNumber;
    private String name;
    private String profileImage;
    private String bio;

}

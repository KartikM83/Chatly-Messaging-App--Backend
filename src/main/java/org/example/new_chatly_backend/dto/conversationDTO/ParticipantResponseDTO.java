package org.example.new_chatly_backend.dto.conversationDTO;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ParticipantResponseDTO {

    private String id;
    private String name;
    private String profileImage;
}

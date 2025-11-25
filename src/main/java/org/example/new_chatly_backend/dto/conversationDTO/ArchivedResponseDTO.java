package org.example.new_chatly_backend.dto.conversationDTO;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ArchivedResponseDTO {
    private String conversationId;
    private boolean archived;
}

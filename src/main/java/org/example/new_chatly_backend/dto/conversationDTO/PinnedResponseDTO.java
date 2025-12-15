package org.example.new_chatly_backend.dto.conversationDTO;


import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PinnedResponseDTO {
    private String conversationId;
    private boolean pinned;
}

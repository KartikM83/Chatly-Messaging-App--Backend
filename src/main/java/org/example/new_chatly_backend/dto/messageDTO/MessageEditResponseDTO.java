package org.example.new_chatly_backend.dto.messageDTO;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class MessageEditResponseDTO {
    private String id;
    private String content;
    private boolean edited;
    private Instant editedAt;
}
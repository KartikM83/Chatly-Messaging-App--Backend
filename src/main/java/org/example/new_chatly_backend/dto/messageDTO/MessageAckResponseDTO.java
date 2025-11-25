package org.example.new_chatly_backend.dto.messageDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageAckResponseDTO {
    private String messageId;
    private String status;
    private int markRead;
}
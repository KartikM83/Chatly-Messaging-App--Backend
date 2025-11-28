package org.example.new_chatly_backend.dto.messageDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.new_chatly_backend.entity.messageEntity.MessageStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageAckResponseDTO {
    private String messageId;
    private MessageStatus status;
    private int markRead;
}
package org.example.new_chatly_backend.dto.messageDTO;

import lombok.*;
import org.example.new_chatly_backend.entity.messageEntity.MessageType;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageRequestDTO {
    private MessageType type;
    private String clientMessageId;
    private String content;
    private String status;
}

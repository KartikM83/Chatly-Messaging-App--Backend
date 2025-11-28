package org.example.new_chatly_backend.dto.messageDTO;

import lombok.*;
import org.example.new_chatly_backend.entity.messageEntity.MessageStatus;
import org.example.new_chatly_backend.entity.messageEntity.MessageType;

import java.awt.*;
import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageResponseDTO {

    private String id;
    private String clientMessageId;
    private String conversationId;
    private String senderId;
    private MessageType type;
    private String content;
    private Instant timestamp;
    private MessageStatus status;
}

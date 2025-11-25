package org.example.new_chatly_backend.dto.messageDTO;

import lombok.*;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageAckRequestDTO {
    private String messageId;
    private String status; // delivered, read
    private Instant deliveredAt;
    private Instant readAt;
}

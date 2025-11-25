package org.example.new_chatly_backend.dto.messageDTO;

import lombok.*;

import java.time.Instant;
import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class MessageReadRequestDTO {
    private List<String> messageIds;
    private Instant readAt;
}
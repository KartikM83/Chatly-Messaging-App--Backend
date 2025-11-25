package org.example.new_chatly_backend.dto.conversationDTO;

import lombok.*;
import org.example.new_chatly_backend.entity.conversationEntity.ConversationType;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConversationResponseDTO {

    private String id;
    private ConversationType type;
    private List<ParticipantResponseDTO> participants;
    private String groupName;
    private String adminId;
    private Instant createdAt;
    private String lastMessage;
    private Instant lastMessageAt;
    private long unreadCount;
    private String groupProfileImage;
    private Boolean pinned;
    private Boolean typing;
    private Boolean archived;


}

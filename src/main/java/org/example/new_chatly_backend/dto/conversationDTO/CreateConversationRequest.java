package org.example.new_chatly_backend.dto.conversationDTO;

import lombok.*;
import org.example.new_chatly_backend.entity.conversationEntity.ConversationType;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateConversationRequest {

    private ConversationType type;
    private String participantId;
    private String name; // only for group
    private List<String> memberIds; // for group
    private String groupProfileImage;
    private Boolean pinned;
    private Boolean typing;
    private Boolean archived;

}

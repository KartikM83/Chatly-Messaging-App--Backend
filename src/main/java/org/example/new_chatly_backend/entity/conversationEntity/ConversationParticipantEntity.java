package org.example.new_chatly_backend.entity.conversationEntity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import org.example.new_chatly_backend.entity.userEntity.UserEntity;

import java.time.Instant;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConversationParticipantEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
//    @JsonBackReference("conversation-participants")
    private ConversationEntity conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity user;

    private boolean isPinned =false;
    private boolean isTyping =false;
    private boolean archived =false;
    private boolean deletedForUser = false;
    private Instant deletedAt;
}

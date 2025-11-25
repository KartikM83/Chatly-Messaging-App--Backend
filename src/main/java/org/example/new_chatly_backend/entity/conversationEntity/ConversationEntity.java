package org.example.new_chatly_backend.entity.conversationEntity;

import jakarta.persistence.*;
import lombok.*;
import org.example.new_chatly_backend.entity.messageEntity.MessageEntity;

import java.time.Instant;
import java.util.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "conversation")
public class ConversationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String name;

    @Enumerated(EnumType.STRING)
    private ConversationType type;

    private String profileImage;

    private String admin;

    private Instant createdAt;

    private boolean isPinned;
    private boolean isTyping;
    private boolean archived;


    // Participants mapping
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
//    @JsonManagedReference("conversation-participants")
    private Set<ConversationParticipantEntity> participants = new HashSet<>();

    // Messages mapping
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
//    @JsonManagedReference("conversation-messages")
    private List<MessageEntity> message = new ArrayList<>();
}

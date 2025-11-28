package org.example.new_chatly_backend.entity.messageEntity;

import jakarta.persistence.*;
import lombok.*;
import org.example.new_chatly_backend.entity.conversationEntity.ConversationEntity;
import org.example.new_chatly_backend.entity.userEntity.UserEntity;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, length = 2000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageType type;


    @Column(nullable = false)
    private Instant createdAt = Instant.now();



    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private UserEntity sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
//    @JsonBackReference("conversation-messages")
    private ConversationEntity conversation;

    @ManyToMany
    @JoinTable(
            name = "message_reads",
            joinColumns = @JoinColumn(name = "message_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<UserEntity> readBy = new HashSet<>();

    @Column(name = "client_message_id")
    private String clientMessageId;

    @Enumerated(EnumType.STRING)
    private MessageStatus status = MessageStatus.SENT;

    private Instant deliveredAt;
    private Instant readAt;

    private boolean edited = false;
    private Instant editedAt;

    private boolean deleted = false;
    private String deletedFor; // "sender" or "everyone"
    private Instant deletedAt;

    @ElementCollection
    @CollectionTable(name = "message_reactions")
    @MapKeyColumn(name = "user_id")
    @Column(name = "reaction")
    private Map<String, String> reactions = new HashMap<>();
}

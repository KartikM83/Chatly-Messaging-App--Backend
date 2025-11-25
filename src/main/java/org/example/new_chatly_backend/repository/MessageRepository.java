package org.example.new_chatly_backend.repository;

import org.example.new_chatly_backend.entity.messageEntity.MessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository

public interface MessageRepository extends JpaRepository<MessageEntity, String> {

    Optional<MessageEntity> findByClientMessageIdAndConversationId(String clientMessageId, String conversationId);

    @Query("SELECT m FROM MessageEntity m WHERE m.conversation.id = :conversationId ORDER BY m.createdAt DESC")
    List<MessageEntity> findTopByConversationIdOrderByCreatedAtDesc(
            @Param("conversationId") String conversationId,
            org.springframework.data.domain.Pageable pageable
    );

    @Query("SELECT m FROM MessageEntity m WHERE m.conversation.id = :conversationId AND m.createdAt < :before ORDER BY m.createdAt DESC")
    List<MessageEntity> findTopByConversationIdAndCreatedAtBeforeOrderByCreatedAtDesc(
            @Param("conversationId") String conversationId,
            @Param("before") Instant before,
            org.springframework.data.domain.Pageable pageable
    );

    @Query("SELECT COUNT(m) FROM MessageEntity m WHERE m.conversation.id = :conversationId AND m.status <> 'read' AND m.sender.id <> :userId")
    long countUnreadMessages(@Param("conversationId") String conversationId, @Param("userId") String userId);

    long countByConversationId(String conversationId);
}

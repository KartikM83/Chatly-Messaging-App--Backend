package org.example.new_chatly_backend.repository;

import org.example.new_chatly_backend.entity.conversationEntity.ConversationParticipantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipantEntity, String> {
    Optional<ConversationParticipantEntity> findByConversation_IdAndUser_Id(String conversationId, String  userId);

    long countByConversation_Id(String id);
}

package org.example.new_chatly_backend.repository;

import org.example.new_chatly_backend.entity.conversationEntity.ConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<ConversationEntity, String> {

    @Query("""
    SELECT DISTINCT c FROM ConversationEntity c
    JOIN c.participants p1
    JOIN c.participants p2
    WHERE c.type = 'DIRECT'
      AND p1.user.id = :user1Id
      AND p2.user.id = :user2Id
""")
    Optional<ConversationEntity> findDirectConversationBetweenUsers(String user1Id, String user2Id);



    @Query("""
        SELECT c FROM ConversationEntity c
        WHERE c.type = 'GROUP' AND c.name = :name AND c.admin = :adminId
    """)
    List<ConversationEntity> findGroupByNameAndAdminId(String name, String adminId);

    @Query("SELECT c FROM ConversationEntity c WHERE c.type='GROUP' AND c.name = :groupName")
    List<ConversationEntity> findGroupByName(String groupName);

    @Query("SELECT DISTINCT c FROM ConversationEntity c JOIN c.participants p WHERE p.user.id = :userId")
    List<ConversationEntity> findByParticipantId(@Param("userId") String userId);


}

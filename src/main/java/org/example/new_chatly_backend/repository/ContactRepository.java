package org.example.new_chatly_backend.repository;

import org.example.new_chatly_backend.entity.contactEntity.ContactEntity;
import org.example.new_chatly_backend.entity.conversationEntity.ConversationEntity;
import org.example.new_chatly_backend.entity.userEntity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<ContactEntity, String> {

    List<ContactEntity> findByOwner(UserEntity owner);


}

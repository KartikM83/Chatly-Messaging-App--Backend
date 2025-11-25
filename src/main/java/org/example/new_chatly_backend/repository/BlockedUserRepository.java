package org.example.new_chatly_backend.repository;

import org.example.new_chatly_backend.entity.userEntity.BlockUserEntity;
import org.example.new_chatly_backend.entity.userEntity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BlockedUserRepository extends JpaRepository<BlockUserEntity,String> {

    public boolean existsByBlockerAndBlocked(UserEntity blocker, UserEntity blocked);



    void deleteByBlockerAndBlocked(UserEntity blocker, UserEntity blocked);
}

package org.example.new_chatly_backend.entity.userEntity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Table(name = "blocked_user", uniqueConstraints = @UniqueConstraint(columnNames = {"blocker_id","blockedUser_id"}))
public class BlockUserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_id", nullable = false)
    private UserEntity blocker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blockedUser_id" ,nullable = false)
    private UserEntity blocked;


}

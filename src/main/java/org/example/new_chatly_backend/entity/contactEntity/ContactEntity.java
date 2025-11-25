package org.example.new_chatly_backend.entity.contactEntity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.new_chatly_backend.entity.userEntity.UserEntity;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Table(name = "contacts")
public class ContactEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private UserEntity owner;

    // contact user reference
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_user_id")
    private UserEntity contactUser;

    // name saved by the owner (can differ from contactUser.name)
    private String name;

    private String phone;

    private String bio;
    private String profileImage;

    private boolean isFavorite;
}

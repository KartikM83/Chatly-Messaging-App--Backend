package org.example.new_chatly_backend.dto.profileSetupDTO;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProfileSetupDTO {

    private String id;
    private String phoneNumber;

    @NotNull(message = "Name is required")
    private String name;

    private String bio;

    private String profileImage;

//    public UserEntity toEntity() {
//        UserEntity user = new UserEntity();
//        user.setPhoneNumber(this.phoneNumber);
//        user.setFullName(this.name);
//        user.setBio(this.bio);
//        user.setProfileImage(this.profileImage);
//        user.setBanned(false);
//        return user;
//    }
}
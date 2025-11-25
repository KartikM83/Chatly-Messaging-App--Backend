package org.example.new_chatly_backend.service.profileSetupService;


import org.example.new_chatly_backend.dto.profileSetupDTO.ProfileSetupDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ProfileSetupService {

    ProfileSetupDTO saveProfile(ProfileSetupDTO dto, MultipartFile file, String requestBaseUrl) throws IOException;
}

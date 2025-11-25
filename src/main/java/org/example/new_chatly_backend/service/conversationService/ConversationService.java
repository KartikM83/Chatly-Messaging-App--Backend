package org.example.new_chatly_backend.service.conversationService;

import jakarta.servlet.http.HttpServletRequest;
import org.example.new_chatly_backend.dto.conversationDTO.ArchivedResponseDTO;
import org.example.new_chatly_backend.dto.conversationDTO.ConversationResponseDTO;
import org.example.new_chatly_backend.dto.conversationDTO.CreateConversationRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ConversationService {
    ConversationResponseDTO createConversation(CreateConversationRequest request, HttpServletRequest servletRequest);

    ConversationResponseDTO getConversationById(String conversationId, HttpServletRequest servletRequest);

    ArchivedResponseDTO archiveConversation(String conversationId, HttpServletRequest servletRequest);

    ArchivedResponseDTO unarchiveConversation(String conversationId, HttpServletRequest servletRequest);

    String deleteConversation(String conversationId, HttpServletRequest servletRequest);

    public  List<ConversationResponseDTO> getUserConversations(String userId);



    ConversationResponseDTO updateConversation(CreateConversationRequest request, MultipartFile file, HttpServletRequest servletRequest, String conversationId);
}

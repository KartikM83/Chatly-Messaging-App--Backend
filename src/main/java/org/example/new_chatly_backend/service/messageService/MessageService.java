package org.example.new_chatly_backend.service.messageService;

import org.example.new_chatly_backend.dto.messageDTO.*;

import java.security.Principal;
import java.util.Map;

public interface MessageService {
    MessageResponseDTO sendMessage(String conversationId, Principal principal, MessageRequestDTO request);

    MessageAckResponseDTO acknowledgeMessage(String conversationId,MessageAckRequestDTO request, Principal principal);

    MessageReadResponseDTO markMessagesAsRead(String conversationId, MessageReadRequestDTO request, Principal principal);

    MessageEditResponseDTO editMessage(String conversationId, String messageId, MessageEditRequestDTO request, Principal principal);

    Object deleteMessage(String conversationId, String messageId, Principal principal);

    Object reactToMessage(String conversationId, String messageId, String reaction, Principal principal);

    Map<String, Object> getMessages(String conversationId, int limit, String before);
}

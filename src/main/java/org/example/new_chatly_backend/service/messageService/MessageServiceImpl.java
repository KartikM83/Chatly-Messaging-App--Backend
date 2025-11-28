package org.example.new_chatly_backend.service.messageService;

import lombok.RequiredArgsConstructor;
import org.example.new_chatly_backend.dto.messageDTO.*;
import org.example.new_chatly_backend.entity.conversationEntity.ConversationEntity;
import org.example.new_chatly_backend.entity.messageEntity.MessageEntity;
import org.example.new_chatly_backend.entity.messageEntity.MessageStatus;
import org.example.new_chatly_backend.entity.messageEntity.MessageType;
import org.example.new_chatly_backend.entity.userEntity.UserEntity;
import org.example.new_chatly_backend.exception.UserNotFoundException;
import org.example.new_chatly_backend.repository.ConversationRepository;
import org.example.new_chatly_backend.repository.MessageRepository;
import org.example.new_chatly_backend.repository.UserRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final ConversationRepository conversationRepo;
    private final UserRepository userRepo;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate; // ✅ inject this

    @Override
    public MessageResponseDTO sendMessage(String conversationId, Principal principal, MessageRequestDTO request) {

        String senderId = principal.getName();

        ConversationEntity conversation = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new UserNotFoundException("Conversation not found"));

        UserEntity sender = userRepo.findById(senderId)
                .orElseThrow(() -> new UserNotFoundException("Sender not found"));

        if (request.getClientMessageId() != null) {
            messageRepository.findByClientMessageIdAndConversationId(request.getClientMessageId(), conversationId)
                    .ifPresent(m -> { throw new IllegalStateException("Message already exists"); });
        }

        MessageEntity m = MessageEntity.builder()
                .conversation(conversation)
                .sender(sender)
                .content(request.getContent())
                .type(request.getType() != null ? request.getType() : MessageType.TEXT)
                .clientMessageId(request.getClientMessageId())
                .createdAt(Instant.now())
                .status(MessageStatus.SENT)
                .build();

        MessageEntity saved = messageRepository.save(m);

        MessageResponseDTO response = MessageResponseDTO.builder()
                .id(saved.getId())
                .clientMessageId(request.getClientMessageId())
                .conversationId(conversationId)
                .senderId(senderId)
                .type(request.getType())
                .content(request.getContent())
                .timestamp(saved.getCreatedAt())
                .status(MessageStatus.SENT)
                .build();

        // ✅ send real-time update via WebSocket
        System.out.println("🟢 Sending WebSocket message to /topic/conversations/" + conversationId);
        System.out.println("Payload: " + response);
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, response);

//        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, response);

        return response;
    }

    @Override
    public MessageAckResponseDTO acknowledgeMessage(String conversationId, MessageAckRequestDTO request, Principal principal) {
        MessageEntity message = messageRepository.findById(request.getMessageId())
                .orElseThrow(() -> new UserNotFoundException("Message not found"));

        if (!message.getConversation().getId().equals(conversationId)) {
            throw new IllegalArgumentException("Message does not belong to this conversation");
        }

        if (request.getStatus().equalsIgnoreCase("delivered")) {
            message.setStatus(MessageStatus.DELIVERED);
            message.setDeliveredAt(request.getDeliveredAt() != null ? request.getDeliveredAt() : Instant.now());
        } else if (request.getStatus().equalsIgnoreCase("read")) {
            message.setStatus(MessageStatus.SEEN);
            message.setReadAt(request.getReadAt() != null ? request.getReadAt() : Instant.now());
        }

        MessageEntity updated = messageRepository.save(message);

        MessageAckResponseDTO response = MessageAckResponseDTO.builder()
                .messageId(updated.getId())
                .status(updated.getStatus())
                .build();

        // 🔔 Notify via WebSocket
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, response);

        return response;
    }

    @Override
    public MessageReadResponseDTO markMessagesAsRead(String conversationId, MessageReadRequestDTO request, Principal principal) {
        int updatedCount = 0;
        String userId = principal.getName();

        for (String messageId : request.getMessageIds()) {
            MessageEntity  message = messageRepository.findById(messageId)
                    .orElse(null);
            if (message == null) continue;
            if (!message.getSender().getId().equals(userId)) { // don’t mark my own msg as read
                message.getReadBy().add(userRepo.findById(userId).orElseThrow());
                message.setStatus(MessageStatus.SEEN);
                message.setReadAt(Instant.now());
                messageRepository.save(message);
                updatedCount++;

                messagingTemplate.convertAndSend(
                        "/topic/conversations/" + conversationId,
                        MessageAckResponseDTO.builder()
                                .messageId(message.getId())
                                .status(MessageStatus.SEEN)
                                .build()
                );
            }
        }

        return MessageReadResponseDTO.builder()
                .markedRead(updatedCount)
                .build();
    }

    public MessageEditResponseDTO editMessage(String conversationId, String messageId, MessageEditRequestDTO request, Principal principal) {
        var senderId = principal.getName();

        var message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        if (!message.getConversation().getId().equals(conversationId)) {
            throw new IllegalArgumentException("Message does not belong to this conversation");
        }

        if (!message.getSender().getId().equals(senderId)) {
            throw new IllegalStateException("You can only edit your own messages");
        }

        message.setContent(request.getContent());
        message.setEdited(true);
        message.setEditedAt(Instant.now());
        messageRepository.save(message);

        MessageEditResponseDTO response = MessageEditResponseDTO.builder()
                .id(message.getId())
                .content(message.getContent())
                .edited(true)
                .editedAt(message.getEditedAt())
                .build();

        // 🔔 Broadcast update to all users in the conversation
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, response);

        return response;
    }

    @Override
    public Map<String, Object> deleteMessage(String conversationId, String messageId, Principal principal) {
        String userId = principal.getName();

        MessageEntity message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        // Optional: verify that message belongs to conversation
        if (!message.getConversation().getId().equals(conversationId)) {
            throw new RuntimeException("Message does not belong to this conversation");
        }

        // ✅ Mark message as deleted
        message.setDeleted(true);
        message.setDeletedFor("sender"); // or logic for "everyone"
        message.setDeletedAt(Instant.now());

        messageRepository.save(message);

        Map<String, Object> response = new HashMap<>();
        response.put("id", message.getId());
        response.put("deletedFor", message.getDeletedFor());
        response.put("deletedAt", message.getDeletedAt());

        // Optional: send WebSocket event to update all users in the conversation
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, response);

        return response;
    }

    @Override
    public Map<String, Object> reactToMessage(String conversationId, String messageId, String reaction, Principal principal) {
        String userId = principal.getName();

        MessageEntity message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));

        if (!message.getConversation().getId().equals(conversationId)) {
            throw new RuntimeException("Message does not belong to this conversation");
        }

        // ✅ Add or update user’s reaction (toggle behavior)
        if (reaction == null || reaction.isBlank()) {
            message.getReactions().remove(userId);
        } else {
            message.getReactions().put(userId, reaction);
        }

        messageRepository.save(message);

        // ✅ Create structured WebSocket event
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "MESSAGE_REACTION");
        payload.put("conversationId", conversationId);

        Map<String, Object> data = new HashMap<>();
        data.put("messageId", messageId);
        data.put("reaction", reaction);
        data.put("by", userId);

        payload.put("data", data);

        // ✅ Send WebSocket update
        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, payload);

        // ✅ Return response for HTTP client
        return data;
    }



    @Override
    public Map<String, Object> getMessages(String conversationId, int limit, String before) {
        ConversationEntity conversation = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        List<MessageEntity> messages;

        if (before != null && !before.isBlank()) {
            List<MessageEntity> result;
            try {
                Instant beforeTime = Instant.parse(before);
                result = messageRepository.findTopByConversationIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                        conversationId, beforeTime, org.springframework.data.domain.PageRequest.of(0, limit)
                );
            } catch (Exception e) {
                Optional<MessageEntity> beforeMsgOpt = messageRepository.findById(before);
                if (beforeMsgOpt.isPresent()) {
                    MessageEntity beforeMsg = beforeMsgOpt.get();
                    result = messageRepository.findTopByConversationIdAndCreatedAtBeforeOrderByCreatedAtDesc(
                            conversationId, beforeMsg.getCreatedAt(), org.springframework.data.domain.PageRequest.of(0, limit)
                    );
                } else {
                    throw new RuntimeException("Invalid 'before' parameter: " + before);
                }
            }
            messages = result;
        } else {
            messages = messageRepository.findTopByConversationIdOrderByCreatedAtDesc(
                    conversationId,
                    org.springframework.data.domain.PageRequest.of(0, limit)
            );
        }

        // Reverse for ascending order
        Collections.reverse(messages);

        List<MessageResponseDTO> messageDTOs = messages.stream()
                .map(msg -> MessageResponseDTO.builder()
                        .id(msg.getId())
                        .clientMessageId(msg.getClientMessageId())
                        .conversationId(conversationId)
                        .senderId(msg.getSender().getId())
                        .type(msg.getType())
                        .content(msg.getContent())
                        .timestamp(msg.getCreatedAt())
                        .status(msg.getStatus())
                        .build())
                .toList();

        long totalCount = messageRepository.countByConversationId(conversationId);
        boolean hasMore = totalCount > messages.size();

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("conversationId", conversationId);
        resultMap.put("messages", messageDTOs);
        resultMap.put("hasMore", hasMore);

        return resultMap;
    }

    public void markAllAsDeliveredForUser(Principal principal) {
        String userId = principal.getName();

        List<MessageEntity> pending = messageRepository.findPendingForUser(userId);
        if (pending.isEmpty()) {
            return;
        }

        Instant now = Instant.now();

        for (MessageEntity m : pending) {
            // Only update if still SENT (avoid downgrading SEEN etc.)
            if (m.getStatus() == MessageStatus.SENT) {
                m.setStatus(MessageStatus.DELIVERED);
                if (m.getDeliveredAt() == null) {
                    m.setDeliveredAt(now);
                }
            }
        }

        messageRepository.saveAll(pending);

        // 🔔 For each updated message, broadcast an ACK to that conversation
        for (MessageEntity m : pending) {
            MessageAckResponseDTO ack = MessageAckResponseDTO.builder()
                    .messageId(m.getId())
                    .status(m.getStatus()) // DELIVERED
                    .build();

            messagingTemplate.convertAndSend(
                    "/topic/conversations/" + m.getConversation().getId(),
                    ack
            );
        }
    }




}




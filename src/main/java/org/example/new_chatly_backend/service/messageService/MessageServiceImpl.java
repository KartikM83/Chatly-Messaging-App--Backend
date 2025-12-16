    package org.example.new_chatly_backend.service.messageService;

    import jakarta.servlet.http.HttpServletRequest;
    import lombok.RequiredArgsConstructor;
    import org.example.new_chatly_backend.dto.messageDTO.*;
    import org.example.new_chatly_backend.entity.conversationEntity.ConversationEntity;
    import org.example.new_chatly_backend.entity.conversationEntity.ConversationParticipantEntity;
    import org.example.new_chatly_backend.entity.messageEntity.MessageEntity;
    import org.example.new_chatly_backend.entity.messageEntity.MessageStatus;
    import org.example.new_chatly_backend.entity.messageEntity.MessageType;
    import org.example.new_chatly_backend.entity.userEntity.UserEntity;
    import org.example.new_chatly_backend.exception.UserNotFoundException;
    import org.example.new_chatly_backend.repository.*;
    import org.example.new_chatly_backend.service.conversationService.ConversationServiceImpl;
    import org.example.new_chatly_backend.service.conversationService.FileStorageService;
    import org.springframework.data.domain.PageRequest;
    import org.springframework.messaging.simp.SimpMessagingTemplate;
    import org.springframework.stereotype.Service;
    import org.springframework.web.multipart.MultipartFile;

    import java.security.Principal;
    import java.time.Instant;
    import java.util.*;
    import java.util.stream.Collectors;

    @Service
    @RequiredArgsConstructor
    public class MessageServiceImpl implements MessageService {

        private final ConversationRepository conversationRepo;
        private final UserRepository userRepo;
        private final MessageRepository messageRepository;
        private final SimpMessagingTemplate messagingTemplate; // ✅ inject this
        private final ConversationParticipantRepository participantRepository;
        private final ConversationServiceImpl conversationService;
        private final FileStorageService fileStorageService;
        private final BlockedUserRepository blockedUserRepo;

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

            conversation.getParticipants().forEach(cp -> {
                if (cp.isDeletedForUser()) {
                    cp.setDeletedForUser(false);
    //                cp.setDeletedAt(null);
                }
            });
            participantRepository.saveAll(conversation.getParticipants());

            conversationService.broadcastConversationToParticipants(conversation);

            MessageResponseDTO response = MessageResponseDTO.builder()
                    .id(saved.getId())
                    .clientMessageId(request.getClientMessageId())
                    .conversationId(conversationId)
                    .senderId(senderId)
                    .type(request.getType())
                    .content(request.getContent())
                    .timestamp(saved.getCreatedAt())
                    .reactions(saved.getReactions())
                    .status(MessageStatus.SENT)
                    .build();

            // ✅ send real-time update via WebSocket
            System.out.println("🟢 Sending WebSocket message to /topic/conversations/" + conversationId);
            System.out.println("Payload: " + response);
            messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, response);

    //        messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, response);

            return response;
        }


        // -------------- NEW: MEDIA MESSAGE (image / video / audio / doc) -----------------
        public MessageResponseDTO sendMediaMessage(String conversationId,
                                                   Principal principal,
                                                   MultipartFile file,
                                                   String typeStr,
                                                   String clientMessageId,
                                                   HttpServletRequest servletRequest, String caption) {

            if (file == null || file.isEmpty()) {
                throw new IllegalArgumentException("File is required");
            }

            String senderId = principal.getName();

            ConversationEntity conversation = conversationRepo.findById(conversationId)
                    .orElseThrow(() -> new UserNotFoundException("Conversation not found"));

            UserEntity sender = userRepo.findById(senderId)
                    .orElseThrow(() -> new UserNotFoundException("Sender not found"));

            if (clientMessageId != null) {
                messageRepository.findByClientMessageIdAndConversationId(clientMessageId, conversationId)
                        .ifPresent(m -> { throw new IllegalStateException("Message already exists"); });
            }

            // ---- detect MessageType ----
            MessageType type = MessageType.DOCUMENT; // default

            if (typeStr != null && !typeStr.isBlank()) {
                try {
                    type = MessageType.valueOf(typeStr.toUpperCase());
                } catch (IllegalArgumentException ignored) {}
            } else {
                String mime = file.getContentType() != null ? file.getContentType() : "";
                if (mime.startsWith("image/")) type = MessageType.IMAGE;
                else if (mime.startsWith("video/")) type = MessageType.VIDEO;
                else if (mime.startsWith("audio/")) type = MessageType.AUDIO;
            }

            // ---- upload file and store URL in content ----
            String baseUrl = servletRequest.getScheme() + "://" +
                    servletRequest.getServerName() + ":" +
                    servletRequest.getServerPort();

            String fileUrl = fileStorageService.upload(file, baseUrl);

            MessageEntity m = MessageEntity.builder()
                    .conversation(conversation)
                    .sender(sender)
                    .content(fileUrl)           // ⬅️ URL of file
                    .type(type)
                    .caption(caption)
                    .clientMessageId(clientMessageId)
                    .createdAt(Instant.now())
                    .status(MessageStatus.SENT)
                    .build();


            MessageEntity saved = messageRepository.save(m);

            conversation.getParticipants().forEach(cp -> {
                if (cp.isDeletedForUser()) {
                    cp.setDeletedForUser(false);
                }
            });
            participantRepository.saveAll(conversation.getParticipants());
            conversationService.broadcastConversationToParticipants(conversation);

            MessageResponseDTO response = MessageResponseDTO.builder()
                    .id(saved.getId())
                    .clientMessageId(saved.getClientMessageId())
                    .conversationId(conversationId)
                    .senderId(senderId)
                    .type(saved.getType())
                    .content(saved.getContent())       // file URL
                    .timestamp(saved.getCreatedAt())
                    .reactions(saved.getReactions())
                    .status(saved.getStatus())
                    .build();

            messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, response);
            return response;
        }

        @Override
        public MessageAckResponseDTO acknowledgeMessage(String conversationId,
                                                        MessageAckRequestDTO request,
                                                        Principal principal) {
            MessageEntity message = messageRepository.findById(request.getMessageId())
                    .orElseThrow(() -> new UserNotFoundException("Message not found"));

            if (!message.getConversation().getId().equals(conversationId)) {
                throw new IllegalArgumentException("Message does not belong to this conversation");
            }

            String userId = principal.getName();
            UserEntity user = userRepo.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException("User not found"));

            if ("delivered".equalsIgnoreCase(request.getStatus())) {

                // 🔥 IMPORTANT: load existing deliveredTo from DB
                message.getDeliveredTo().size();

                message.getDeliveredTo().add(user);
                if (message.getDeliveredAt() == null) {
                    message.setDeliveredAt(
                            request.getDeliveredAt() != null ? request.getDeliveredAt() : Instant.now()
                    );
                }

            } else if ("read".equalsIgnoreCase(request.getStatus())) {

                // 🔥 Load both collections from DB
                message.getDeliveredTo().size();
                message.getReadBy().size();

                message.getReadBy().add(user);
                message.setReadAt(request.getReadAt() != null ? request.getReadAt() : Instant.now());

                message.getDeliveredTo().add(user);
                if (message.getDeliveredAt() == null) {
                    message.setDeliveredAt(Instant.now());
                }
            }

            // ✅ 1) Pehle DB me join-table rows save karo
            messageRepository.save(message);

            // ✅ 2) Ab DB counts sahi aayenge
            recomputeGlobalStatus(message);

            // ✅ 3) Status change persist karo
            MessageEntity updated = messageRepository.save(message);

            MessageAckResponseDTO response = MessageAckResponseDTO.builder()
                    .messageId(updated.getId())
                    .status(updated.getStatus())
                    .build();

            messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, response);

            return response;
        }



        @Override
        public MessageReadResponseDTO markMessagesAsRead(String conversationId,
                                                         MessageReadRequestDTO request,
                                                         Principal principal) {


            if (request == null || request.getMessageIds() == null || request.getMessageIds().isEmpty()) {
                throw new IllegalArgumentException("messageIds required");
            }

            int updatedCount = 0;
            String userId = principal.getName();
            UserEntity user = userRepo.findById(userId).orElseThrow();

            List<String> validIds = request.getMessageIds().stream()
                    .filter(id -> id != null && !id.isBlank())
                    .collect(Collectors.toList());

            if (validIds.isEmpty()) {
                // nothing valid to do — return 0 changed (or throw 400 if you prefer)
                return MessageReadResponseDTO.builder().markedRead(0).build();
            }

            for (String messageId : validIds) {
                MessageEntity message = messageRepository.findById(messageId).orElse(null);
                if (message == null) continue;
                if (message.getSender().getId().equals(userId)) continue;

                // 🔥 Load existing sets
                message.getDeliveredTo().size();
                message.getReadBy().size();

                if (message.getReadBy().contains(user)) continue;

                message.getReadBy().add(user);
                message.setReadAt(Instant.now());

                message.getDeliveredTo().add(user);
                if (message.getDeliveredAt() == null) {
                    message.setDeliveredAt(Instant.now());
                }

                // ✅ 1) Pehle join-table + timestamps save karo
                messageRepository.save(message);

                // ✅ 2) Ab DB counters sahi aayenge
                recomputeGlobalStatus(message);

                // ✅ 3) Status persist
                messageRepository.save(message);

                updatedCount++;

                messagingTemplate.convertAndSend(
                        "/topic/conversations/" + conversationId,
                        MessageAckResponseDTO.builder()
                                .messageId(message.getId())
                                .status(message.getStatus())
                                .build()
                );
            }

            ConversationEntity conversation = conversationRepo.findById(conversationId)
                    .orElseThrow(() -> new RuntimeException("Conversation not found"));

            conversationService.broadcastConversationToParticipants(conversation);

            return MessageReadResponseDTO.builder()
                    .markedRead(updatedCount)
                    .build();
        }



        public MessageEditResponseDTO editMessage(String conversationId, String messageId,
                                                  MessageEditRequestDTO request, Principal principal) {
            var senderId = principal.getName();

            var message = messageRepository.findById(messageId)
                    .orElseThrow(() -> new IllegalArgumentException("Message not found"));

            if (!message.getConversation().getId().equals(conversationId)) {
                throw new IllegalArgumentException("Message does not belong to this conversation");
            }

            if (!message.getSender().getId().equals(senderId)) {
                throw new IllegalStateException("You can only edit your own messages");
            }

            // ✅ DB update
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

            // ✅ WebSocket payload -> event style
            Map<String, Object> payload = new HashMap<>();
            payload.put("event", "MESSAGE_EDIT");
            payload.put("conversationId", conversationId);

            Map<String, Object> data = new HashMap<>();
            data.put("id", message.getId());
            data.put("content", message.getContent());
            data.put("edited", true);
            data.put("editedAt", message.getEditedAt());

            payload.put("data", data);

            // 🔔 Sab participants ko edit event
            messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, payload);

            return response;
        }


        @Override
        public Map<String, Object> deleteMessage(String conversationId, String messageId, String scope, Principal principal) {
            String userId = principal.getName();



            if (messageId == null || messageId.isBlank()) {
                throw new IllegalArgumentException("messageId is required");
            }

            MessageEntity message = messageRepository.findById(messageId)
                    .orElseThrow(() -> new RuntimeException("Message not found"));

            if (!message.getConversation().getId().equals(conversationId)) {
                throw new RuntimeException("Message does not belong to this conversation");
            }

            UserEntity user = userRepo.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException("User not found"));

            Map<String, Object> response = new HashMap<>();

            // -----------------------
            // 1) DELETE FOR ME
            // -----------------------
            if ("ME".equalsIgnoreCase(scope)) {

                message.getDeletedForUsers().add(user);
                messageRepository.save(message);

                // 🔥 find last visible message FOR THIS USER

                List<MessageEntity> list =
                        messageRepository.findLastVisibleForUser(
                                conversationId,
                                user.getId(),
                                PageRequest.of(0, 1) // 🔥 LIMIT 1
                        );

                MessageEntity lastVisible =
                        list.isEmpty() ? null : list.get(0);

                Map<String, Object> payload = new HashMap<>();
                payload.put("event", "MESSAGE_DELETE");
                payload.put("conversationId", conversationId);

                Map<String, Object> data = new HashMap<>();
                data.put("scope", "ME");
                data.put("userId", user.getId());
                data.put("hasLastMessage", lastVisible != null);
                data.put("lastMessage",
                        lastVisible != null ? lastVisible.getContent() : "");
                data.put("lastMessageAt",
                        lastVisible != null ? lastVisible.getCreatedAt() : Instant.now());

                payload.put("data", data);

                // ✅ SEND ONLY TO THIS USER
                messagingTemplate.convertAndSend(
                        "/topic/conversations/" + conversationId,
                        payload
                );


                return data;
            }


            // -----------------------
            // 2) DELETE FOR EVERYONE
            // -----------------------
            if ("EVERYONE".equalsIgnoreCase(scope)) {

                // Optional: only sender can delete for everyone
                if (!message.getSender().getId().equals(userId)) {
                    throw new IllegalStateException("You can delete for everyone only for your own messages");
                }

                // Optional: time limit check (e.g. 2 min) – your choice
                // if (message.getCreatedAt().isBefore(Instant.now().minusSeconds(120))) {...}

                // Option A: hard delete from DB:
                // messageRepository.delete(message);

                // Option B: soft delete + “This message was deleted”
                message.setDeleted(true);
                message.setDeletedFor("everyone");
                message.setDeletedAt(Instant.now());
                message.setContent("This message was deleted");
                message.setType(MessageType.TEXT);
                message.getReactions().clear();

                messageRepository.save(message);

                // 🔔 WebSocket event so all clients can update UI
                Map<String, Object> payload = new HashMap<>();
                payload.put("event", "MESSAGE_DELETE");
                payload.put("conversationId", conversationId);

                Map<String, Object> data = new HashMap<>();
                data.put("messageId", message.getId());
                data.put("scope", "EVERYONE");
                data.put("deletedAt", message.getDeletedAt());

                payload.put("data", data);

                messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, payload);

                // 🔔 Conversation summary update (optional, nice to have)
                Map<String, Object> summary = new HashMap<>();
                summary.put("event", "CONVERSATION_UPDATE");
                summary.put("conversationId", conversationId);
                summary.put("lastMessage", "This message was deleted");
                summary.put("lastMessageAt", message.getDeletedAt());

                messagingTemplate.convertAndSend("/topic/conversations/" + conversationId, summary);


                return data;
            }

            throw new IllegalArgumentException("Invalid scope. Use ME or EVERYONE");
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
        public Map<String, Object> getMessages(String conversationId, int limit, String before, Principal principal) {

            String userId = principal.getName();
            UserEntity currentUser = userRepo.findById(userId).orElseThrow(()->new UserNotFoundException("Current user not found"));
            ConversationEntity conversation = conversationRepo.findById(conversationId)
                    .orElseThrow(() -> new RuntimeException("Conversation not found"));

            ConversationParticipantEntity cp = conversation.getParticipants().stream()
                    .filter(p -> p.getUser().getId().equals(userId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("User not part of this conversation"));

            // 👇 ye hi magic line hai
            Instant deletedAt = cp.getDeletedAt();  // is user ne kab "clear chat" kiya tha

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

            // 🧹 PER-USER LOCAL CLEAR:
            // agar user ne kabhi delete kiya hai, to uss time se pehle ke messages hata do
            if (deletedAt != null) {
                messages = messages.stream()
                        .filter(m -> m.getCreatedAt().isAfter(deletedAt))   // STRICTLY after
                        .collect(Collectors.toList());
            }

            messages = messages.stream()
                    .filter(m -> m.getDeletedForUsers() == null || !m.getDeletedForUsers().contains(currentUser))
                    .collect(Collectors.toList());

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
                            .reactions(msg.getReactions())
                            .build())
                    .toList();

            // ❗IMPORTANT: hasMore ab "limit" se based rakho, totalCount se nahi
            // Kyunki purane (deletedAt se pehle wale) messages hum waise bhi nahi dikhayenge
            boolean hasMore = messageDTOs.size() == limit;

            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("conversationId", conversationId);
            resultMap.put("messages", messageDTOs);
            resultMap.put("hasMore", hasMore);

            return resultMap;
        }





        public void markAllAsDeliveredForUser(Principal principal) {
            String userId = principal.getName();
            UserEntity user = userRepo.findById(userId).orElseThrow();

            List<MessageEntity> pending = messageRepository.findPendingForUser(userId);
            if (pending.isEmpty()) return;

            Instant now = Instant.now();

            for (MessageEntity m : pending) {

                // 🔥 Load existing deliveredTo from DB
                m.getDeliveredTo().size();

                if (!m.getDeliveredTo().contains(user)) {
                    m.getDeliveredTo().add(user);
                    if (m.getDeliveredAt() == null) {
                        m.setDeliveredAt(now);
                    }

                    // 1) save join-table
                    messageRepository.save(m);

                    // 2) recompute using DB counts
                    recomputeGlobalStatus(m);

                    // 3) save status
                    messageRepository.save(m);
                }
            }

            for (MessageEntity m : pending) {
                MessageAckResponseDTO ack = MessageAckResponseDTO.builder()
                        .messageId(m.getId())
                        .status(m.getStatus())
                        .build();

                messagingTemplate.convertAndSend(
                        "/topic/conversations/" + m.getConversation().getId(),
                        ack
                );
            }
        }



        private void recomputeGlobalStatus(MessageEntity message) {
            ConversationEntity conversation = message.getConversation();
            String senderId = message.getSender().getId();

            // 1️⃣ Total receivers = participants - sender
            long totalReceivers = conversation.getParticipants().stream()
                    .map(cp -> cp.getUser().getId())
                    .filter(id -> !id.equals(senderId))
                    .distinct()
                    .count();

            if (totalReceivers == 0) {
                message.setStatus(MessageStatus.SENT);
                return;
            }

            // 2️⃣ DB se delivered / read counts (distinct users)
            long deliveredCount = messageRepository.countDeliveredReceivers(message.getId());
            long readCount      = messageRepository.countReadReceivers(message.getId());

            System.out.println("🔎 recomputeGlobalStatus msg=" + message.getId()
                    + " totalReceivers=" + totalReceivers
                    + " deliveredCount=" + deliveredCount
                    + " readCount=" + readCount);

            // 3️⃣ WhatsApp style logic:
            //
            // SENT      -> sabko deliver nahi hua
            // DELIVERED -> sabko deliver hua, but sabne read nahi kiya
            // SEEN      -> sabne read kiya

            if (readCount >= totalReceivers) {
                message.setStatus(MessageStatus.SEEN);       // ✅ double blue
            } else if (deliveredCount >= totalReceivers) {
                message.setStatus(MessageStatus.DELIVERED);  // ✅ double grey
            } else {
                message.setStatus(MessageStatus.SENT);       // ✅ single tick
            }
        }









    }




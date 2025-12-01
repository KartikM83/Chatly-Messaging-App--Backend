package org.example.new_chatly_backend.service.conversationService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.User;
import org.example.new_chatly_backend.dto.conversationDTO.ArchivedResponseDTO;
import org.example.new_chatly_backend.dto.conversationDTO.ConversationResponseDTO;
import org.example.new_chatly_backend.dto.conversationDTO.CreateConversationRequest;
import org.example.new_chatly_backend.dto.conversationDTO.ParticipantResponseDTO;
import org.example.new_chatly_backend.entity.conversationEntity.ConversationEntity;
import org.example.new_chatly_backend.entity.conversationEntity.ConversationParticipantEntity;
import org.example.new_chatly_backend.entity.conversationEntity.ConversationType;
import org.example.new_chatly_backend.entity.messageEntity.MessageEntity;
import org.example.new_chatly_backend.entity.userEntity.UserEntity;
import org.example.new_chatly_backend.exception.UserNotFoundException;

import org.example.new_chatly_backend.repository.ConversationParticipantRepository;
import org.example.new_chatly_backend.repository.ConversationRepository;
import org.example.new_chatly_backend.repository.MessageRepository;
import org.example.new_chatly_backend.repository.UserRepository;
import org.example.new_chatly_backend.utility.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {
    private final UserRepository userRepo;
    private final ConversationRepository conversationRepo;
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepo;
    private final FileStorageService fileStorageService;
    private final SimpMessagingTemplate messagingTemplate;


    @Override
    public ConversationResponseDTO createConversation(CreateConversationRequest request, HttpServletRequest servletRequest) {

        String currentUserId = JwtUtil.extractUserIdFromRequest(servletRequest);


        UserEntity currentUser = userRepo.findById(currentUserId).orElseThrow(() -> new UserNotFoundException("Current user not found"));

        if (ConversationType.DIRECT.equals(request.getType())) {

            UserEntity participant = userRepo.findById(request.getParticipantId()).orElseThrow(() -> new UserNotFoundException("Participant not found"));

            Optional<ConversationEntity> existing = conversationRepo.findDirectConversationBetweenUsers(currentUserId, participant.getId());

            if (existing.isPresent()) {
                ConversationEntity existingConversation = existing.get();
                return mapToConversationResponseDTO(existingConversation,currentUserId);
            }

            ConversationEntity conversation = new ConversationEntity();
            conversation.setType(ConversationType.DIRECT);
            conversation.setCreatedAt(Instant.now());
            conversation.setAdmin(currentUser.getId());


            Set<ConversationParticipantEntity> participants = new HashSet<>();

            ConversationParticipantEntity cp1 = ConversationParticipantEntity.builder()
                    .user(currentUser)
                    .build();
            cp1.setConversation(conversation);
            participants.add(cp1);

            ConversationParticipantEntity cp2 = ConversationParticipantEntity.builder()
                    .user(participant)
                    .build();
            cp2.setConversation(conversation);
            participants.add(cp2);

            conversation.setParticipants(participants);


            ConversationEntity savedConversation = conversationRepo.save(conversation);
            return mapToConversationResponseDTO(savedConversation,currentUserId);


        } else if (ConversationType.GROUP.equals(request.getType())) {

            Set<String> requestedMemberIds = new HashSet<>(request.getMemberIds());
            requestedMemberIds.add(currentUserId);

            List<ConversationEntity> sameNameGroups =
                    conversationRepo.findGroupByNameAndAdminId(request.getName(), currentUserId);

            for (ConversationEntity existingGroup : sameNameGroups) {
                Set<String> existingMemberIds = existingGroup.getParticipants().stream()
                        .map(cp -> cp.getUser().getId())
                        .collect(Collectors.toSet());

                // ✅ Check for identical members
                if (existingMemberIds.equals(requestedMemberIds)) {
                    return mapToConversationResponseDTO(existingGroup,currentUserId);
                }
            }

            // ✅ Create new group
            ConversationEntity conversation = new ConversationEntity();
            conversation.setType(ConversationType.GROUP);
            conversation.setName(request.getName());
            conversation.setProfileImage(request.getGroupProfileImage());
            conversation.setAdmin(currentUserId);
            conversation.setCreatedAt(Instant.now());

            System.out.println(conversation);
            System.err.println("Creating group = " + conversation);



            Set<ConversationParticipantEntity> participants = new HashSet<>();
            for (String memberId : requestedMemberIds) {
                UserEntity member = userRepo.findById(memberId)
                        .orElseThrow(() -> new RuntimeException("User not found: " + memberId));
                ConversationParticipantEntity participant = ConversationParticipantEntity.builder()
                        .conversation(conversation)
                        .user(member)
                        .build();
                participants.add(participant);
            }
            for (ConversationParticipantEntity p : participants) {
                p.setConversation(conversation);
            }
            conversation.setParticipants(participants);

            ConversationEntity saved = conversationRepo.save(conversation);

            return mapToConversationResponseDTO(saved,currentUserId);
        }

        // ❌ Invalid Type
        throw new RuntimeException("Invalid conversation type: " + request.getType());
    }

    public ConversationResponseDTO createConversation(CreateConversationRequest request,
                                                      MultipartFile file,
                                                      HttpServletRequest servletRequest) {

        String currentUserId = JwtUtil.extractUserIdFromRequest(servletRequest);
        UserEntity currentUser = userRepo.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException("Current user not found"));

        // base URL for uploads (same as update)
        String baseUrl = servletRequest.getScheme() + "://" +
                servletRequest.getServerName() + ":" +
                servletRequest.getServerPort();

        if (ConversationType.DIRECT.equals(request.getType())) {
            // === DIRECT CHAT LOGIC (unchanged) ===
            UserEntity participant = userRepo.findById(request.getParticipantId())
                    .orElseThrow(() -> new UserNotFoundException("Participant not found"));

            Optional<ConversationEntity> existing =
                    conversationRepo.findDirectConversationBetweenUsers(currentUserId, participant.getId());

            if (existing.isPresent()) {
                ConversationEntity existingConversation = existing.get();

                // ✅ Notify both users that this conversation exists / was reopened
                broadcastConversationToParticipants(existingConversation);
                return mapToConversationResponseDTO(existingConversation,currentUserId);
            }

            ConversationEntity conversation = new ConversationEntity();
            conversation.setType(ConversationType.DIRECT);
            conversation.setCreatedAt(Instant.now());
            conversation.setAdmin(currentUser.getId());


            Set<ConversationParticipantEntity> participants = new HashSet<>();

            ConversationParticipantEntity cp1 = ConversationParticipantEntity.builder()
                    .user(currentUser)
                    .build();
            cp1.setConversation(conversation);
            participants.add(cp1);

            ConversationParticipantEntity cp2 = ConversationParticipantEntity.builder()
                    .user(participant)
                    .build();
            cp2.setConversation(conversation);
            participants.add(cp2);

            conversation.setParticipants(participants);

            ConversationEntity savedConversation = conversationRepo.save(conversation);
            broadcastConversationToParticipants(savedConversation);
            return mapToConversationResponseDTO(savedConversation,currentUserId);

        } else if (ConversationType.GROUP.equals(request.getType())) {

            // === GROUP CHAT LOGIC ===
            Set<String> requestedMemberIds = new HashSet<>(request.getMemberIds());
            requestedMemberIds.add(currentUserId);

            List<ConversationEntity> sameNameGroups =
                    conversationRepo.findGroupByNameAndAdminId(request.getName(), currentUserId);

            for (ConversationEntity existingGroup : sameNameGroups) {
                Set<String> existingMemberIds = existingGroup.getParticipants().stream()
                        .map(cp -> cp.getUser().getId())
                        .collect(Collectors.toSet());

                if (existingMemberIds.equals(requestedMemberIds)) {
                    broadcastConversationToParticipants(existingGroup);
                    return mapToConversationResponseDTO(existingGroup,currentUserId);
                }
            }

            ConversationEntity conversation = new ConversationEntity();
            conversation.setType(ConversationType.GROUP);
            conversation.setName(request.getName());
            conversation.setAdmin(currentUserId);
            conversation.setCreatedAt(Instant.now());

            // ✅ group profile image:
            // 1) if file uploaded → upload & use URL
            // 2) else fall back to request.getGroupProfileImage()
            if (file != null && !file.isEmpty()) {
                String imageUrl = fileStorageService.upload(file, baseUrl);
                conversation.setProfileImage(imageUrl);
            } else if (request.getGroupProfileImage() != null) {
                conversation.setProfileImage(request.getGroupProfileImage());
            }

            Set<ConversationParticipantEntity> participants = new HashSet<>();
            for (String memberId : requestedMemberIds) {
                UserEntity member = userRepo.findById(memberId)
                        .orElseThrow(() -> new RuntimeException("User not found: " + memberId));
                ConversationParticipantEntity participant = ConversationParticipantEntity.builder()
                        .conversation(conversation)
                        .user(member)
                        .build();
                participants.add(participant);
            }
            conversation.setParticipants(participants);

            ConversationEntity saved = conversationRepo.save(conversation);
            broadcastConversationToParticipants(saved);
            return mapToConversationResponseDTO(saved,currentUserId);
        }

        throw new RuntimeException("Invalid conversation type: " + request.getType());
    }







    @Override
    public ConversationResponseDTO getConversationById(String conversationId, HttpServletRequest servletRequest) {
        String currentUserId = JwtUtil.extractUserIdFromRequest(servletRequest);
        UserEntity currentUser = userRepo.findById(currentUserId).orElseThrow(()->new UserNotFoundException("Current user not found"));

        ConversationEntity conversation = conversationRepo.findById(conversationId).orElseThrow(()->new RuntimeException("Conversation not found"));

        boolean isParticipant = conversation.getParticipants().stream()
                .anyMatch(cp -> cp.getUser().getId().equals(currentUserId));

        if (!isParticipant) {
            throw new RuntimeException("Access denied: You are not part of this conversation");
        }

        return mapToConversationResponseDTO(conversation,currentUserId);
    }

    @Override
    public ArchivedResponseDTO archiveConversation(String conversationId, HttpServletRequest servletRequest) {
        String currentUserId = JwtUtil.extractUserIdFromRequest(servletRequest);
        UserEntity currentUser = userRepo.findById(currentUserId).orElseThrow(()->new UserNotFoundException("Current user not found"));
        System.out.println("Current user"+currentUserId);

        ConversationParticipantEntity participant = participantRepository.findByConversation_IdAndUser_Id(conversationId, currentUserId)
                .orElseThrow(() -> new RuntimeException("Access denied: You are not part of this conversation"));

        participant.setArchived(true);
        participantRepository.save(participant);

        return ArchivedResponseDTO.builder()
                .conversationId(conversationId)
                .archived(participant.isArchived())
                .build();



    }

    @Override
    public ArchivedResponseDTO unarchiveConversation(String conversationId, HttpServletRequest servletRequest) {
        String currentUserId = JwtUtil.extractUserIdFromRequest(servletRequest);
        UserEntity currentUser = userRepo.findById(currentUserId).orElseThrow(()->new UserNotFoundException("Current user not found"));

        ConversationParticipantEntity participant = participantRepository.findByConversation_IdAndUser_Id(conversationId, currentUserId)
                .orElseThrow(() -> new RuntimeException("Access denied: You are not part of this conversation"));

        participant.setArchived(false);
        participantRepository.save(participant);

        return ArchivedResponseDTO.builder()
                .conversationId(conversationId)
                .archived(participant.isArchived())
                .build();
    }

    @Override
    public String deleteConversation(String conversationId, HttpServletRequest servletRequest) {
        String currentUserId = JwtUtil.extractUserIdFromRequest(servletRequest);
        UserEntity currentUser = userRepo.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException("Current user not found"));

        ConversationParticipantEntity participant = participantRepository
                .findByConversation_IdAndUser_Id(conversationId, currentUserId)
                .orElseThrow(() -> new RuntimeException("Access denied: You are not part of this conversation"));

        // ❌ NO: participantRepository.delete(participant);

        // ✅ Soft delete / hide for user
        participant.setDeletedForUser(true);
        participant.setDeletedAt(Instant.now());
        participantRepository.save(participant);

        return "Conversation hidden for user";
    }





    @Override
    public List<ConversationResponseDTO> getUserConversations(String userId) {
        // 1) Get all conversations where this user is a participant
        List<ConversationEntity> conversations = conversationRepo.findByParticipantId(userId);

        // 2) Map to DTOs (this already sets lastMessage, lastMessageAt, unreadCount)
        List<ConversationResponseDTO> dtos = conversations.stream()
                .map(conversation -> mapToConversationResponseDTO(conversation, userId))
                .collect(Collectors.toList());

        // 3) Sort by last activity: lastMessageAt (if exists) else createdAt
        dtos.sort(
                Comparator.comparing(
                        (ConversationResponseDTO c) ->
                                c.getLastMessageAt() != null ? c.getLastMessageAt() : c.getCreatedAt()
                ).reversed() // newest first
        );

        return dtos;
    }



    @Override
    public ConversationResponseDTO updateConversation(CreateConversationRequest request,
                                                      MultipartFile file,
                                                      HttpServletRequest servletRequest,
                                                      String conversationId) {

        ConversationEntity conversation = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("conversation not found"));

        // ensure non-null request object
        if (request == null) {
            request = new CreateConversationRequest();
        }

        if (request.getName() != null) {
            conversation.setName(request.getName());
        }



        String baseUrl = servletRequest.getScheme() + "://" +
                servletRequest.getServerName() + ":" +
                servletRequest.getServerPort();

        if (file != null && !file.isEmpty()) {
            String imageUrl = fileStorageService.upload(file, baseUrl);
            conversation.setProfileImage(imageUrl);
        }

        String currentUserId = JwtUtil.extractUserIdFromRequest(servletRequest);
        conversationRepo.save(conversation);
        return mapToConversationResponseDTO(conversation,currentUserId);
    }









    private ConversationResponseDTO mapToConversationResponseDTO(ConversationEntity conversation,
                                                                 String currentUserId) {

        List<ParticipantResponseDTO> participantDTOs = conversation.getParticipants()
                .stream()
                .map(cp -> ParticipantResponseDTO.builder()
                        .id(cp.getUser().getId())
                        .name(cp.getUser().getName())
                        .profileImage(cp.getUser().getProfileImage())
                        .build())
                .toList();

        // find THIS user's participant row
        Optional<ConversationParticipantEntity> meOpt = conversation.getParticipants()
                .stream()
                .filter(cp -> cp.getUser().getId().equals(currentUserId))
                .findFirst();

        boolean archivedForMe = meOpt.map(ConversationParticipantEntity::isArchived).orElse(false);
        boolean pinnedForMe   = meOpt.map(ConversationParticipantEntity::isPinned).orElse(false);
        long unreadCount = messageRepo.countUnreadMessages(conversation.getId(), currentUserId);

        // 🔹 last message
        List<MessageEntity> lastMessages = messageRepo
                .findTopByConversationIdOrderByCreatedAtDesc(
                        conversation.getId(),
                        org.springframework.data.domain.PageRequest.of(0, 1)
                );

        Optional<MessageEntity> lastMessageOpt =
                lastMessages.isEmpty() ? Optional.empty() : Optional.of(lastMessages.get(0));

        return ConversationResponseDTO.builder()
                .id(conversation.getId())
                .type(conversation.getType())
                .groupName(conversation.getName())
                .participants(participantDTOs)
                .adminId(conversation.getAdmin())
                .groupProfileImage(conversation.getProfileImage())
                .createdAt(conversation.getCreatedAt())
                .archived(archivedForMe)   // 👈 per user
                .pinned(pinnedForMe)
                .lastMessage(lastMessageOpt.map(MessageEntity::getContent).orElse(null))
                .lastMessageAt(lastMessageOpt.map(MessageEntity::getCreatedAt).orElse(null))
                .unreadCount(unreadCount)
                .typing(false)             // or null; typing should be via websocket
                .build();
    }


    public void broadcastConversationToParticipants(ConversationEntity conversation) {


        // Send to each participant's personal topic
        conversation.getParticipants().forEach(cp -> {
            String userId = cp.getUser().getId();
            ConversationResponseDTO dto = mapToConversationResponseDTO(conversation, userId);
            String destination = "/topic/users/" + userId + "/conversations";
            System.out.println("🔔 Broadcasting conversation " + dto.getId() + " to " + destination);
            messagingTemplate.convertAndSend(destination, dto);
        });
    }


}


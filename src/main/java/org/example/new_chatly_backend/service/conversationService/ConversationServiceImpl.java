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


    @Override
    public ConversationResponseDTO createConversation(CreateConversationRequest request, HttpServletRequest servletRequest) {

        String currentUserId = JwtUtil.extractUserIdFromRequest(servletRequest);


        UserEntity currentUser = userRepo.findById(currentUserId).orElseThrow(() -> new UserNotFoundException("Current user not found"));

        if (ConversationType.DIRECT.equals(request.getType())) {

            UserEntity participant = userRepo.findById(request.getParticipantId()).orElseThrow(() -> new UserNotFoundException("Participant not found"));

            Optional<ConversationEntity> existing = conversationRepo.findDirectConversationBetweenUsers(currentUserId, participant.getId());

            if (existing.isPresent()) {
                ConversationEntity existingConversation = existing.get();
                return mapToConversationResponseDTO(existingConversation);
            }

            ConversationEntity conversation = new ConversationEntity();
            conversation.setType(ConversationType.DIRECT);
            conversation.setCreatedAt(Instant.now());
            conversation.setAdmin(currentUser.getId());
            conversation.setArchived(false);
            conversation.setPinned(false);
            conversation.setTyping(false);

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
            return mapToConversationResponseDTO(savedConversation);


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
                    return mapToConversationResponseDTO(existingGroup);
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

            return mapToConversationResponseDTO(saved);
        }

        // ❌ Invalid Type
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

        return mapToConversationResponseDTO(conversation);
    }

    @Override
    public ArchivedResponseDTO archiveConversation(String conversationId, HttpServletRequest servletRequest) {
        String currentUserId = JwtUtil.extractUserIdFromRequest(servletRequest);
        UserEntity currentUser = userRepo.findById(currentUserId).orElseThrow(()->new UserNotFoundException("Current user not found"));

        ConversationParticipantEntity participant = participantRepository.findByConversation_IdAndUser_Id(conversationId, currentUserId)
                .orElseThrow(() -> new RuntimeException("Access denied: You are not part of this conversation"));

        participant.setArchived(true);
        participantRepository.save(participant);

        return ArchivedResponseDTO.builder()
                .conversationId(participant.getId())
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
                .conversationId(participant.getId())
                .archived(participant.isArchived())
                .build();
    }

    @Override
    public String deleteConversation(String conversationId, HttpServletRequest servletRequest) {
        String currentUserId = JwtUtil.extractUserIdFromRequest(servletRequest);
        UserEntity currentUser = userRepo.findById(currentUserId).orElseThrow(()->new UserNotFoundException("Current user not found"));

        ConversationParticipantEntity participant = participantRepository.findByConversation_IdAndUser_Id(conversationId, currentUserId)
                .orElseThrow(() -> new RuntimeException("Access denied: You are not part of this conversation"));

        participantRepository.delete(participant);

        return "Conversation is deleted";
    }

    @Override
    public List<ConversationResponseDTO> getUserConversations(String userId) {
        List<ConversationEntity> conversations = conversationRepo.findByParticipantId(userId);

        return conversations.stream().map(conversation -> {
            // ✅ Fetch last message safely using Pageable
            List<MessageEntity> lastMessages = messageRepo
                    .findTopByConversationIdOrderByCreatedAtDesc(conversation.getId(), org.springframework.data.domain.PageRequest.of(0, 1));

            Optional<MessageEntity> lastMessageOpt = lastMessages.isEmpty() ? Optional.empty() : Optional.of(lastMessages.get(0));

            // ✅ Unread count
            long unreadCount = messageRepo.countUnreadMessages(conversation.getId(), userId);

            // ✅ Convert participant entities to DTOs
            List<ParticipantResponseDTO> participants = conversation.getParticipants().stream()
                    .map(p -> ParticipantResponseDTO.builder()
                            .id(p.getUser().getId())
                            .name(p.getUser().getName())
                            .profileImage(p.getUser().getProfileImage())
                            .build())
                    .toList();

            return ConversationResponseDTO.builder()
                    .id(conversation.getId())
                    .type(conversation.getType())
                    .groupName(conversation.getName())
                    .participants(participants)
                    .adminId(conversation.getAdmin())
                    .createdAt(conversation.getCreatedAt())
                    .groupProfileImage(conversation.getProfileImage())
                    // ✅ Add recent message info
                    .lastMessage(lastMessageOpt.map(MessageEntity::getContent).orElse(null))
                    .lastMessageAt(lastMessageOpt.map(MessageEntity::getCreatedAt).orElse(null))
                    .unreadCount(unreadCount)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public ConversationResponseDTO updateConversation(CreateConversationRequest request,
                                                      MultipartFile file,
                                                      HttpServletRequest servletRequest,
                                                      String conversationId) {

        ConversationEntity conversation = conversationRepo.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("conversation not found"));

        // Update ONLY provided fields
        if (request.getName() != null) {
            conversation.setName(request.getName());
        }

        if (request.getArchived() != null) {
            conversation.setArchived(request.getArchived());
        }

        if (request.getPinned() != null) {
            conversation.setPinned(request.getPinned());
        }

        // Base URL for file upload
        String baseUrl = servletRequest.getScheme() + "://" +
                servletRequest.getServerName() + ":" +
                servletRequest.getServerPort();

        // Handle profile image update
        if (file != null && !file.isEmpty()) {
            String imageUrl = fileStorageService.upload(file, baseUrl);
            conversation.setProfileImage(imageUrl);
        }

        conversationRepo.save(conversation);
        return mapToConversationResponseDTO(conversation);
    }








    private ConversationResponseDTO mapToConversationResponseDTO(ConversationEntity conversation) {
        List<ParticipantResponseDTO> participantDTOs = conversation.getParticipants()
                .stream()
                .map(cp -> ParticipantResponseDTO.builder()
                        .id(cp.getUser().getId())
                        .name(cp.getUser().getName())
                        .profileImage(cp.getUser().getProfileImage())// may be null if user name not set
                        .build())
                .toList();

        return ConversationResponseDTO.builder()
                .id(conversation.getId())
                .type(conversation.getType()) // e.g., "direct"
                .groupName(conversation.getName())
                .participants(participantDTOs)
                .adminId(conversation.getAdmin())
                .groupProfileImage(conversation.getProfileImage())
                .createdAt(conversation.getCreatedAt())
                .archived(conversation.isArchived())
                .pinned(conversation.isPinned())
                .typing(conversation.isTyping())
                .build();
    }

}


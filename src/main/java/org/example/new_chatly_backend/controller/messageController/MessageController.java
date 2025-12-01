package org.example.new_chatly_backend.controller.messageController;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.new_chatly_backend.dto.messageDTO.*;
import org.example.new_chatly_backend.service.messageService.MessageService;
import org.example.new_chatly_backend.service.messageService.MessageServiceImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("conversation")
@CrossOrigin(origins = "http://localhost:5173/")
public class MessageController {

    private final MessageServiceImpl messageService;

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<Map<String, Object>> getMessages(
            @PathVariable String conversationId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String before,
            Principal principal
    ) {
        Map<String, Object> response = messageService.getMessages(conversationId, limit, before,principal);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<MessageResponseDTO> sendMessage(@PathVariable String conversationId , Principal principal, @RequestBody MessageRequestDTO request){

        MessageResponseDTO response =messageService.sendMessage(conversationId,principal,request);
        return ResponseEntity.ok(response);

    }

    @PostMapping("/{conversationId}/messages/ack")
    public ResponseEntity<MessageAckResponseDTO> acknowledgeMessage(
            @PathVariable String conversationId,
            Principal principal,
            @RequestBody MessageAckRequestDTO request) {

        MessageAckResponseDTO response = messageService.acknowledgeMessage(conversationId,request, principal);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{conversationId}/messages/read")
    public ResponseEntity<MessageReadResponseDTO> markMessagesAsRead(
            @PathVariable String conversationId,
            Principal principal,
            @RequestBody MessageReadRequestDTO request) {

        MessageReadResponseDTO response = messageService.markMessagesAsRead(conversationId, request, principal);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{conversationId}/messages/{messageId}")
    public ResponseEntity<MessageEditResponseDTO> editMessage(
            @PathVariable String conversationId,
            @PathVariable String messageId,
            Principal principal,
            @RequestBody MessageEditRequestDTO request) {

        MessageEditResponseDTO response = messageService.editMessage(conversationId, messageId, request, principal);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{conversationId}/messages/{messageId}")
    public ResponseEntity<?> deleteMessage(
            @PathVariable String conversationId,
            @PathVariable String messageId,
            Principal principal
    ) {
        var response = messageService.deleteMessage(conversationId, messageId, principal);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{conversationId}/messages/react")
    public ResponseEntity<Map<String, Object>> reactToMessage(
            @PathVariable String conversationId,
            @RequestBody Map<String, String> body,
            Principal principal
    ) {
        String messageId = body.get("messageId");
        String reaction = body.get("reaction");

        var response = messageService.reactToMessage(conversationId, messageId, reaction, principal);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/messages/sync-delivered")
    public ResponseEntity<Void> syncDelivered(Principal principal) {
        messageService.markAllAsDeliveredForUser(principal);
        return ResponseEntity.ok().build();
    }









}

package org.example.new_chatly_backend.controller.conversationController;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.new_chatly_backend.dto.conversationDTO.ArchivedResponseDTO;
import org.example.new_chatly_backend.dto.conversationDTO.ConversationResponseDTO;
import org.example.new_chatly_backend.dto.conversationDTO.CreateConversationRequest;
import org.example.new_chatly_backend.service.conversationService.ConversationServiceImpl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("conversations")
public class ConversationController {

    private final ConversationServiceImpl conversationService;

    @GetMapping
    public List<ConversationResponseDTO> getUserConversations(@RequestParam String userId) {
        return conversationService.getUserConversations(userId);
    }



    @PostMapping
    public ResponseEntity<ConversationResponseDTO> createConversation(
            @RequestBody CreateConversationRequest request,
            HttpServletRequest servletRequest){

        ConversationResponseDTO response = conversationService.createConversation(request,servletRequest);
        return ResponseEntity.ok(response);
    }

    @PutMapping(value = "/{conversationId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ConversationResponseDTO> updateConversation(
            @PathVariable String conversationId,
            @RequestPart(value="data", required = false) String dataJson,
            @RequestPart(value = "file", required = false) MultipartFile file,
            HttpServletRequest servletRequest
    ) {
        CreateConversationRequest request = null;
        try {
            if (dataJson != null && !dataJson.isBlank()) {
                ObjectMapper mapper = new ObjectMapper();
                request = mapper.readValue(dataJson, CreateConversationRequest.class);
            }
        } catch (Exception e) {
            throw new RuntimeException("Invalid JSON in 'data' part: " + e.getMessage(), e);
        }

        ConversationResponseDTO response =
                conversationService.updateConversation(request, file, servletRequest, conversationId);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/{conversationId}")
    public ResponseEntity<ConversationResponseDTO> getConversationById(@PathVariable String conversationId,HttpServletRequest servletRequest){
        ConversationResponseDTO response = conversationService.getConversationById(conversationId,servletRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{conversationId}/archive")
    public ResponseEntity<ArchivedResponseDTO> archiveConversation(@PathVariable String conversationId, HttpServletRequest servletRequest){
        ArchivedResponseDTO response =conversationService.archiveConversation(conversationId,servletRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{conversationId}/unarchive")
    public ResponseEntity<ArchivedResponseDTO> unarchiveConversation(@PathVariable String conversationId, HttpServletRequest servletRequest){
        ArchivedResponseDTO response =conversationService.unarchiveConversation(conversationId,servletRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{conversationId}/delete")
    public ResponseEntity<String> deleteConversation(@PathVariable String conversationId, HttpServletRequest servletRequest){
        String response =conversationService.deleteConversation(conversationId,servletRequest);
        return ResponseEntity.ok(response);
    }
}

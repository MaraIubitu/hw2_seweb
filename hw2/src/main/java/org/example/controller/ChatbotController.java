package org.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.example.service.ChatbotService;
import org.example.service.VectorDatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chat")
public class ChatbotController {

    @Autowired
    private ChatbotService chatbotService;

    @Autowired
    private VectorDatabaseService vectorDatabaseService;

    @PostMapping("/message")
    public ResponseEntity<Map<String, String>> sendMessage(
            @RequestBody Map<String, String> request) {
        
        String message = request.get("message");
        String pageContext = request.getOrDefault("pageContext", "");

        if (message == null || message.trim().isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Message cannot be empty");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            String reply = chatbotService.processChatQuery(message, pageContext);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", reply);
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error processing chat message", e);
            Map<String, String> response = new HashMap<>();
            response.put("error", "Error processing message: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/starters")
    public ResponseEntity<List<String>> getConversationStarters(
            @RequestParam(required = false, defaultValue = "") String pageContext) {
        
        List<String> starters = chatbotService.generateConversationStarters(pageContext);
        return ResponseEntity.ok(starters);
    }

    @PostMapping("/init")
    public ResponseEntity<Map<String, String>> initializeChat() {
        try {
            vectorDatabaseService.buildVectorDatabase();
            
            Map<String, String> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Chat initialized");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error initializing chat", e);
            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Error initializing chat: " + e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
}

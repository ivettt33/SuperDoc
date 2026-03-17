package com.superdoc.api.controller;

import com.superdoc.api.model.dto.ChatDtos.*;
import com.superdoc.api.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/chat", produces = "application/json")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping(consumes = "application/json")
    public ResponseEntity<ChatResponse> chat(
            @RequestBody @Valid ChatRequest request,
            @AuthenticationPrincipal String email) {
        ChatResponse response = chatService.getChatResponse(request);
        return ResponseEntity.ok(response);
    }
}


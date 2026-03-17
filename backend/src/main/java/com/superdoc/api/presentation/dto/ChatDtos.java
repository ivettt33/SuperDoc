package com.superdoc.api.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public class ChatDtos {
    public record ChatRequest(
            @NotBlank(message = "Message cannot be empty")
            @Size(max = 2000, message = "Message cannot exceed 2000 characters")
            String message,
            List<ChatMessage> conversationHistory
    ) {}

    public record ChatMessage(
            String role, // "user" or "assistant"
            String content
    ) {}

    public record ChatResponse(
            String response,
            List<ChatMessage> conversationHistory
    ) {}
}


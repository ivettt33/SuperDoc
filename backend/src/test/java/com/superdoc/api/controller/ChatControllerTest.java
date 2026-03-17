package com.superdoc.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.superdoc.api.model.dto.ChatDtos.*;
import com.superdoc.api.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static com.superdoc.api.controller.TestAuthHelper.withEmail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ChatController.class)
@Import(TestSecurityConfig.class)
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatService chatService;

    @Test
    void chat_validRequest_returnsOk() throws Exception {
        // Arrange
        String userEmail = "user@example.com";
        ChatRequest request = new ChatRequest("How do I book an appointment?", null);
        ChatResponse response = new ChatResponse(
                "To book an appointment, go to the appointments page...",
                List.of(
                        new ChatMessage("user", "How do I book an appointment?"),
                        new ChatMessage("assistant", "To book an appointment, go to the appointments page...")
                )
        );

        when(chatService.getChatResponse(any(ChatRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/chat")
                        .with(withEmail(userEmail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response").value("To book an appointment, go to the appointments page..."))
                .andExpect(jsonPath("$.conversationHistory").isArray())
                .andExpect(jsonPath("$.conversationHistory[0].role").value("user"))
                .andExpect(jsonPath("$.conversationHistory[1].role").value("assistant"));

        verify(chatService).getChatResponse(any(ChatRequest.class));
    }

    @Test
    void chat_withConversationHistory_returnsOk() throws Exception {
        // Arrange
        String userEmail = "user@example.com";
        List<ChatMessage> history = List.of(
                new ChatMessage("user", "Hello"),
                new ChatMessage("assistant", "Hi! How can I help?")
        );
        ChatRequest request = new ChatRequest("How do I book?", history);
        ChatResponse response = new ChatResponse(
                "To book an appointment...",
                List.of(
                        new ChatMessage("user", "Hello"),
                        new ChatMessage("assistant", "Hi! How can I help?"),
                        new ChatMessage("user", "How do I book?"),
                        new ChatMessage("assistant", "To book an appointment...")
                )
        );

        when(chatService.getChatResponse(any(ChatRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/chat")
                        .with(withEmail(userEmail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationHistory").isArray())
                .andExpect(jsonPath("$.conversationHistory.length()").value(4));

        verify(chatService).getChatResponse(any(ChatRequest.class));
    }

    @Test
    void chat_emptyMessage_returnsBadRequest() throws Exception {
        // Arrange
        String userEmail = "user@example.com";
        ChatRequest request = new ChatRequest("", null);

        // Act & Assert
        mockMvc.perform(post("/chat")
                        .with(withEmail(userEmail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(chatService, never()).getChatResponse(any(ChatRequest.class));
    }

    @Test
    void chat_nullMessage_returnsBadRequest() throws Exception {
        // Arrange
        String userEmail = "user@example.com";
        String invalidJson = "{\"message\":null}";

        // Act & Assert
        mockMvc.perform(post("/chat")
                        .with(withEmail(userEmail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verify(chatService, never()).getChatResponse(any(ChatRequest.class));
    }

    @Test
    void chat_messageTooLong_returnsBadRequest() throws Exception {
        // Arrange
        String userEmail = "user@example.com";
        String longMessage = "a".repeat(2001); // Exceeds 2000 character limit
        ChatRequest request = new ChatRequest(longMessage, null);

        // Act & Assert
        mockMvc.perform(post("/chat")
                        .with(withEmail(userEmail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(chatService, never()).getChatResponse(any(ChatRequest.class));
    }

    @Test
    void chat_missingMessage_returnsBadRequest() throws Exception {
        // Arrange
        String userEmail = "user@example.com";
        String invalidJson = "{\"conversationHistory\":[]}";

        // Act & Assert
        mockMvc.perform(post("/chat")
                        .with(withEmail(userEmail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verify(chatService, never()).getChatResponse(any(ChatRequest.class));
    }

    @Test
    void chat_invalidJson_returnsInternalServerError() throws Exception {
        // Arrange
        String userEmail = "user@example.com";
        String invalidJson = "{invalid json}";

        // Act & Assert
        // Invalid JSON causes a parsing error which returns 500
        mockMvc.perform(post("/chat")
                        .with(withEmail(userEmail))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isInternalServerError());

        verify(chatService, never()).getChatResponse(any(ChatRequest.class));
    }
}


package com.superdoc.api.service;

import com.superdoc.api.model.dto.ChatDtos.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @InjectMocks
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        // Disable OpenAI API key to test fallback FAQ system
        ReflectionTestUtils.setField(chatService, "apiKey", "");
        ReflectionTestUtils.setField(chatService, "model", "gpt-3.5-turbo");
        ReflectionTestUtils.setField(chatService, "maxTokens", 500);
    }

    @Test
    void getChatResponse_noApiKey_usesFallbackSystem() {
        // Arrange
        ChatRequest request = new ChatRequest("How do I book an appointment?", null);

        // Act
        ChatResponse response = chatService.getChatResponse(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.response()).isNotBlank();
        assertThat(response.response()).contains("book");
        assertThat(response.conversationHistory()).hasSize(2);
        assertThat(response.conversationHistory().get(0).role()).isEqualTo("user");
        assertThat(response.conversationHistory().get(1).role()).isEqualTo("assistant");
    }

    @Test
    void getChatResponse_appointmentQuestion_returnsBookingInstructions() {
        // Arrange
        ChatRequest request = new ChatRequest("I want to book an appointment", null);

        // Act
        ChatResponse response = chatService.getChatResponse(request);

        // Assert
        assertThat(response.response()).contains("book");
        assertThat(response.response()).contains("appointment");
    }

    @Test
    void getChatResponse_cancelQuestion_returnsCancelInstructions() {
        // Arrange
        ChatRequest request = new ChatRequest("How can I cancel my appointment?", null);

        // Act
        ChatResponse response = chatService.getChatResponse(request);

        // Assert
        assertThat(response.response()).contains("cancel");
    }

    @Test
    void getChatResponse_profileQuestion_returnsProfileInstructions() {
        // Arrange
        ChatRequest request = new ChatRequest("How do I edit my profile?", null);

        // Act
        ChatResponse response = chatService.getChatResponse(request);

        // Assert
        assertThat(response.response()).contains("profile");
        assertThat(response.response()).contains("Account");
    }

    @Test
    void getChatResponse_doctorQuestion_returnsDoctorFindingInstructions() {
        // Arrange
        ChatRequest request = new ChatRequest("How do I find a doctor?", null);

        // Act
        ChatResponse response = chatService.getChatResponse(request);

        // Assert
        assertThat(response.response()).contains("doctor");
    }

    @Test
    void getChatResponse_helpQuestion_returnsHelpMessage() {
        // Arrange
        ChatRequest request = new ChatRequest("Help me", null);

        // Act
        ChatResponse response = chatService.getChatResponse(request);

        // Assert
        assertThat(response.response()).contains("help");
        assertThat(response.response()).contains("appointments");
    }

    @Test
    void getChatResponse_greeting_returnsGreetingResponse() {
        // Arrange
        ChatRequest request = new ChatRequest("Hello", null);

        // Act
        ChatResponse response = chatService.getChatResponse(request);

        // Assert
        assertThat(response.response()).contains("Hello");
        assertThat(response.response()).contains("SuperDoc");
    }

    @Test
    void getChatResponse_withConversationHistory_maintainsHistory() {
        // Arrange
        List<ChatMessage> history = List.of(
            new ChatMessage("user", "Hello"),
            new ChatMessage("assistant", "Hi! How can I help?")
        );
        ChatRequest request = new ChatRequest("How do I book?", history);

        // Act
        ChatResponse response = chatService.getChatResponse(request);

        // Assert
        assertThat(response.conversationHistory()).hasSize(4); // 2 existing + 2 new
        assertThat(response.conversationHistory().get(0).role()).isEqualTo("user");
        assertThat(response.conversationHistory().get(1).role()).isEqualTo("assistant");
        assertThat(response.conversationHistory().get(2).role()).isEqualTo("user");
        assertThat(response.conversationHistory().get(3).role()).isEqualTo("assistant");
    }

    @Test
    void getChatResponse_unknownQuestion_returnsDefaultHelpMessage() {
        // Arrange
        ChatRequest request = new ChatRequest("What is the weather?", null);

        // Act
        ChatResponse response = chatService.getChatResponse(request);

        // Assert
        assertThat(response.response()).contains("SuperDoc");
        assertThat(response.response()).contains("help");
    }

    @Test
    void getChatResponse_emptyMessage_handlesGracefully() {
        // Arrange - ChatService should handle validation at controller level,
        // but we test that fallback handles edge cases
        ChatRequest request = new ChatRequest("", null);

        // Act
        ChatResponse response = chatService.getChatResponse(request);

        // Assert - Should return default response
        assertThat(response.response()).isNotBlank();
        assertThat(response.conversationHistory()).hasSize(2);
    }

    @Test
    void getChatResponse_caseInsensitive_matchesKeywords() {
        // Arrange
        ChatRequest request1 = new ChatRequest("BOOK APPOINTMENT", null);
        ChatRequest request2 = new ChatRequest("book appointment", null);
        ChatRequest request3 = new ChatRequest("Book Appointment", null);

        // Act
        ChatResponse response1 = chatService.getChatResponse(request1);
        ChatResponse response2 = chatService.getChatResponse(request2);
        ChatResponse response3 = chatService.getChatResponse(request3);

        // Assert - All should match booking keywords
        assertThat(response1.response()).contains("book");
        assertThat(response2.response()).contains("book");
        assertThat(response3.response()).contains("book");
    }
}


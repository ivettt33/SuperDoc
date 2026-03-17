package com.superdoc.api.service;

import com.superdoc.api.model.dto.ChatDtos.ChatMessage;
import com.superdoc.api.model.dto.ChatDtos.ChatRequest;
import com.superdoc.api.model.dto.ChatDtos.ChatResponse;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessageRole;
import com.theokanning.openai.service.OpenAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    @Value("${openai.api.key:}")
    private String apiKey;

    @Value("${openai.api.model:gpt-3.5-turbo}")
    private String model;

    @Value("${openai.api.max-tokens:500}")
    private Integer maxTokens;

    @jakarta.annotation.PostConstruct
    public void init() {
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("WARNING: OpenAI API key is not configured!");
        } else {
            System.out.println("OpenAI API key loaded (length: " + apiKey.length() + ")");
        }
    }

    private static final String SYSTEM_PROMPT = """
            You are a helpful assistant for SuperDoc, a medical appointment booking platform.
            Your role is to help users with frequently asked questions about:
            - Booking appointments
            - Managing appointments (canceling, rescheduling)
            - Profile management
            - General platform usage
            
            Important guidelines:
            1. Be friendly, professional, and concise
            2. Do NOT provide medical advice or diagnoses
            3. If asked about medical conditions, symptoms, or treatments, politely redirect to consulting with a doctor
            4. Focus on helping with platform features and appointment-related questions
            5. If you don't know something, admit it and suggest contacting support
            
            Keep responses brief and helpful (under 200 words when possible).
            """;

    public ChatResponse getChatResponse(ChatRequest request) {
        // If no API key or quota exceeded, use FAQ-based fallback
        if (apiKey == null || apiKey.isEmpty()) {
            return getFallbackResponse(request);
        }

        try {
            OpenAiService service = new OpenAiService(apiKey, Duration.ofSeconds(30));

            // Build conversation history
            List<com.theokanning.openai.completion.chat.ChatMessage> messages = new ArrayList<>();
            
            // Add system message
            messages.add(new com.theokanning.openai.completion.chat.ChatMessage(
                    ChatMessageRole.SYSTEM.value(),
                    SYSTEM_PROMPT
            ));

            // Add conversation history if provided
            if (request.conversationHistory() != null && !request.conversationHistory().isEmpty()) {
                for (ChatMessage msg : request.conversationHistory()) {
                    String role = msg.role().equals("user") ? ChatMessageRole.USER.value() : ChatMessageRole.ASSISTANT.value();
                    messages.add(new com.theokanning.openai.completion.chat.ChatMessage(role, msg.content()));
                }
            }

            // Add current user message
            messages.add(new com.theokanning.openai.completion.chat.ChatMessage(
                    ChatMessageRole.USER.value(),
                    request.message()
            ));

            // Create completion request
            ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                    .model(model)
                    .messages(messages)
                    .maxTokens(maxTokens)
                    .temperature(0.7)
                    .build();

            // Get response
            String response = service.createChatCompletion(completionRequest)
                    .getChoices()
                    .get(0)
                    .getMessage()
                    .getContent();

            // Update conversation history
            List<ChatMessage> updatedHistory = new ArrayList<>(
                    request.conversationHistory() != null ? request.conversationHistory() : List.of()
            );
            updatedHistory.add(new ChatMessage("user", request.message()));
            updatedHistory.add(new ChatMessage("assistant", response));

            return new ChatResponse(response, updatedHistory);

        } catch (Exception e) {
            // Log the full exception for debugging
            System.err.println("ChatService error: " + e.getClass().getName());
            System.err.println("ChatService error message: " + e.getMessage());
            e.printStackTrace();
            
            // If quota exceeded or API issues, use fallback FAQ system
            if (e.getMessage() != null && (e.getMessage().contains("quota") || e.getMessage().contains("billing"))) {
                System.out.println("OpenAI API quota exceeded, using fallback FAQ system");
                return getFallbackResponse(request);
            }
            
            // For other errors, return error message
            String errorMessage = "Sorry, I encountered an error. ";
            if (e.getMessage() != null && e.getMessage().contains("API key")) {
                errorMessage += "API configuration issue. Using FAQ fallback.";
                return getFallbackResponse(request);
            } else if (e.getMessage() != null) {
                errorMessage += e.getMessage();
            } else {
                errorMessage += "Please try again later or contact support.";
            }
            
            return new ChatResponse(
                    errorMessage,
                    request.conversationHistory() != null ? request.conversationHistory() : List.of()
            );
        }
    }

    /**
     * Fallback FAQ-based response system when OpenAI API is not available
     */
    private ChatResponse getFallbackResponse(ChatRequest request) {
        String userMessage = request.message().toLowerCase();
        String response;

        // Simple FAQ matching
        if (userMessage.contains("book") || userMessage.contains("appointment") || userMessage.contains("schedule")) {
            response = "To book an appointment:\n1. Go to the Appointments page\n2. Click 'Book Appointment'\n3. Select a doctor and available time slot\n4. Add any notes and confirm your booking\n\nYou can also ask me about canceling or rescheduling appointments!";
        } else if (userMessage.contains("cancel") || userMessage.contains("delete")) {
            response = "To cancel an appointment:\n1. Go to the Appointments page\n2. Find your appointment\n3. Click the 'Cancel' button\n\nNote: You cannot cancel appointments that have already passed. Past appointments will show as 'PASSED' status.";
        } else if (userMessage.contains("reschedule") || userMessage.contains("change") || userMessage.contains("modify")) {
            response = "To reschedule an appointment:\n1. Go to the Appointments page\n2. Find your appointment\n3. Click to view details\n4. You can update the date/time if the appointment hasn't passed yet";
        } else if (userMessage.contains("profile") || userMessage.contains("account") || userMessage.contains("edit")) {
            response = "To manage your profile:\n1. Click on 'Account' in the sidebar\n2. View your profile information\n3. Click 'Edit' to update your details\n4. You can upload a profile picture and update your information";
        } else if (userMessage.contains("doctor") || userMessage.contains("find")) {
            response = "To find doctors:\n1. Go to the Appointments page\n2. Click 'Book Appointment'\n3. You'll see a list of available doctors\n4. You can filter by specialization\n5. Check their availability and book a time slot";
        } else if (userMessage.contains("help") || userMessage.contains("how")) {
            response = "I can help you with:\n• Booking appointments\n• Canceling appointments\n• Managing your profile\n• Finding doctors\n• General platform questions\n\nJust ask me anything about using SuperDoc!";
        } else if (userMessage.contains("hello") || userMessage.contains("hi") || userMessage.contains("hey")) {
            response = "Hello! I'm the SuperDoc Assistant. I can help you with booking appointments, managing your profile, and answering questions about the platform. What would you like to know?";
        } else {
            response = "I'm here to help with SuperDoc! I can assist with:\n• Booking and managing appointments\n• Profile management\n• Finding doctors\n• Platform usage questions\n\nTry asking: 'How do I book an appointment?' or 'How do I cancel an appointment?'";
        }

        // Update conversation history
        List<ChatMessage> updatedHistory = new ArrayList<>(
                request.conversationHistory() != null ? request.conversationHistory() : List.of()
        );
        updatedHistory.add(new ChatMessage("user", request.message()));
        updatedHistory.add(new ChatMessage("assistant", response));

        return new ChatResponse(response, updatedHistory);
    }
}


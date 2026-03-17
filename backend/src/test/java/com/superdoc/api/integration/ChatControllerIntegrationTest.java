package com.superdoc.api.integration;

import com.superdoc.api.enumerate.Role;
import com.superdoc.api.model.dto.ChatDtos.*;
import com.superdoc.api.model.dto.AuthDtos.LoginRequest;
import com.superdoc.api.model.dto.AuthDtos.RegisterRequest;
import com.superdoc.api.persistence.entities.UserEntity;
import com.superdoc.api.persistence.repo.UserRepository;
import com.superdoc.api.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class ChatControllerIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15-alpine")
                    .withDatabaseName("superdoc_test")
                    .withUsername("superdoc")
                    .withPassword("superdoc_pw");

    @DynamicPropertySource
    static void overrideDatasourceProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
    }

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private DbVerifier dbVerifier;

    private UserEntity user;
    private String userToken;

    @BeforeEach
    void setUp() {
        // Clean up
        userRepository.deleteAll();

        // Create user
        user = new UserEntity();
        user.setEmail("user@test.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setRole(Role.PATIENT);
        user = userRepository.save(user);

        // Generate token
        userToken = jwtService.generate(
                java.util.Map.of("role", Role.PATIENT.name(), "uid", user.getId()),
                user.getEmail()
        );
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    void chat_withValidMessage_shouldReturnResponse() {
        // Arrange - verify user exists in database
        UserEntity userInDb = dbVerifier.findEntity(UserEntity.class, user.getId());
        assertThat(userInDb).isNotNull();
        assertThat(userInDb.getEmail()).isEqualTo("user@test.com");
        
        ChatRequest request = new ChatRequest("How do I book an appointment?", null);
        HttpEntity<ChatRequest> entity = new HttpEntity<>(request, authHeaders(userToken));

        // Act
        ResponseEntity<ChatResponse> response = rest.exchange(
                "/chat",
                HttpMethod.POST,
                entity,
                ChatResponse.class
        );

        // Assert HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ChatResponse chatResponse = response.getBody();
        assertThat(chatResponse).isNotNull();
        assertThat(chatResponse.response()).isNotBlank();
        assertThat(chatResponse.conversationHistory()).isNotNull();
        assertThat(chatResponse.conversationHistory().size()).isGreaterThanOrEqualTo(2);
        
        // Verify conversation history contains user message and assistant response
        assertThat(chatResponse.conversationHistory().get(0).role()).isEqualTo("user");
        assertThat(chatResponse.conversationHistory().get(1).role()).isEqualTo("assistant");
        
        // Verify user entity remains unchanged (chat doesn't persist data)
        UserEntity userAfterChat = dbVerifier.findEntity(UserEntity.class, user.getId());
        assertThat(userAfterChat).isNotNull();
        assertThat(userAfterChat.getEmail()).isEqualTo("user@test.com");
        assertThat(userAfterChat.getRole()).isEqualTo(Role.PATIENT);
    }

    @Test
    void chat_withConversationHistory_shouldMaintainHistory() {
        // Arrange
        List<ChatMessage> history = List.of(
                new ChatMessage("user", "Hello"),
                new ChatMessage("assistant", "Hi! How can I help?")
        );
        ChatRequest request = new ChatRequest("How do I book?", history);
        HttpEntity<ChatRequest> entity = new HttpEntity<>(request, authHeaders(userToken));

        // Act
        ResponseEntity<ChatResponse> response = rest.exchange(
                "/chat",
                HttpMethod.POST,
                entity,
                ChatResponse.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ChatResponse chatResponse = response.getBody();
        assertThat(chatResponse).isNotNull();
        assertThat(chatResponse.conversationHistory().size()).isGreaterThanOrEqualTo(4); // 2 existing + 2 new
    }

    @Test
    void chat_emptyMessage_shouldFail() {
        // Arrange - verify user exists
        UserEntity userBefore = dbVerifier.findEntity(UserEntity.class, user.getId());
        assertThat(userBefore).isNotNull();
        
        ChatRequest request = new ChatRequest("", null);
        HttpEntity<ChatRequest> entity = new HttpEntity<>(request, authHeaders(userToken));

        // Act
        ResponseEntity<String> response = rest.exchange(
                "/chat",
                HttpMethod.POST,
                entity,
                String.class
        );

        // Assert HTTP error
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        
        // Verify user entity remains unchanged (no side effects)
        UserEntity userAfter = dbVerifier.findEntity(UserEntity.class, user.getId());
        assertThat(userAfter).isNotNull();
        assertThat(userAfter.getEmail()).isEqualTo(userBefore.getEmail());
        assertThat(userAfter.getRole()).isEqualTo(userBefore.getRole());
    }

    @Test
    void chat_withoutAuthentication_shouldFail() {
        // Arrange - verify user exists
        UserEntity userBefore = dbVerifier.findEntity(UserEntity.class, user.getId());
        assertThat(userBefore).isNotNull();
        
        ChatRequest request = new ChatRequest("How do I book an appointment?", null);
        HttpEntity<ChatRequest> entity = new HttpEntity<>(request); // No auth headers

        // Act
        ResponseEntity<String> response = rest.exchange(
                "/chat",
                HttpMethod.POST,
                entity,
                String.class
        );

        // Assert HTTP error
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        
        // Verify user entity remains unchanged (no side effects from failed request)
        UserEntity userAfter = dbVerifier.findEntity(UserEntity.class, user.getId());
        assertThat(userAfter).isNotNull();
        assertThat(userAfter.getEmail()).isEqualTo(userBefore.getEmail());
        assertThat(userAfter.getRole()).isEqualTo(userBefore.getRole());
    }

    @Test
    void chat_appointmentQuestion_shouldReturnBookingInstructions() {
        // Arrange
        ChatRequest request = new ChatRequest("I want to book an appointment", null);
        HttpEntity<ChatRequest> entity = new HttpEntity<>(request, authHeaders(userToken));

        // Act
        ResponseEntity<ChatResponse> response = rest.exchange(
                "/chat",
                HttpMethod.POST,
                entity,
                ChatResponse.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ChatResponse chatResponse = response.getBody();
        assertThat(chatResponse).isNotNull();
        String responseText = chatResponse.response().toLowerCase();
        assertThat(responseText).containsAnyOf("book", "appointment");
    }
}


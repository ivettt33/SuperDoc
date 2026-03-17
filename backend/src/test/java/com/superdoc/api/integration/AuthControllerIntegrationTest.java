package com.superdoc.api.integration;

import com.superdoc.api.enumerate.Role;
import com.superdoc.api.model.dto.AuthDtos.JwtResponse;
import com.superdoc.api.model.dto.AuthDtos.LoginRequest;
import com.superdoc.api.model.dto.AuthDtos.RegisterRequest;
import com.superdoc.api.persistence.entities.UserEntity;
import com.superdoc.api.persistence.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class AuthControllerIntegrationTest {

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
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DbVerifier dbVerifier;

    @BeforeEach
    void setUp() {
        users.deleteAll();
    }

    @Test
    void registerAndLoginFlowShouldSucceed() {
        // Arrange
        long initialCount = dbVerifier.countEntities(UserEntity.class);
        RegisterRequest registerRequest = new RegisterRequest(
                "integration.user@superdoc.test",
                "securePass8",
                Role.DOCTOR
        );

        // Act: call registration endpoint
        ResponseEntity<Void> registerResponse = rest.postForEntity(
                "/auth/register",
                registerRequest,
                Void.class
        );

        // Assert registration response
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        HttpHeaders headers = registerResponse.getHeaders();
        assertThat(headers.getLocation()).isNotNull();

        // Verify count increased
        long countAfterRegistration = dbVerifier.countEntities(UserEntity.class);
        assertThat(countAfterRegistration).isEqualTo(initialCount + 1);

        // Assert user persisted with encoded password and correct role using EntityManager
        // Find by email using EntityManager (not repository!)
        UserEntity savedUser = dbVerifier.findEntityByField(UserEntity.class, "email", registerRequest.email());
        assertThat(savedUser).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo(registerRequest.email());
        assertThat(savedUser.getRole()).isEqualTo(Role.DOCTOR);
        assertThat(passwordEncoder.matches(registerRequest.password(), savedUser.getPasswordHash())).isTrue();
        assertThat(savedUser.getCreatedAt()).isNotNull();

        // Act: login with same credentials
        LoginRequest loginRequest = new LoginRequest(registerRequest.email(), registerRequest.password());
        ResponseEntity<JwtResponse> loginResponse = rest.postForEntity(
                "/auth/login",
                loginRequest,
                JwtResponse.class
        );

        // Assert login response
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        JwtResponse jwtResponse = loginResponse.getBody();
        assertThat(jwtResponse).isNotNull();
        assertThat(jwtResponse.token()).isNotBlank();
        assertThat(jwtResponse.email()).isEqualTo(registerRequest.email());
        assertThat(jwtResponse.role()).isEqualTo(Role.DOCTOR);
        assertThat(jwtResponse.onboardingRequired()).isTrue();
    }

    @Test
    void register_duplicateEmail_shouldFail() {
        // Arrange - create existing user
        long initialCount = dbVerifier.countEntities(UserEntity.class);
        UserEntity existingUser = new UserEntity();
        existingUser.setEmail("existing@test.com");
        existingUser.setPasswordHash(passwordEncoder.encode("password123"));
        existingUser.setRole(Role.PATIENT);
        existingUser = users.save(existingUser);
        
        long countAfterSetup = dbVerifier.countEntities(UserEntity.class);
        assertThat(countAfterSetup).isEqualTo(initialCount + 1);

        RegisterRequest duplicateRequest = new RegisterRequest(
                "existing@test.com",
                "newPassword123",
                Role.DOCTOR
        );

        // Act
        ResponseEntity<String> response = rest.postForEntity(
                "/auth/register",
                duplicateRequest,
                String.class
        );

        // Assert HTTP error
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Assert NO new user was created (count unchanged)
        long finalCount = dbVerifier.countEntities(UserEntity.class);
        assertThat(finalCount).isEqualTo(initialCount + 1);
        
        // Verify original user still exists and unchanged
        UserEntity persisted = dbVerifier.findEntity(UserEntity.class, existingUser.getId());
        assertThat(persisted).isNotNull();
        assertThat(persisted.getEmail()).isEqualTo("existing@test.com");
        assertThat(persisted.getRole()).isEqualTo(Role.PATIENT);
    }

    @Test
    void register_invalidEmail_shouldFail() {
        // Arrange
        long initialCount = dbVerifier.countEntities(UserEntity.class);
        RegisterRequest invalidRequest = new RegisterRequest(
                "invalid-email",
                "password123",
                Role.DOCTOR
        );

        // Act
        ResponseEntity<String> response = rest.postForEntity(
                "/auth/register",
                invalidRequest,
                String.class
        );

        // Assert HTTP error
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Assert NO user was created
        long finalCount = dbVerifier.countEntities(UserEntity.class);
        assertThat(finalCount).isEqualTo(initialCount);
    }

    @Test
    void login_invalidCredentials_shouldFail() {
        // Arrange - create a user
        UserEntity user = new UserEntity();
        user.setEmail("test@test.com");
        user.setPasswordHash(passwordEncoder.encode("correctPassword"));
        user.setRole(Role.PATIENT);
        user = users.save(user);

        LoginRequest invalidLogin = new LoginRequest(
                "test@test.com",
                "wrongPassword"
        );

        // Act
        ResponseEntity<String> response = rest.postForEntity(
                "/auth/login",
                invalidLogin,
                String.class
        );

        // Assert HTTP error
        // Note: AuthService throws IllegalArgumentException for bad credentials, which results in 400 BAD_REQUEST
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        // Verify user still exists (login failure shouldn't affect user)
        UserEntity persisted = dbVerifier.findEntity(UserEntity.class, user.getId());
        assertThat(persisted).isNotNull();
        assertThat(persisted.getEmail()).isEqualTo("test@test.com");
    }
}


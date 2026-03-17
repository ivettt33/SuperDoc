package com.superdoc.api.integration;

import com.superdoc.api.enumerate.Role;
import com.superdoc.api.model.dto.AuthDtos.LoginRequest;
import com.superdoc.api.model.dto.AuthDtos.RegisterRequest;
import com.superdoc.api.model.dto.OnboardingDtos.PatientOnboardRequest;
import com.superdoc.api.persistence.entities.PatientProfileEntity;
import com.superdoc.api.model.dto.ProfileDtos.PatientProfileResponse;
import com.superdoc.api.persistence.entities.UserEntity;
import com.superdoc.api.persistence.repo.PatientProfileRepository;
import com.superdoc.api.persistence.repo.UserRepository;
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

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class PatientProfileControllerIntegrationTest {

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
    private PatientProfileRepository patientProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DbVerifier dbVerifier;

    private String patientEmail;
    private String patientToken;

    @BeforeEach
    void setUp() {
        users.deleteAll();
        patientProfileRepository.deleteAll();

        // Create and register patient user
        patientEmail = "patient.profile@superdoc.test";
        RegisterRequest register = new RegisterRequest(patientEmail, "securePass8", Role.PATIENT);
        rest.postForEntity("/auth/register", register, Void.class);

        // Login and get token
        LoginRequest login = new LoginRequest(patientEmail, "securePass8");
        var loginResponse = rest.postForEntity("/auth/login", login, com.superdoc.api.model.dto.AuthDtos.JwtResponse.class);
        patientToken = loginResponse.getBody().token();
    }

    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    void createProfileShouldSucceed() {
        // Arrange
        long initialCount = dbVerifier.countEntities(PatientProfileEntity.class);
        PatientOnboardRequest request = new PatientOnboardRequest(
                "Jane",
                "Doe",
                LocalDate.of(1990, 5, 15),
                "Female",
                "Hypertension, Diabetes",
                "INS123456",
                "https://example.com/patient.jpg"
        );

        // Act
        HttpEntity<PatientOnboardRequest> entity = new HttpEntity<>(request, createAuthHeaders(patientToken));
        ResponseEntity<PatientProfileResponse> response = rest.exchange(
                "/patients/profile",
                HttpMethod.POST,
                entity,
                PatientProfileResponse.class
        );

        // Assert HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PatientProfileResponse profile = response.getBody();
        assertThat(profile).isNotNull();
        assertThat(profile.firstName()).isEqualTo("Jane");
        assertThat(profile.lastName()).isEqualTo("Doe");
        assertThat(profile.dateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 15));
        assertThat(profile.gender()).isEqualTo("Female");
        assertThat(profile.conditions()).isEqualTo("Hypertension, Diabetes");
        assertThat(profile.insuranceNumber()).isEqualTo("INS123456");

        // Verify count increased
        long finalCount = dbVerifier.countEntities(PatientProfileEntity.class);
        assertThat(finalCount).isEqualTo(initialCount + 1);

        // Verify persisted in database using EntityManager
        PatientProfileEntity persisted = dbVerifier.findEntity(PatientProfileEntity.class, profile.id());
        assertThat(persisted).isNotNull();
        assertThat(persisted.getFirstName()).isEqualTo("Jane");
        assertThat(persisted.getLastName()).isEqualTo("Doe");
        assertThat(persisted.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 15));
        assertThat(persisted.getGender()).isEqualTo("Female");
        assertThat(persisted.getConditions()).isEqualTo("Hypertension, Diabetes");
        assertThat(persisted.getInsuranceNumber()).isEqualTo("INS123456");
        assertThat(persisted.getProfilePicture()).isEqualTo("https://example.com/patient.jpg");
        
        // Verify relationship to user using EntityManager (not repository!)
        UserEntity userInDb = dbVerifier.findEntityByField(UserEntity.class, "email", patientEmail);
        assertThat(userInDb.getPatientProfile()).isNotNull();
        assertThat(userInDb.getPatientProfile().getId()).isEqualTo(profile.id());
    }

    @Test
    void updateProfileShouldSucceed() {
        // Arrange - create initial profile
        PatientOnboardRequest initialRequest = new PatientOnboardRequest(
                "Jane",
                "Doe",
                LocalDate.of(1990, 5, 15),
                "Female",
                "Hypertension",
                "INS123456",
                null
        );
        HttpEntity<PatientOnboardRequest> initialEntity = new HttpEntity<>(initialRequest, createAuthHeaders(patientToken));
        ResponseEntity<PatientProfileResponse> initialResponse = rest.exchange(
                "/patients/profile",
                HttpMethod.POST,
                initialEntity,
                PatientProfileResponse.class
        );
        
        // Get original profile to check updatedAt timestamp
        PatientProfileEntity originalProfile = dbVerifier.findEntity(
                PatientProfileEntity.class,
                initialResponse.getBody().id()
        );
        java.time.Instant originalUpdatedAt = originalProfile.getUpdatedAt();

        // Update profile
        PatientOnboardRequest updateRequest = new PatientOnboardRequest(
                "Jane",
                "Smith", // Changed last name
                LocalDate.of(1990, 5, 15),
                "Female",
                "Hypertension, Diabetes", // Updated conditions
                "INS789012", // Changed insurance
                "https://example.com/new-photo.jpg"
        );

        // Act
        HttpEntity<PatientOnboardRequest> updateEntity = new HttpEntity<>(updateRequest, createAuthHeaders(patientToken));
        ResponseEntity<PatientProfileResponse> response = rest.exchange(
                "/patients/profile",
                HttpMethod.POST,
                updateEntity,
                PatientProfileResponse.class
        );

        // Assert HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PatientProfileResponse updatedProfile = response.getBody();
        assertThat(updatedProfile).isNotNull();
        assertThat(updatedProfile.lastName()).isEqualTo("Smith");
        assertThat(updatedProfile.conditions()).isEqualTo("Hypertension, Diabetes");
        assertThat(updatedProfile.insuranceNumber()).isEqualTo("INS789012");

        // Verify in database using EntityManager
        PatientProfileEntity persisted = dbVerifier.findEntity(PatientProfileEntity.class, updatedProfile.id());
        assertThat(persisted).isNotNull();
        assertThat(persisted.getLastName()).isEqualTo("Smith");
        assertThat(persisted.getConditions()).isEqualTo("Hypertension, Diabetes");
        assertThat(persisted.getInsuranceNumber()).isEqualTo("INS789012");
        assertThat(persisted.getProfilePicture()).isEqualTo("https://example.com/new-photo.jpg");
        
        // Verify updatedAt timestamp changed
        assertThat(persisted.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    void getMyProfileShouldReturnPatientProfile() {
        // Arrange - create profile first
        PatientOnboardRequest request = new PatientOnboardRequest(
                "John",
                "Patient",
                LocalDate.of(1985, 3, 20),
                "Male",
                "Asthma",
                "INS555666",
                null
        );
        HttpEntity<PatientOnboardRequest> createEntity = new HttpEntity<>(request, createAuthHeaders(patientToken));
        ResponseEntity<PatientProfileResponse> createResponse = rest.exchange(
                "/patients/profile",
                HttpMethod.POST,
                createEntity,
                PatientProfileResponse.class
        );
        
        Long profileId = createResponse.getBody().id();

        // Act
        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(patientToken));
        ResponseEntity<PatientProfileResponse> response = rest.exchange(
                "/patients/profile/me",
                HttpMethod.GET,
                entity,
                PatientProfileResponse.class
        );

        // Assert HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PatientProfileResponse profile = response.getBody();
        assertThat(profile).isNotNull();
        assertThat(profile.firstName()).isEqualTo("John");
        assertThat(profile.lastName()).isEqualTo("Patient");
        assertThat(profile.dateOfBirth()).isEqualTo(LocalDate.of(1985, 3, 20));
        
        // Verify profile exists in database using EntityManager
        PatientProfileEntity persisted = dbVerifier.findEntity(PatientProfileEntity.class, profileId);
        assertThat(persisted).isNotNull();
        assertThat(persisted.getFirstName()).isEqualTo("John");
        assertThat(persisted.getLastName()).isEqualTo("Patient");
        assertThat(persisted.getDateOfBirth()).isEqualTo(LocalDate.of(1985, 3, 20));
    }

    @Test
    void getMyProfileWithoutAuthenticationShouldFail() {
        // Act
        ResponseEntity<Void> response = rest.getForEntity("/patients/profile/me", Void.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void createProfileWithoutAuthenticationShouldFail() {
        // Arrange
        long initialCount = dbVerifier.countEntities(PatientProfileEntity.class);
        PatientOnboardRequest request = new PatientOnboardRequest(
                "Jane",
                "Doe",
                LocalDate.of(1990, 5, 15),
                "Female",
                null,
                null,
                null
        );

        // Act
        HttpEntity<PatientOnboardRequest> entity = new HttpEntity<>(request);
        ResponseEntity<Void> response = rest.exchange(
                "/patients/profile",
                HttpMethod.POST,
                entity,
                Void.class
        );

        // Assert HTTP error
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        
        // Assert NO profile was created
        long finalCount = dbVerifier.countEntities(PatientProfileEntity.class);
        assertThat(finalCount).isEqualTo(initialCount);
    }
}


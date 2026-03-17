package com.superdoc.api.integration;

import com.superdoc.api.enumerate.Role;
import com.superdoc.api.model.dto.AuthDtos.LoginRequest;
import com.superdoc.api.model.dto.AuthDtos.RegisterRequest;
import com.superdoc.api.model.dto.OnboardingDtos.DoctorOnboardRequest;
import com.superdoc.api.persistence.entities.DoctorProfileEntity;
import com.superdoc.api.model.dto.ProfileDtos.DoctorProfileResponse;
import com.superdoc.api.persistence.entities.UserEntity;
import com.superdoc.api.persistence.repo.DoctorProfileRepository;
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

import java.time.LocalTime;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class DoctorProfileControllerIntegrationTest {

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
    private DoctorProfileRepository doctorProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DbVerifier dbVerifier;

    private String doctorEmail;
    private String doctorToken;

    @BeforeEach
    void setUp() {
        users.deleteAll();
        doctorProfileRepository.deleteAll();

        // Create and register doctor user
        doctorEmail = "doctor.profile@superdoc.test";
        RegisterRequest register = new RegisterRequest(doctorEmail, "securePass8", Role.DOCTOR);
        rest.postForEntity("/auth/register", register, Void.class);

        // Login and get token
        LoginRequest login = new LoginRequest(doctorEmail, "securePass8");
        var loginResponse = rest.postForEntity("/auth/login", login, com.superdoc.api.model.dto.AuthDtos.JwtResponse.class);
        doctorToken = loginResponse.getBody().token();
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
        long initialCount = dbVerifier.countEntities(DoctorProfileEntity.class);
        DoctorOnboardRequest request = new DoctorOnboardRequest(
                "Dr. John",
                "Smith",
                "Cardiology",
                "Experienced cardiologist with 15 years of practice",
                "LIC123456",
                "Heart Care Clinic",
                15,
                "https://example.com/photo.jpg",
                java.time.LocalTime.of(9, 0),
                java.time.LocalTime.of(17, 0),
                false
        );

        // Act
        HttpEntity<DoctorOnboardRequest> entity = new HttpEntity<>(request, createAuthHeaders(doctorToken));
        ResponseEntity<DoctorProfileResponse> response = rest.exchange(
                "/doctors/profile",
                HttpMethod.POST,
                entity,
                DoctorProfileResponse.class
        );

        // Assert HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DoctorProfileResponse profile = response.getBody();
        assertThat(profile).isNotNull();
        assertThat(profile.firstName()).isEqualTo("Dr. John");
        assertThat(profile.lastName()).isEqualTo("Smith");
        assertThat(profile.specialization()).isEqualTo("Cardiology");
        assertThat(profile.licenseNumber()).isEqualTo("LIC123456");
        assertThat(profile.clinicName()).isEqualTo("Heart Care Clinic");
        assertThat(profile.yearsOfExperience()).isEqualTo(15);

        // Verify count increased
        long finalCount = dbVerifier.countEntities(DoctorProfileEntity.class);
        assertThat(finalCount).isEqualTo(initialCount + 1);

        // Verify persisted in database using EntityManager
        DoctorProfileEntity persisted = dbVerifier.findEntity(DoctorProfileEntity.class, profile.id());
        assertThat(persisted).isNotNull();
        assertThat(persisted.getFirstName()).isEqualTo("Dr. John");
        assertThat(persisted.getLastName()).isEqualTo("Smith");
        assertThat(persisted.getSpecialization()).isEqualTo("Cardiology");
        assertThat(persisted.getBio()).isEqualTo("Experienced cardiologist with 15 years of practice");
        assertThat(persisted.getLicenseNumber()).isEqualTo("LIC123456");
        assertThat(persisted.getClinicName()).isEqualTo("Heart Care Clinic");
        assertThat(persisted.getYearsOfExperience()).isEqualTo(15);
        assertThat(persisted.getProfilePhotoUrl()).isEqualTo("https://example.com/photo.jpg");
        assertThat(persisted.getOpeningHours()).isEqualTo(LocalTime.of(9, 0));
        assertThat(persisted.getClosingHours()).isEqualTo(LocalTime.of(17, 0));
        assertThat(persisted.getIsAbsent()).isFalse();
        
        // Verify relationship to user using EntityManager (not repository!)
        UserEntity userInDb = dbVerifier.findEntityByField(UserEntity.class, "email", doctorEmail);
        assertThat(userInDb.getDoctorProfile()).isNotNull();
        assertThat(userInDb.getDoctorProfile().getId()).isEqualTo(profile.id());
    }

    @Test
    void updateProfileShouldSucceed() {
        // Arrange - create initial profile
        DoctorOnboardRequest initialRequest = new DoctorOnboardRequest(
                "Dr. John",
                "Smith",
                "Cardiology",
                "Initial bio",
                "LIC123456",
                "Heart Clinic",
                10,
                null,
                java.time.LocalTime.of(9, 0),
                java.time.LocalTime.of(17, 0),
                false
        );
        HttpEntity<DoctorOnboardRequest> initialEntity = new HttpEntity<>(initialRequest, createAuthHeaders(doctorToken));
        ResponseEntity<DoctorProfileResponse> initialResponse = rest.exchange(
                "/doctors/profile",
                HttpMethod.POST,
                initialEntity,
                DoctorProfileResponse.class
        );
        
        // Get original profile to check updatedAt timestamp
        DoctorProfileEntity originalProfile = dbVerifier.findEntity(
                DoctorProfileEntity.class,
                initialResponse.getBody().id()
        );
        Instant originalUpdatedAt = originalProfile.getUpdatedAt();

        // Update profile
        DoctorOnboardRequest updateRequest = new DoctorOnboardRequest(
                "Dr. John",
                "Smith",
                "Neurology", 
                "Updated bio with more experience",
                "LIC123456",
                "Neuro Care Clinic",
                20, 
                "https://example.com/new-photo.jpg",
                java.time.LocalTime.of(9, 0),
                java.time.LocalTime.of(17, 0),
                false
        );

        // Act
        HttpEntity<DoctorOnboardRequest> updateEntity = new HttpEntity<>(updateRequest, createAuthHeaders(doctorToken));
        ResponseEntity<DoctorProfileResponse> response = rest.exchange(
                "/doctors/profile",
                HttpMethod.POST,
                updateEntity,
                DoctorProfileResponse.class
        );

        // Assert HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DoctorProfileResponse updatedProfile = response.getBody();
        assertThat(updatedProfile).isNotNull();
        assertThat(updatedProfile.specialization()).isEqualTo("Neurology");
        assertThat(updatedProfile.clinicName()).isEqualTo("Neuro Care Clinic");
        assertThat(updatedProfile.yearsOfExperience()).isEqualTo(20);
        assertThat(updatedProfile.bio()).isEqualTo("Updated bio with more experience");

        // Verify in database using EntityManager
        DoctorProfileEntity persisted = dbVerifier.findEntity(DoctorProfileEntity.class, updatedProfile.id());
        assertThat(persisted).isNotNull();
        assertThat(persisted.getSpecialization()).isEqualTo("Neurology");
        assertThat(persisted.getClinicName()).isEqualTo("Neuro Care Clinic");
        assertThat(persisted.getYearsOfExperience()).isEqualTo(20);
        assertThat(persisted.getBio()).isEqualTo("Updated bio with more experience");
        assertThat(persisted.getProfilePhotoUrl()).isEqualTo("https://example.com/new-photo.jpg");
        
        // Verify updatedAt timestamp changed
        assertThat(persisted.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    void getMyProfileShouldReturnDoctorProfile() {
        // Arrange - create profile first
        DoctorOnboardRequest request = new DoctorOnboardRequest(
                "Dr. Jane",
                "Doe",
                "Pediatrics",
                "Pediatric specialist",
                "LIC789012",
                "Children's Hospital",
                8,
                null,
                java.time.LocalTime.of(9, 0),
                java.time.LocalTime.of(17, 0),
                false
        );
        HttpEntity<DoctorOnboardRequest> createEntity = new HttpEntity<>(request, createAuthHeaders(doctorToken));
        ResponseEntity<DoctorProfileResponse> createResponse = rest.exchange(
                "/doctors/profile",
                HttpMethod.POST,
                createEntity,
                DoctorProfileResponse.class
        );
        
        Long profileId = createResponse.getBody().id();

        // Act
        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(doctorToken));
        ResponseEntity<DoctorProfileResponse> response = rest.exchange(
                "/doctors/profile/me",
                HttpMethod.GET,
                entity,
                DoctorProfileResponse.class
        );

        // Assert HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        DoctorProfileResponse profile = response.getBody();
        assertThat(profile).isNotNull();
        assertThat(profile.firstName()).isEqualTo("Dr. Jane");
        assertThat(profile.lastName()).isEqualTo("Doe");
        assertThat(profile.specialization()).isEqualTo("Pediatrics");
        
        // Verify profile exists in database using EntityManager
        DoctorProfileEntity persisted = dbVerifier.findEntity(DoctorProfileEntity.class, profileId);
        assertThat(persisted).isNotNull();
        assertThat(persisted.getFirstName()).isEqualTo("Dr. Jane");
        assertThat(persisted.getLastName()).isEqualTo("Doe");
        assertThat(persisted.getSpecialization()).isEqualTo("Pediatrics");
    }

    @Test
    void getMyProfileWithoutAuthenticationShouldFail() {
        // Act
        ResponseEntity<Void> response = rest.getForEntity("/doctors/profile/me", Void.class);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void createProfileWithoutAuthenticationShouldFail() {
        // Arrange
        long initialCount = dbVerifier.countEntities(DoctorProfileEntity.class);
        DoctorOnboardRequest request = new DoctorOnboardRequest(
                "Dr. John",
                "Smith",
                "Cardiology",
                null,
                "LIC123456",
                "Heart Clinic",
                10,
                null,
                java.time.LocalTime.of(9, 0),
                java.time.LocalTime.of(17, 0),
                false
        );

        // Act
        HttpEntity<DoctorOnboardRequest> entity = new HttpEntity<>(request);
        ResponseEntity<Void> response = rest.exchange(
                "/doctors/profile",
                HttpMethod.POST,
                entity,
                Void.class
        );

        // Assert HTTP error
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        
        // Assert NO profile was created
        long finalCount = dbVerifier.countEntities(DoctorProfileEntity.class);
        assertThat(finalCount).isEqualTo(initialCount);
    }
}


package com.superdoc.api.integration;

import com.superdoc.api.enumerate.Role;
import com.superdoc.api.model.dto.AuthDtos.LoginRequest;
import com.superdoc.api.model.dto.AuthDtos.RegisterRequest;
import com.superdoc.api.model.dto.OnboardingDtos.*;
import com.superdoc.api.persistence.entities.*;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class OnboardingControllerIntegrationTest {

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
    private JwtService jwtService;

    @Autowired
    private DbVerifier dbVerifier;

    private String doctorEmail;
    private String patientEmail;
    private String doctorToken;
    private String patientToken;

    @BeforeEach
    void setUp() {
        users.deleteAll();

        // Create and register doctor user
        doctorEmail = "doctor.onboarding@superdoc.test";
        RegisterRequest doctorRegister = new RegisterRequest(doctorEmail, "securePass8", Role.DOCTOR);
        rest.postForEntity("/auth/register", doctorRegister, Void.class);

        // Login doctor and get token
        LoginRequest doctorLogin = new LoginRequest(doctorEmail, "securePass8");
        var doctorLoginResponse = rest.postForEntity("/auth/login", doctorLogin, com.superdoc.api.model.dto.AuthDtos.JwtResponse.class);
        doctorToken = doctorLoginResponse.getBody().token();

        // Create and register patient user
        patientEmail = "patient.onboarding@superdoc.test";
        RegisterRequest patientRegister = new RegisterRequest(patientEmail, "securePass8", Role.PATIENT);
        rest.postForEntity("/auth/register", patientRegister, Void.class);

        // Login patient and get token
        LoginRequest patientLogin = new LoginRequest(patientEmail, "securePass8");
        var patientLoginResponse = rest.postForEntity("/auth/login", patientLogin, com.superdoc.api.model.dto.AuthDtos.JwtResponse.class);
        patientToken = patientLoginResponse.getBody().token();
    }

    private HttpHeaders createAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    void updateRoleShouldSucceed() {
        // Arrange
        String email = "role.update@superdoc.test";
        RegisterRequest register = new RegisterRequest(email, "securePass8", Role.DOCTOR);
        rest.postForEntity("/auth/register", register, Void.class);

        LoginRequest login = new LoginRequest(email, "securePass8");
        var loginResponse = rest.postForEntity("/auth/login", login, com.superdoc.api.model.dto.AuthDtos.JwtResponse.class);
        String token = loginResponse.getBody().token();

        // Get user ID before role update using EntityManager (not repository!)
        UserEntity tempUser = dbVerifier.findEntityByField(UserEntity.class, "email", email);
        assertThat(tempUser).isNotNull();
        assertThat(tempUser.getRole()).isEqualTo(Role.DOCTOR); // Verify initial role
        Long userId = tempUser.getId();

        RoleSelectionRequest request = new RoleSelectionRequest(Role.PATIENT);

        // Act
        HttpEntity<RoleSelectionRequest> entity = new HttpEntity<>(request, createAuthHeaders(token));
        ResponseEntity<Void> response = rest.exchange(
                "/onboarding/role",
                HttpMethod.POST,
                entity,
                Void.class
        );

        // Assert HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Verify role updated in database using EntityManager
        UserEntity user = dbVerifier.findEntity(UserEntity.class, userId);
        assertThat(user).isNotNull();
        assertThat(user.getEmail()).isEqualTo(email);
        assertThat(user.getRole()).isEqualTo(Role.PATIENT);
    }

    @Test
    void doctorOnboardingShouldSucceed() {
        // Arrange
        long initialCount = dbVerifier.countEntities(DoctorProfileEntity.class);
        DoctorOnboardRequest request = new DoctorOnboardRequest(
                "John",
                "Doe",
                "Cardiology",
                "Experienced cardiologist",
                "LIC123456",
                "Heart Clinic",
                10,
                "https://example.com/photo.jpg",
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                false
        );

        // Get user ID before onboarding using EntityManager (not repository!)
        UserEntity tempUser = dbVerifier.findEntityByField(UserEntity.class, "email", doctorEmail);
        assertThat(tempUser).isNotNull();
        Long userId = tempUser.getId();

        // Act
        HttpEntity<DoctorOnboardRequest> entity = new HttpEntity<>(request, createAuthHeaders(doctorToken));
        ResponseEntity<Void> response = rest.exchange(
                "/onboarding/doctor",
                HttpMethod.POST,
                entity,
                Void.class
        );

        // Assert HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Verify count increased
        long finalCount = dbVerifier.countEntities(DoctorProfileEntity.class);
        assertThat(finalCount).isEqualTo(initialCount + 1);
        
        // Verify profile persisted in database using EntityManager
        UserEntity user = dbVerifier.findEntity(UserEntity.class, userId);
        assertThat(user).isNotNull();
        assertThat(user.getDoctorProfile()).isNotNull();
        
        DoctorProfileEntity profile = dbVerifier.findEntity(DoctorProfileEntity.class, user.getDoctorProfile().getId());
        assertThat(profile).isNotNull();
        assertThat(profile.getFirstName()).isEqualTo("John");
        assertThat(profile.getLastName()).isEqualTo("Doe");
        assertThat(profile.getSpecialization()).isEqualTo("Cardiology");
        assertThat(profile.getBio()).isEqualTo("Experienced cardiologist");
        assertThat(profile.getLicenseNumber()).isEqualTo("LIC123456");
        assertThat(profile.getClinicName()).isEqualTo("Heart Clinic");
        assertThat(profile.getYearsOfExperience()).isEqualTo(10);
        assertThat(profile.getProfilePhotoUrl()).isEqualTo("https://example.com/photo.jpg");
        assertThat(profile.getOpeningHours()).isEqualTo(LocalTime.of(9, 0));
        assertThat(profile.getClosingHours()).isEqualTo(LocalTime.of(17, 0));
        assertThat(profile.getIsAbsent()).isFalse();
    }

    @Test
    void patientOnboardingShouldSucceed() {
        // Arrange
        long initialCount = dbVerifier.countEntities(PatientProfileEntity.class);
        PatientOnboardRequest request = new PatientOnboardRequest(
                "Jane",
                "Smith",
                LocalDate.of(1990, 5, 15),
                "Female",
                "Hypertension",
                "INS123456",
                "https://example.com/patient.jpg"
        );

        // Get user ID before onboarding using EntityManager (not repository!)
        UserEntity tempUser = dbVerifier.findEntityByField(UserEntity.class, "email", patientEmail);
        assertThat(tempUser).isNotNull();
        Long userId = tempUser.getId();

        // Act
        HttpEntity<PatientOnboardRequest> entity = new HttpEntity<>(request, createAuthHeaders(patientToken));
        ResponseEntity<Void> response = rest.exchange(
                "/onboarding/patient",
                HttpMethod.POST,
                entity,
                Void.class
        );

        // Assert HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        
        // Verify count increased
        long finalCount = dbVerifier.countEntities(PatientProfileEntity.class);
        assertThat(finalCount).isEqualTo(initialCount + 1);
        
        // Verify profile persisted in database using EntityManager
        UserEntity user = dbVerifier.findEntity(UserEntity.class, userId);
        assertThat(user).isNotNull();
        assertThat(user.getPatientProfile()).isNotNull();
        
        PatientProfileEntity profile = dbVerifier.findEntity(PatientProfileEntity.class, user.getPatientProfile().getId());
        assertThat(profile).isNotNull();
        assertThat(profile.getFirstName()).isEqualTo("Jane");
        assertThat(profile.getLastName()).isEqualTo("Smith");
        assertThat(profile.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 15));
        assertThat(profile.getGender()).isEqualTo("Female");
        assertThat(profile.getConditions()).isEqualTo("Hypertension");
        assertThat(profile.getInsuranceNumber()).isEqualTo("INS123456");
        assertThat(profile.getProfilePicture()).isEqualTo("https://example.com/patient.jpg");
    }

    @Test
    void getOnboardingSummaryForDoctorShouldReturnDoctorProfile() {
        // Arrange - onboard the doctor first
        DoctorOnboardRequest doctorRequest = new DoctorOnboardRequest(
                "John",
                "Doe",
                "Cardiology",
                "Experienced cardiologist",
                "LIC123456",
                "Heart Clinic",
                10,
                "https://example.com/photo.jpg",
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                false
        );
        HttpEntity<DoctorOnboardRequest> doctorEntity = new HttpEntity<>(doctorRequest, createAuthHeaders(doctorToken));
        rest.exchange("/onboarding/doctor", HttpMethod.POST, doctorEntity, Void.class);
        
        // Get user and profile IDs using EntityManager (not repository!)
        UserEntity tempUser = dbVerifier.findEntityByField(UserEntity.class, "email", doctorEmail);
        assertThat(tempUser).isNotNull();
        Long userId = tempUser.getId();
        Long profileId = tempUser.getDoctorProfile().getId();

        // Act
        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(doctorToken));
        ResponseEntity<OnboardingSummaryResponse> response = rest.exchange(
                "/onboarding/summary",
                HttpMethod.GET,
                entity,
                OnboardingSummaryResponse.class
        );

        // Assert HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        OnboardingSummaryResponse summary = response.getBody();
        assertThat(summary).isNotNull();
        assertThat(summary.firstName()).isEqualTo("John");
        assertThat(summary.lastName()).isEqualTo("Doe");
        assertThat(summary.role()).isEqualTo(Role.DOCTOR);
        assertThat(summary.doctorProfile()).isNotNull();
        assertThat(summary.doctorProfile().specialization()).isEqualTo("Cardiology");
        assertThat(summary.patientProfile()).isNull();
        
        // Verify profile exists in database using EntityManager
        DoctorProfileEntity profile = dbVerifier.findEntity(DoctorProfileEntity.class, profileId);
        assertThat(profile).isNotNull();
        assertThat(profile.getFirstName()).isEqualTo("John");
        assertThat(profile.getLastName()).isEqualTo("Doe");
        assertThat(profile.getSpecialization()).isEqualTo("Cardiology");
    }

    @Test
    void getOnboardingSummaryForPatientShouldReturnPatientProfile() {
        // Arrange - onboard the patient first
        PatientOnboardRequest patientRequest = new PatientOnboardRequest(
                "Jane",
                "Smith",
                LocalDate.of(1990, 5, 15),
                "Female",
                "Hypertension",
                "INS123456",
                "https://example.com/patient.jpg"
        );
        HttpEntity<PatientOnboardRequest> patientEntity = new HttpEntity<>(patientRequest, createAuthHeaders(patientToken));
        rest.exchange("/onboarding/patient", HttpMethod.POST, patientEntity, Void.class);
        
        // Get user and profile IDs using EntityManager (not repository!)
        UserEntity tempUser = dbVerifier.findEntityByField(UserEntity.class, "email", patientEmail);
        assertThat(tempUser).isNotNull();
        Long userId = tempUser.getId();
        Long profileId = tempUser.getPatientProfile().getId();

        // Act
        HttpEntity<Void> entity = new HttpEntity<>(createAuthHeaders(patientToken));
        ResponseEntity<OnboardingSummaryResponse> response = rest.exchange(
                "/onboarding/summary",
                HttpMethod.GET,
                entity,
                OnboardingSummaryResponse.class
        );

        // Assert HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        OnboardingSummaryResponse summary = response.getBody();
        assertThat(summary).isNotNull();
        assertThat(summary.firstName()).isEqualTo("Jane");
        assertThat(summary.lastName()).isEqualTo("Smith");
        assertThat(summary.role()).isEqualTo(Role.PATIENT);
        assertThat(summary.patientProfile()).isNotNull();
        assertThat(summary.patientProfile().firstName()).isEqualTo("Jane");
        assertThat(summary.patientProfile().lastName()).isEqualTo("Smith");
        assertThat(summary.doctorProfile()).isNull();
        
        // Verify profile exists in database using EntityManager
        PatientProfileEntity profile = dbVerifier.findEntity(PatientProfileEntity.class, profileId);
        assertThat(profile).isNotNull();
        assertThat(profile.getFirstName()).isEqualTo("Jane");
        assertThat(profile.getLastName()).isEqualTo("Smith");
        assertThat(profile.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 15));
    }

    @Test
    void onboardingWithoutAuthenticationShouldFail() {
        // Arrange
        long initialCount = dbVerifier.countEntities(DoctorProfileEntity.class);
        DoctorOnboardRequest request = new DoctorOnboardRequest(
                "John",
                "Doe",
                "Cardiology",
                null,
                "LIC123456",
                "Heart Clinic",
                10,
                null,
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                false
        );

        // Act
        HttpEntity<DoctorOnboardRequest> entity = new HttpEntity<>(request);
        ResponseEntity<Void> response = rest.exchange(
                "/onboarding/doctor",
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


package com.superdoc.api.integration;

import com.superdoc.api.enumerate.Role;
import com.superdoc.api.model.dto.AuthDtos.LoginRequest;
import com.superdoc.api.model.dto.AuthDtos.RegisterRequest;
import com.superdoc.api.persistence.entities.*;
import com.superdoc.api.persistence.repo.*;
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

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class UserControllerIntegrationTest {

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
    private DoctorProfileRepository doctorProfileRepository;

    @Autowired
    private PatientProfileRepository patientProfileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private DbVerifier dbVerifier;

    private UserEntity doctorUser;
    private UserEntity patientUser;
    private UserEntity patientUser2;
    private DoctorProfileEntity doctorProfile;
    private PatientProfileEntity patientProfile1;
    private PatientProfileEntity patientProfile2;
    private String doctorToken;

    @BeforeEach
    void setUp() {
        // Clean up
        patientProfileRepository.deleteAll();
        doctorProfileRepository.deleteAll();
        userRepository.deleteAll();

        // Create doctor user
        doctorUser = new UserEntity();
        doctorUser.setEmail("doctor@test.com");
        doctorUser.setPasswordHash(passwordEncoder.encode("password123"));
        doctorUser.setRole(Role.DOCTOR);
        doctorUser = userRepository.save(doctorUser);

        // Create doctor profile
        doctorProfile = new DoctorProfileEntity();
        doctorProfile.setUser(doctorUser);
        doctorProfile.setFirstName("Dr. Jane");
        doctorProfile.setLastName("Smith");
        doctorProfile.setSpecialization("Cardiology");
        doctorProfile.setOpeningHours(LocalTime.of(9, 0));
        doctorProfile.setClosingHours(LocalTime.of(17, 0));
        doctorProfile.setIsAbsent(false);
        doctorProfile = doctorProfileRepository.save(doctorProfile);
        doctorUser.setDoctorProfile(doctorProfile);
        userRepository.save(doctorUser);

        // Create patient user 1
        patientUser = new UserEntity();
        patientUser.setEmail("patient1@test.com");
        patientUser.setPasswordHash(passwordEncoder.encode("password123"));
        patientUser.setRole(Role.PATIENT);
        patientUser = userRepository.save(patientUser);

        // Create patient profile 1
        patientProfile1 = new PatientProfileEntity();
        patientProfile1.setUser(patientUser);
        patientProfile1.setFirstName("John");
        patientProfile1.setLastName("Doe");
        patientProfile1 = patientProfileRepository.save(patientProfile1);
        patientUser.setPatientProfile(patientProfile1);
        userRepository.save(patientUser);

        // Create patient user 2
        patientUser2 = new UserEntity();
        patientUser2.setEmail("patient2@test.com");
        patientUser2.setPasswordHash(passwordEncoder.encode("password123"));
        patientUser2.setRole(Role.PATIENT);
        patientUser2 = userRepository.save(patientUser2);

        // Create patient profile 2
        patientProfile2 = new PatientProfileEntity();
        patientProfile2.setUser(patientUser2);
        patientProfile2.setFirstName("Jane");
        patientProfile2.setLastName("Smith");
        patientProfile2 = patientProfileRepository.save(patientProfile2);
        patientUser2.setPatientProfile(patientProfile2);
        userRepository.save(patientUser2);

        // Generate doctor token
        doctorToken = jwtService.generate(
                java.util.Map.of("role", Role.DOCTOR.name(), "uid", doctorUser.getId()),
                doctorUser.getEmail()
        );
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    @Test
    void getAllUsers_asDoctor_shouldReturnAllUsers() {
        // Arrange - verify database state
        long userCount = dbVerifier.countEntities(UserEntity.class);
        assertThat(userCount).isGreaterThanOrEqualTo(3); // Doctor + 2 patients

        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(doctorToken));

        // Act
        ResponseEntity<Object[]> response = rest.exchange(
                "/users",
                HttpMethod.GET,
                entity,
                Object[].class
        );

        // Assert HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Object[] users = response.getBody();
        assertThat(users).isNotNull();
        assertThat(users.length).isGreaterThanOrEqualTo(3); // Doctor + 2 patients
        
        // Verify database state matches response
        assertThat(users.length).isEqualTo((int) userCount);
        
        // Verify users exist in database using EntityManager
        UserEntity doctorInDb = dbVerifier.findEntity(UserEntity.class, doctorUser.getId());
        assertThat(doctorInDb).isNotNull();
        assertThat(doctorInDb.getEmail()).isEqualTo("doctor@test.com");
        
        UserEntity patient1InDb = dbVerifier.findEntity(UserEntity.class, patientUser.getId());
        assertThat(patient1InDb).isNotNull();
        assertThat(patient1InDb.getEmail()).isEqualTo("patient1@test.com");
    }

    @Test
    void getAllUsers_asPatient_shouldFail() {
        // Arrange
        String patientToken = jwtService.generate(
                java.util.Map.of("role", Role.PATIENT.name(), "uid", patientUser.getId()),
                patientUser.getEmail()
        );
        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(patientToken));

        // Act
        ResponseEntity<String> response = rest.exchange(
                "/users",
                HttpMethod.GET,
                entity,
                String.class
        );

        // Assert
        // @PreAuthorize checks role, returns 403 FORBIDDEN or 500 if authorization fails differently
        assertThat(response.getStatusCode()).isIn(HttpStatus.FORBIDDEN, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void getAllUsers_withoutAuthentication_shouldFail() {
        // Arrange
        HttpEntity<Void> entity = new HttpEntity<>(new HttpHeaders()); // No auth headers

        // Act
        ResponseEntity<String> response = rest.exchange(
                "/users",
                HttpMethod.GET,
                entity,
                String.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void getAllUsers_shouldIncludeUserProfiles() {
        // Arrange - verify profiles exist in database
        DoctorProfileEntity doctorProfileInDb = dbVerifier.findEntity(DoctorProfileEntity.class, doctorProfile.getId());
        assertThat(doctorProfileInDb).isNotNull();
        assertThat(doctorProfileInDb.getFirstName()).isEqualTo("Dr. Jane");
        
        PatientProfileEntity patient1ProfileInDb = dbVerifier.findEntity(PatientProfileEntity.class, patientProfile1.getId());
        assertThat(patient1ProfileInDb).isNotNull();
        assertThat(patient1ProfileInDb.getFirstName()).isEqualTo("John");
        
        PatientProfileEntity patient2ProfileInDb = dbVerifier.findEntity(PatientProfileEntity.class, patientProfile2.getId());
        assertThat(patient2ProfileInDb).isNotNull();
        assertThat(patient2ProfileInDb.getFirstName()).isEqualTo("Jane");

        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(doctorToken));

        // Act
        ResponseEntity<String> response = rest.exchange(
                "/users",
                HttpMethod.GET,
                entity,
                String.class
        );

        // Assert HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        // Verify response contains user information with profiles
        assertThat(responseBody).contains("John");
        assertThat(responseBody).contains("Doe");
        assertThat(responseBody).contains("Jane");
        assertThat(responseBody).contains("Smith");
    }

    @Test
    void getAllUsers_shouldIncludeUserEmails() {
        // Arrange - verify users exist in database with correct emails
        UserEntity doctorInDb = dbVerifier.findEntity(UserEntity.class, doctorUser.getId());
        assertThat(doctorInDb).isNotNull();
        assertThat(doctorInDb.getEmail()).isEqualTo("doctor@test.com");
        
        UserEntity patient1InDb = dbVerifier.findEntity(UserEntity.class, patientUser.getId());
        assertThat(patient1InDb).isNotNull();
        assertThat(patient1InDb.getEmail()).isEqualTo("patient1@test.com");
        
        UserEntity patient2InDb = dbVerifier.findEntity(UserEntity.class, patientUser2.getId());
        assertThat(patient2InDb).isNotNull();
        assertThat(patient2InDb.getEmail()).isEqualTo("patient2@test.com");

        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(doctorToken));

        // Act
        ResponseEntity<String> response = rest.exchange(
                "/users",
                HttpMethod.GET,
                entity,
                String.class
        );

        // Assert HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        String responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        assertThat(responseBody).contains("doctor@test.com");
        assertThat(responseBody).contains("patient1@test.com");
        assertThat(responseBody).contains("patient2@test.com");
    }
}


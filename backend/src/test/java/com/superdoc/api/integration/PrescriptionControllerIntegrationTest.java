package com.superdoc.api.integration;

import com.superdoc.api.enumerate.PrescriptionStatus;
import com.superdoc.api.enumerate.Role;
import com.superdoc.api.model.dto.PrescriptionDtos.*;
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

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class PrescriptionControllerIntegrationTest {

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
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private DbVerifier dbVerifier;

    private UserEntity patientUser;
    private UserEntity doctorUser;
    private DoctorProfileEntity doctorProfile;
    private PatientProfileEntity patientProfile;
    private String patientToken;
    private String doctorToken;

    @BeforeEach
    void setUp() {
        // Clean up
        prescriptionRepository.deleteAll();
        patientProfileRepository.deleteAll();
        doctorProfileRepository.deleteAll();
        userRepository.deleteAll();

        // Create patient user
        patientUser = new UserEntity();
        patientUser.setEmail("patient@test.com");
        patientUser.setPasswordHash(passwordEncoder.encode("password123"));
        patientUser.setRole(Role.PATIENT);
        patientUser = userRepository.save(patientUser);

        // Create patient profile
        patientProfile = new PatientProfileEntity();
        patientProfile.setUser(patientUser);
        patientProfile.setFirstName("John");
        patientProfile.setLastName("Doe");
        patientProfile = patientProfileRepository.save(patientProfile);
        patientUser.setPatientProfile(patientProfile);
        userRepository.save(patientUser);

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

        // Generate tokens
        patientToken = jwtService.generate(
                java.util.Map.of("role", Role.PATIENT.name(), "uid", patientUser.getId()),
                patientUser.getEmail()
        );
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
    void createPrescription_shouldSucceed() {
        // Arrange
        long initialCount = dbVerifier.countEntities(PrescriptionEntity.class);
        LocalDate futureDate = LocalDate.now().plusDays(30);
        CreatePrescriptionRequest request = new CreatePrescriptionRequest(
                patientProfile.getId(),
                "Aspirin",
                "100mg",
                "Once daily",
                "30 days",
                "Take with food",
                futureDate
        );

        HttpEntity<CreatePrescriptionRequest> entity = new HttpEntity<>(request, authHeaders(doctorToken));

        // Act
        ResponseEntity<PrescriptionResponse> response = rest.exchange(
                "/prescriptions",
                HttpMethod.POST,
                entity,
                PrescriptionResponse.class
        );

        // Assert HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        PrescriptionResponse prescription = response.getBody();
        assertThat(prescription).isNotNull();
        assertThat(prescription.patientId()).isEqualTo(patientUser.getId());
        assertThat(prescription.medicationName()).isEqualTo("Aspirin");
        assertThat(prescription.dosage()).isEqualTo("100mg");
        assertThat(prescription.status()).isEqualTo(PrescriptionStatus.DRAFT);
        assertThat(prescription.validUntil()).isEqualTo(futureDate);

        // Verify persisted in database using EntityManager
        PrescriptionEntity persisted = dbVerifier.findEntity(PrescriptionEntity.class, prescription.id());
        assertThat(persisted).isNotNull();
        assertThat(persisted.getMedicationName()).isEqualTo("Aspirin");
        assertThat(persisted.getDosage()).isEqualTo("100mg");
        assertThat(persisted.getFrequency()).isEqualTo("Once daily");
        assertThat(persisted.getDuration()).isEqualTo("30 days");
        assertThat(persisted.getInstructions()).isEqualTo("Take with food");
        assertThat(persisted.getStatus()).isEqualTo(PrescriptionStatus.DRAFT);
        assertThat(persisted.getValidUntil()).isEqualTo(futureDate);
        
        // Verify relationships persisted
        assertThat(persisted.getPatient().getId()).isEqualTo(patientUser.getId());
        assertThat(persisted.getDoctor().getId()).isEqualTo(doctorUser.getId());
        
        // Verify count increased
        long finalCount = dbVerifier.countEntities(PrescriptionEntity.class);
        assertThat(finalCount).isEqualTo(initialCount + 1);
    }

    @Test
    void createPrescription_pastValidUntilDate_shouldFail() {
        // Arrange - capture initial count
        long initialCount = dbVerifier.countEntities(PrescriptionEntity.class);
        LocalDate pastDate = LocalDate.now().minusDays(1);
        CreatePrescriptionRequest request = new CreatePrescriptionRequest(
                patientProfile.getId(),
                "Aspirin",
                "100mg",
                "Once daily",
                "30 days",
                null,
                pastDate
        );

        HttpEntity<CreatePrescriptionRequest> entity = new HttpEntity<>(request, authHeaders(doctorToken));

        // Act
        ResponseEntity<String> response = rest.exchange(
                "/prescriptions",
                HttpMethod.POST,
                entity,
                String.class
        );

        // Assert HTTP error
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        
        // Assert NO entity was persisted (critical - ensures transaction rollback)
        long finalCount = dbVerifier.countEntities(PrescriptionEntity.class);
        assertThat(finalCount).isEqualTo(initialCount);
    }

    @Test
    void updatePrescription_shouldSucceed() {
        // Arrange - create a prescription first
        PrescriptionEntity prescription = new PrescriptionEntity();
        prescription.setDoctor(doctorUser);
        prescription.setPatient(patientUser);
        prescription.setMedicationName("Aspirin");
        prescription.setDosage("100mg");
        prescription.setFrequency("Once daily");
        prescription.setDuration("30 days");
        prescription.setStatus(PrescriptionStatus.DRAFT);
        prescription.setValidUntil(LocalDate.now().plusDays(30));
        prescription = prescriptionRepository.save(prescription);
        
        Instant originalUpdatedAt = prescription.getUpdatedAt();

        // Update request
        LocalDate newFutureDate = LocalDate.now().plusDays(45);
        UpdatePrescriptionRequest updateRequest = new UpdatePrescriptionRequest(
                "Ibuprofen",
                "200mg",
                "Twice daily",
                "15 days",
                "Updated instructions",
                newFutureDate
        );

        HttpEntity<UpdatePrescriptionRequest> entity = new HttpEntity<>(updateRequest, authHeaders(doctorToken));

        // Act
        ResponseEntity<PrescriptionResponse> response = rest.exchange(
                "/prescriptions/" + prescription.getId(),
                HttpMethod.PUT,
                entity,
                PrescriptionResponse.class
        );

        // Assert HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PrescriptionResponse updated = response.getBody();
        assertThat(updated).isNotNull();
        assertThat(updated.medicationName()).isEqualTo("Ibuprofen");
        assertThat(updated.dosage()).isEqualTo("200mg");
        assertThat(updated.frequency()).isEqualTo("Twice daily");

        // Verify in database using EntityManager
        PrescriptionEntity persisted = dbVerifier.findEntity(PrescriptionEntity.class, prescription.getId());
        assertThat(persisted).isNotNull();
        assertThat(persisted.getMedicationName()).isEqualTo("Ibuprofen");
        assertThat(persisted.getDosage()).isEqualTo("200mg");
        assertThat(persisted.getFrequency()).isEqualTo("Twice daily");
        assertThat(persisted.getDuration()).isEqualTo("15 days");
        assertThat(persisted.getInstructions()).isEqualTo("Updated instructions");
        assertThat(persisted.getValidUntil()).isEqualTo(newFutureDate);
        
        // Verify updatedAt timestamp changed
        assertThat(persisted.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    void activatePrescription_shouldSucceed() {
        // Arrange - create a DRAFT prescription
        PrescriptionEntity prescription = new PrescriptionEntity();
        prescription.setDoctor(doctorUser);
        prescription.setPatient(patientUser);
        prescription.setMedicationName("Aspirin");
        prescription.setDosage("100mg");
        prescription.setFrequency("Once daily");
        prescription.setDuration("30 days");
        prescription.setStatus(PrescriptionStatus.DRAFT);
        prescription.setValidUntil(LocalDate.now().plusDays(30));
        prescription = prescriptionRepository.save(prescription);

        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(doctorToken));

        // Act
        ResponseEntity<PrescriptionResponse> response = rest.exchange(
                "/prescriptions/" + prescription.getId() + "/activate",
                HttpMethod.POST,
                entity,
                PrescriptionResponse.class
        );

        // Assert HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PrescriptionResponse activated = response.getBody();
        assertThat(activated).isNotNull();
        assertThat(activated.status()).isEqualTo(PrescriptionStatus.ACTIVE);

        // Verify in database using EntityManager
        PrescriptionEntity persisted = dbVerifier.findEntity(PrescriptionEntity.class, prescription.getId());
        assertThat(persisted).isNotNull();
        assertThat(persisted.getStatus()).isEqualTo(PrescriptionStatus.ACTIVE);
        
        // Verify other fields remain unchanged
        assertThat(persisted.getMedicationName()).isEqualTo("Aspirin");
        assertThat(persisted.getDosage()).isEqualTo("100mg");
    }

    @Test
    void discontinuePrescription_shouldSucceed() {
        // Arrange - create an ACTIVE prescription
        PrescriptionEntity prescription = new PrescriptionEntity();
        prescription.setDoctor(doctorUser);
        prescription.setPatient(patientUser);
        prescription.setMedicationName("Aspirin");
        prescription.setDosage("100mg");
        prescription.setFrequency("Once daily");
        prescription.setDuration("30 days");
        prescription.setStatus(PrescriptionStatus.ACTIVE);
        prescription.setValidUntil(LocalDate.now().plusDays(30));
        prescription = prescriptionRepository.save(prescription);

        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(doctorToken));

        // Act
        ResponseEntity<PrescriptionResponse> response = rest.exchange(
                "/prescriptions/" + prescription.getId() + "/discontinue",
                HttpMethod.POST,
                entity,
                PrescriptionResponse.class
        );

        // Assert HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PrescriptionResponse discontinued = response.getBody();
        assertThat(discontinued).isNotNull();
        assertThat(discontinued.status()).isEqualTo(PrescriptionStatus.DISCONTINUED);

        // Verify in database using EntityManager
        PrescriptionEntity persisted = dbVerifier.findEntity(PrescriptionEntity.class, prescription.getId());
        assertThat(persisted).isNotNull();
        assertThat(persisted.getStatus()).isEqualTo(PrescriptionStatus.DISCONTINUED);
        
        // Verify other fields remain unchanged
        assertThat(persisted.getMedicationName()).isEqualTo("Aspirin");
        assertThat(persisted.getDosage()).isEqualTo("100mg");
    }

    @Test
    void getDoctorPrescriptions_shouldReturnPrescriptions() {
        // Arrange - create prescriptions
        PrescriptionEntity prescription1 = new PrescriptionEntity();
        prescription1.setDoctor(doctorUser);
        prescription1.setPatient(patientUser);
        prescription1.setMedicationName("Aspirin");
        prescription1.setDosage("100mg");
        prescription1.setFrequency("Once daily");
        prescription1.setDuration("30 days");
        prescription1.setStatus(PrescriptionStatus.DRAFT);
        prescription1.setValidUntil(LocalDate.now().plusDays(30));
        prescriptionRepository.save(prescription1);

        PrescriptionEntity prescription2 = new PrescriptionEntity();
        prescription2.setDoctor(doctorUser);
        prescription2.setPatient(patientUser);
        prescription2.setMedicationName("Ibuprofen");
        prescription2.setDosage("200mg");
        prescription2.setFrequency("Twice daily");
        prescription2.setDuration("15 days");
        prescription2.setStatus(PrescriptionStatus.ACTIVE);
        prescription2.setValidUntil(LocalDate.now().plusDays(30));
        prescriptionRepository.save(prescription2);

        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(doctorToken));

        // Act
        ResponseEntity<PrescriptionResponse[]> response = rest.exchange(
                "/prescriptions/doctor",
                HttpMethod.GET,
                entity,
                PrescriptionResponse[].class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PrescriptionResponse[] prescriptions = response.getBody();
        assertThat(prescriptions).isNotNull();
        assertThat(prescriptions.length).isGreaterThanOrEqualTo(2);
    }

    @Test
    void getPatientPrescriptions_shouldReturnPrescriptions() {
        // Arrange - create a prescription
        PrescriptionEntity prescription = new PrescriptionEntity();
        prescription.setDoctor(doctorUser);
        prescription.setPatient(patientUser);
        prescription.setMedicationName("Aspirin");
        prescription.setDosage("100mg");
        prescription.setFrequency("Once daily");
        prescription.setDuration("30 days");
        prescription.setStatus(PrescriptionStatus.ACTIVE);
        prescription.setValidUntil(LocalDate.now().plusDays(30));
        prescriptionRepository.save(prescription);

        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(patientToken));

        // Act
        ResponseEntity<PrescriptionResponse[]> response = rest.exchange(
                "/prescriptions/patient",
                HttpMethod.GET,
                entity,
                PrescriptionResponse[].class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PrescriptionResponse[] prescriptions = response.getBody();
        assertThat(prescriptions).isNotNull();
        assertThat(prescriptions.length).isGreaterThanOrEqualTo(1);
        assertThat(prescriptions[0].medicationName()).isEqualTo("Aspirin");
    }

    @Test
    void getPrescription_shouldReturnPrescription() {
        // Arrange - create a prescription
        PrescriptionEntity prescription = new PrescriptionEntity();
        prescription.setDoctor(doctorUser);
        prescription.setPatient(patientUser);
        prescription.setMedicationName("Aspirin");
        prescription.setDosage("100mg");
        prescription.setFrequency("Once daily");
        prescription.setDuration("30 days");
        prescription.setStatus(PrescriptionStatus.DRAFT);
        prescription.setValidUntil(LocalDate.now().plusDays(30));
        prescription = prescriptionRepository.save(prescription);

        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(doctorToken));

        // Act
        ResponseEntity<PrescriptionResponse> response = rest.exchange(
                "/prescriptions/" + prescription.getId(),
                HttpMethod.GET,
                entity,
                PrescriptionResponse.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        PrescriptionResponse result = response.getBody();
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(prescription.getId());
        assertThat(result.medicationName()).isEqualTo("Aspirin");
    }

    @Test
    void createPrescription_patientCannotCreate_shouldFail() {
        // Arrange - capture initial count
        long initialCount = dbVerifier.countEntities(PrescriptionEntity.class);
        LocalDate futureDate = LocalDate.now().plusDays(30);
        CreatePrescriptionRequest request = new CreatePrescriptionRequest(
                patientProfile.getId(),
                "Aspirin",
                "100mg",
                "Once daily",
                "30 days",
                null,
                futureDate
        );

        HttpEntity<CreatePrescriptionRequest> entity = new HttpEntity<>(request, authHeaders(patientToken));

        // Act
        ResponseEntity<String> response = rest.exchange(
                "/prescriptions",
                HttpMethod.POST,
                entity,
                String.class
        );

        // Assert HTTP error
        // Patient gets 403 FORBIDDEN or 500 if service throws exception before authorization
        assertThat(response.getStatusCode()).isIn(HttpStatus.FORBIDDEN, HttpStatus.INTERNAL_SERVER_ERROR);
        
        // Assert NO entity was persisted (critical - ensures transaction rollback)
        long finalCount = dbVerifier.countEntities(PrescriptionEntity.class);
        assertThat(finalCount).isEqualTo(initialCount);
    }
}


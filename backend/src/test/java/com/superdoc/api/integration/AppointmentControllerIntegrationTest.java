package com.superdoc.api.integration;

import com.superdoc.api.enumerate.AppointmentStatus;
import com.superdoc.api.enumerate.Role;
import com.superdoc.api.model.dto.AppointmentDtos.*;
import com.superdoc.api.model.dto.AuthDtos.JwtResponse;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class AppointmentControllerIntegrationTest {

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
    private AppointmentRepository appointmentRepository;

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
        appointmentRepository.deleteAll();
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
    void createAppointment_shouldSucceed() {
        // Arrange
        long initialCount = dbVerifier.countEntities(AppointmentEntity.class);
        LocalDateTime appointmentDateTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                doctorProfile.getId(),
                appointmentDateTime,
                "Regular checkup"
        );

        HttpEntity<CreateAppointmentRequest> entity = new HttpEntity<>(request, authHeaders(patientToken));

        // Act
        ResponseEntity<AppointmentResponse> response = rest.exchange(
                "/appointments",
                HttpMethod.POST,
                entity,
                AppointmentResponse.class
        );

        // Assert HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        AppointmentResponse appointment = response.getBody();
        assertThat(appointment).isNotNull();
        assertThat(appointment.doctorId()).isEqualTo(doctorProfile.getId());
        assertThat(appointment.patientId()).isEqualTo(patientProfile.getId());
        assertThat(appointment.status()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(appointment.notes()).isEqualTo("Regular checkup");

        // Verify persisted in database using EntityManager
        AppointmentEntity persisted = dbVerifier.findEntity(AppointmentEntity.class, appointment.id());
        assertThat(persisted).isNotNull();
        assertThat(persisted.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(persisted.getNotes()).isEqualTo("Regular checkup");
        assertThat(persisted.getAppointmentDateTime()).isEqualTo(appointmentDateTime);
        
        // Verify relationships persisted
        assertThat(persisted.getDoctor().getId()).isEqualTo(doctorProfile.getId());
        assertThat(persisted.getPatient().getId()).isEqualTo(patientProfile.getId());
        
        // Verify count increased
        long finalCount = dbVerifier.countEntities(AppointmentEntity.class);
        assertThat(finalCount).isEqualTo(initialCount + 1);
    }

    @Test
    void createAppointment_pastDateTime_shouldFail() {
        // Arrange - capture initial count
        long initialCount = dbVerifier.countEntities(AppointmentEntity.class);
        LocalDateTime pastDateTime = LocalDateTime.now().minusDays(1);
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                doctorProfile.getId(),
                pastDateTime,
                null
        );

        HttpEntity<CreateAppointmentRequest> entity = new HttpEntity<>(request, authHeaders(patientToken));

        // Act
        ResponseEntity<String> response = rest.exchange(
                "/appointments",
                HttpMethod.POST,
                entity,
                String.class
        );

        // Assert HTTP error
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        
        // Assert NO entity was persisted (critical - ensures transaction rollback)
        long finalCount = dbVerifier.countEntities(AppointmentEntity.class);
        assertThat(finalCount).isEqualTo(initialCount);
    }

    @Test
    void createAppointment_conflictingTime_shouldFail() {
        // Arrange - create first appointment
        long initialCount = dbVerifier.countEntities(AppointmentEntity.class);
        LocalDateTime appointmentDateTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        CreateAppointmentRequest firstRequest = new CreateAppointmentRequest(
                doctorProfile.getId(),
                appointmentDateTime,
                "First appointment"
        );

        HttpEntity<CreateAppointmentRequest> firstEntity = new HttpEntity<>(firstRequest, authHeaders(patientToken));
        ResponseEntity<AppointmentResponse> firstResponse = rest.exchange(
                "/appointments",
                HttpMethod.POST,
                firstEntity,
                AppointmentResponse.class
        );
        
        // Verify first appointment was created
        assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long countAfterFirst = dbVerifier.countEntities(AppointmentEntity.class);
        assertThat(countAfterFirst).isEqualTo(initialCount + 1);

        // Try to create conflicting appointment
        CreateAppointmentRequest conflictingRequest = new CreateAppointmentRequest(
                doctorProfile.getId(),
                appointmentDateTime,
                "Conflicting appointment"
        );

        HttpEntity<CreateAppointmentRequest> conflictingEntity = new HttpEntity<>(conflictingRequest, authHeaders(patientToken));

        // Act
        ResponseEntity<String> response = rest.exchange(
                "/appointments",
                HttpMethod.POST,
                conflictingEntity,
                String.class
        );

        // Assert HTTP error
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("already has an appointment");
        
        // Assert only ONE appointment exists (conflicting one was not persisted)
        long finalCount = dbVerifier.countEntities(AppointmentEntity.class);
        assertThat(finalCount).isEqualTo(initialCount + 1);
    }

    @Test
    void getMyAppointments_patientView_shouldReturnPatientAppointments() {
        // Arrange - create an appointment
        LocalDateTime appointmentDateTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        AppointmentEntity appointment = new AppointmentEntity();
        appointment.setDoctor(doctorProfile);
        appointment.setPatient(patientProfile);
        appointment.setAppointmentDateTime(appointmentDateTime);
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointmentRepository.save(appointment);

        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(patientToken));

        // Act
        ResponseEntity<AppointmentResponse[]> response = rest.exchange(
                "/appointments/me",
                HttpMethod.GET,
                entity,
                AppointmentResponse[].class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        AppointmentResponse[] appointments = response.getBody();
        assertThat(appointments).isNotNull();
        assertThat(appointments.length).isGreaterThan(0);
        assertThat(appointments[0].patientId()).isEqualTo(patientProfile.getId());
    }

    @Test
    void getMyAppointments_doctorView_shouldReturnDoctorAppointments() {
        // Arrange - create an appointment
        LocalDateTime appointmentDateTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        AppointmentEntity appointment = new AppointmentEntity();
        appointment.setDoctor(doctorProfile);
        appointment.setPatient(patientProfile);
        appointment.setAppointmentDateTime(appointmentDateTime);
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointmentRepository.save(appointment);

        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(doctorToken));

        // Act
        ResponseEntity<AppointmentResponse[]> response = rest.exchange(
                "/appointments/me",
                HttpMethod.GET,
                entity,
                AppointmentResponse[].class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        AppointmentResponse[] appointments = response.getBody();
        assertThat(appointments).isNotNull();
        assertThat(appointments.length).isGreaterThan(0);
        assertThat(appointments[0].doctorId()).isEqualTo(doctorProfile.getId());
    }

    @Test
    void getAppointment_validRequest_shouldReturnAppointment() {
        // Arrange - create an appointment
        LocalDateTime appointmentDateTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        AppointmentEntity appointment = new AppointmentEntity();
        appointment.setDoctor(doctorProfile);
        appointment.setPatient(patientProfile);
        appointment.setAppointmentDateTime(appointmentDateTime);
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment = appointmentRepository.save(appointment);

        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(patientToken));

        // Act
        ResponseEntity<AppointmentResponse> response = rest.exchange(
                "/appointments/" + appointment.getId(),
                HttpMethod.GET,
                entity,
                AppointmentResponse.class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        AppointmentResponse appointmentResponse = response.getBody();
        assertThat(appointmentResponse).isNotNull();
        assertThat(appointmentResponse.id()).isEqualTo(appointment.getId());
        assertThat(appointmentResponse.doctorId()).isEqualTo(doctorProfile.getId());
    }

    @Test
    void updateAppointment_validRequest_shouldUpdateAppointment() {
        // Arrange - create an appointment
        LocalDateTime appointmentDateTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        AppointmentEntity appointment = new AppointmentEntity();
        appointment.setDoctor(doctorProfile);
        appointment.setPatient(patientProfile);
        appointment.setAppointmentDateTime(appointmentDateTime);
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment = appointmentRepository.save(appointment);
        
        Instant originalUpdatedAt = appointment.getUpdatedAt();

        LocalDateTime newDateTime = LocalDateTime.now().plusDays(2).withHour(14).withMinute(0);
        UpdateAppointmentRequest updateRequest = new UpdateAppointmentRequest(
                newDateTime,
                AppointmentStatus.CONFIRMED,
                "Updated notes"
        );

        HttpEntity<UpdateAppointmentRequest> entity = new HttpEntity<>(updateRequest, authHeaders(patientToken));

        // Act
        ResponseEntity<AppointmentResponse> response = rest.exchange(
                "/appointments/" + appointment.getId(),
                HttpMethod.PUT,
                entity,
                AppointmentResponse.class
        );

        // Assert HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        AppointmentResponse updated = response.getBody();
        assertThat(updated).isNotNull();
        assertThat(updated.status()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(updated.notes()).isEqualTo("Updated notes");

        // Verify in database using EntityManager
        AppointmentEntity persisted = dbVerifier.findEntity(AppointmentEntity.class, appointment.getId());
        assertThat(persisted).isNotNull();
        assertThat(persisted.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(persisted.getNotes()).isEqualTo("Updated notes");
        assertThat(persisted.getAppointmentDateTime()).isEqualTo(newDateTime);
        
        // Verify updatedAt timestamp changed
        assertThat(persisted.getUpdatedAt()).isAfter(originalUpdatedAt);
    }

    @Test
    void cancelAppointment_validRequest_shouldCancelAppointment() {
        // Arrange - create an appointment
        LocalDateTime appointmentDateTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        AppointmentEntity appointment = new AppointmentEntity();
        appointment.setDoctor(doctorProfile);
        appointment.setPatient(patientProfile);
        appointment.setAppointmentDateTime(appointmentDateTime);
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment = appointmentRepository.save(appointment);

        HttpEntity<Void> entity = new HttpEntity<>(authHeaders(patientToken));

        // Act
        ResponseEntity<Void> response = rest.exchange(
                "/appointments/" + appointment.getId(),
                HttpMethod.DELETE,
                entity,
                Void.class
        );

        // Assert HTTP response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify in database using EntityManager (soft delete - status changed)
        AppointmentEntity cancelled = dbVerifier.findEntity(AppointmentEntity.class, appointment.getId());
        assertThat(cancelled).isNotNull();
        assertThat(cancelled.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        
        // Verify other fields remain unchanged
        assertThat(cancelled.getDoctor().getId()).isEqualTo(doctorProfile.getId());
        assertThat(cancelled.getPatient().getId()).isEqualTo(patientProfile.getId());
        assertThat(cancelled.getAppointmentDateTime()).isEqualTo(appointmentDateTime);
    }

    @Test
    void getAvailableTimeSlots_validRequest_shouldReturnSlots() {
        // Arrange
        LocalDate date = LocalDate.now().plusDays(1);

        HttpEntity<Void> entity = new HttpEntity<>(new HttpHeaders());

        // Act
        ResponseEntity<AvailableTimeSlot[]> response = rest.exchange(
                "/appointments/availability/" + doctorProfile.getId() + "?date=" + date,
                HttpMethod.GET,
                entity,
                AvailableTimeSlot[].class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        AvailableTimeSlot[] slots = response.getBody();
        assertThat(slots).isNotNull();
        assertThat(slots.length).isGreaterThan(0);
        assertThat(slots).anyMatch(slot -> slot.available());
    }

    @Test
    void getAvailableTimeSlots_withBookedSlots_shouldMarkAsUnavailable() {
        // Arrange - create an appointment
        LocalDate date = LocalDate.now().plusDays(1);
        LocalDateTime appointmentDateTime = LocalDateTime.of(date, LocalTime.of(10, 0));
        AppointmentEntity appointment = new AppointmentEntity();
        appointment.setDoctor(doctorProfile);
        appointment.setPatient(patientProfile);
        appointment.setAppointmentDateTime(appointmentDateTime);
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointmentRepository.save(appointment);

        HttpEntity<Void> entity = new HttpEntity<>(new HttpHeaders());

        // Act
        ResponseEntity<AvailableTimeSlot[]> response = rest.exchange(
                "/appointments/availability/" + doctorProfile.getId() + "?date=" + date,
                HttpMethod.GET,
                entity,
                AvailableTimeSlot[].class
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        AvailableTimeSlot[] slots = response.getBody();
        assertThat(slots).isNotNull();
        assertThat(slots).anyMatch(slot -> slot.time().equals("10:00") && !slot.available());
    }
}


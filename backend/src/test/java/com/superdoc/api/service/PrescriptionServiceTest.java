package com.superdoc.api.service;

import com.superdoc.api.enumerate.PrescriptionStatus;
import com.superdoc.api.enumerate.Role;
import com.superdoc.api.model.dto.PrescriptionDtos.*;
import com.superdoc.api.BLL.domain.Prescription;
import com.superdoc.api.BLL.domain.User;
import com.superdoc.api.BLL.domain.Patient;
import com.superdoc.api.BLL.IRepositories.IPrescriptionRepository;
import com.superdoc.api.BLL.IRepositories.IUserRepository;
import com.superdoc.api.BLL.IRepositories.IPatientProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceTest {

    @Mock private IPrescriptionRepository prescriptionRepository;
    @Mock private IUserRepository userRepository;
    @Mock private IPatientProfileRepository patientProfileRepository;

    @InjectMocks private PrescriptionService service;

    private User doctor;
    private User patient;
    private Patient patientProfile;
    private Prescription prescription;

    @BeforeEach
    void setUp() {
        // Setup doctor
        doctor = User.builder()
                .id(1L)
                .email("doctor@test.com")
                .role(Role.DOCTOR)
                .build();

        // Setup patient
        patient = User.builder()
                .id(2L)
                .email("patient@test.com")
                .role(Role.PATIENT)
                .build();

        // Setup patient profile
        patientProfile = Patient.builder()
                .id(100L)
                .firstName("John")
                .lastName("Doe")
                .user(patient)
                .build();

        // Setup prescription
        prescription = Prescription.builder()
                .id(1L)
                .doctor(doctor)
                .patient(patient)
                .medicationName("Aspirin")
                .dosage("100mg")
                .frequency("Once daily")
                .duration("7 days")
                .instructions("Take with food")
                .status(PrescriptionStatus.DRAFT)
                .validUntil(LocalDate.now().plusDays(30))
                .issuedAt(Instant.now())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void createPrescription_validRequest_savesAndReturnsPrescription() {
        // Arrange
        CreatePrescriptionRequest request = new CreatePrescriptionRequest(
            100L, // patient profile ID
            "Aspirin",
            "100mg",
            "Once daily",
            "7 days",
            "Take with food",
            LocalDate.now().plusDays(30)
        );

        when(userRepository.findByEmail("doctor@test.com")).thenReturn(Optional.of(doctor));
        when(patientProfileRepository.findById(100L)).thenReturn(Optional.of(patientProfile));
        when(prescriptionRepository.save(any(Prescription.class))).thenReturn(prescription);

        // Act
        PrescriptionResponse response = service.createPrescription("doctor@test.com", request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.medicationName()).isEqualTo("Aspirin");
        assertThat(response.status()).isEqualTo(PrescriptionStatus.DRAFT);
        assertThat(response.patientId()).isEqualTo(2L); // patient user ID
        assertThat(response.doctorId()).isEqualTo(1L);

        verify(prescriptionRepository).save(argThat(p -> 
            p.getMedicationName().equals("Aspirin") &&
            p.getStatus() == PrescriptionStatus.DRAFT &&
            p.getDoctor() != null &&
            p.getPatient() != null
        ));
    }

    @Test
    void createPrescription_userNotFound_throwsException() {
        // Arrange
        CreatePrescriptionRequest request = new CreatePrescriptionRequest(
            100L, "Aspirin", "100mg", "Once daily", "7 days", null,
            LocalDate.now().plusDays(30)
        );

        when(userRepository.findByEmail("doctor@test.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.createPrescription("doctor@test.com", request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("User not found");
    }

    @Test
    void createPrescription_nonDoctor_throwsException() {
        // Arrange
        User nonDoctor = User.builder()
                .role(Role.PATIENT)
                .build();
        
        CreatePrescriptionRequest request = new CreatePrescriptionRequest(
            100L, "Aspirin", "100mg", "Once daily", "7 days", null,
            LocalDate.now().plusDays(30)
        );

        when(userRepository.findByEmail("patient@test.com")).thenReturn(Optional.of(nonDoctor));

        // Act & Assert
        assertThatThrownBy(() -> service.createPrescription("patient@test.com", request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Only doctors can create prescriptions");
    }

    @Test
    void createPrescription_patientProfileNotFound_throwsException() {
        // Arrange
        CreatePrescriptionRequest request = new CreatePrescriptionRequest(
            999L, "Aspirin", "100mg", "Once daily", "7 days", null,
            LocalDate.now().plusDays(30)
        );

        when(userRepository.findByEmail("doctor@test.com")).thenReturn(Optional.of(doctor));
        when(patientProfileRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.createPrescription("doctor@test.com", request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Patient profile not found");
    }

    @Test
    void createPrescription_pastValidUntilDate_throwsException() {
        // Arrange
        CreatePrescriptionRequest request = new CreatePrescriptionRequest(
            100L, "Aspirin", "100mg", "Once daily", "7 days", null,
            LocalDate.now().minusDays(1) // Past date
        );

        when(userRepository.findByEmail("doctor@test.com")).thenReturn(Optional.of(doctor));
        when(patientProfileRepository.findById(100L)).thenReturn(Optional.of(patientProfile));

        // Act & Assert
        assertThatThrownBy(() -> service.createPrescription("doctor@test.com", request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Valid until date must be in the future");
    }

    @Test
    void updatePrescription_validRequest_updatesPrescription() {
        // Arrange
        UpdatePrescriptionRequest request = new UpdatePrescriptionRequest(
            "Ibuprofen", "200mg", "Twice daily", "10 days", "Take after meals", null
        );

        when(userRepository.findByEmail("doctor@test.com")).thenReturn(Optional.of(doctor));
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(prescriptionRepository.save(any(Prescription.class))).thenReturn(prescription);

        // Act
        PrescriptionResponse response = service.updatePrescription(1L, "doctor@test.com", request);

        // Assert
        assertThat(response).isNotNull();
        verify(prescriptionRepository).save(argThat(p -> 
            p.getMedicationName().equals("Ibuprofen") &&
            p.getDosage().equals("200mg")
        ));
    }

    @Test
    void updatePrescription_notOwner_throwsException() {
        // Arrange
        User otherDoctor = User.builder()
                .id(999L)
                .email("other@test.com")
                .role(Role.DOCTOR)
                .build();

        UpdatePrescriptionRequest request = new UpdatePrescriptionRequest(
            "Ibuprofen", null, null, null, null, null
        );

        when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(otherDoctor));
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));

        // Act & Assert
        assertThatThrownBy(() -> service.updatePrescription(1L, "other@test.com", request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("You can only update prescriptions you created");
    }

    @Test
    void updatePrescription_activeStatus_throwsException() {
        // Arrange
        prescription.setStatus(PrescriptionStatus.ACTIVE);
        UpdatePrescriptionRequest request = new UpdatePrescriptionRequest(
            "Ibuprofen", null, null, null, null, null
        );

        when(userRepository.findByEmail("doctor@test.com")).thenReturn(Optional.of(doctor));
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));

        // Act & Assert
        assertThatThrownBy(() -> service.updatePrescription(1L, "doctor@test.com", request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Cannot edit a prescription that is already ACTIVE");
    }

    @Test
    void activatePrescription_validDraftPrescription_activates() {
        // Arrange
        when(userRepository.findByEmail("doctor@test.com")).thenReturn(Optional.of(doctor));
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(prescriptionRepository.save(any(Prescription.class))).thenReturn(prescription);

        // Act
        PrescriptionResponse response = service.activatePrescription(1L, "doctor@test.com");

        // Assert
        assertThat(response.status()).isEqualTo(PrescriptionStatus.ACTIVE);
        verify(prescriptionRepository).save(argThat(p -> 
            p.getStatus() == PrescriptionStatus.ACTIVE
        ));
    }

    @Test
    void activatePrescription_notDraftStatus_throwsException() {
        // Arrange
        prescription.setStatus(PrescriptionStatus.ACTIVE);
        when(userRepository.findByEmail("doctor@test.com")).thenReturn(Optional.of(doctor));
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));

        // Act & Assert
        assertThatThrownBy(() -> service.activatePrescription(1L, "doctor@test.com"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Can only activate prescriptions with DRAFT status");
    }

    @Test
    void discontinuePrescription_validActivePrescription_discontinues() {
        // Arrange
        prescription.setStatus(PrescriptionStatus.ACTIVE);
        when(userRepository.findByEmail("doctor@test.com")).thenReturn(Optional.of(doctor));
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));
        when(prescriptionRepository.save(any(Prescription.class))).thenReturn(prescription);

        // Act
        PrescriptionResponse response = service.discontinuePrescription(1L, "doctor@test.com");

        // Assert
        assertThat(response.status()).isEqualTo(PrescriptionStatus.DISCONTINUED);
        verify(prescriptionRepository).save(argThat(p -> 
            p.getStatus() == PrescriptionStatus.DISCONTINUED
        ));
    }

    @Test
    void discontinuePrescription_notActiveStatus_throwsException() {
        // Arrange
        prescription.setStatus(PrescriptionStatus.DRAFT);
        when(userRepository.findByEmail("doctor@test.com")).thenReturn(Optional.of(doctor));
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));

        // Act & Assert
        assertThatThrownBy(() -> service.discontinuePrescription(1L, "doctor@test.com"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Can only discontinue ACTIVE prescriptions");
    }

    @Test
    void getDoctorPrescriptions_validDoctor_returnsPrescriptions() {
        // Arrange
        when(userRepository.findByEmail("doctor@test.com")).thenReturn(Optional.of(doctor));
        when(prescriptionRepository.findByDoctor_IdOrderByCreatedAtDesc(1L)).thenReturn(List.of(prescription));

        // Act
        List<PrescriptionResponse> responses = service.getDoctorPrescriptions("doctor@test.com");

        // Assert
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).medicationName()).isEqualTo("Aspirin");
    }

    @Test
    void getPatientPrescriptions_validPatient_returnsPrescriptions() {
        // Arrange
        when(userRepository.findByEmail("patient@test.com")).thenReturn(Optional.of(patient));
        when(prescriptionRepository.findByPatientEmail("patient@test.com")).thenReturn(List.of(prescription));

        // Act
        List<PrescriptionResponse> responses = service.getPatientPrescriptions("patient@test.com");

        // Assert
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).medicationName()).isEqualTo("Aspirin");
    }

    @Test
    void getPrescription_asDoctor_returnsPrescription() {
        // Arrange
        when(userRepository.findByEmail("doctor@test.com")).thenReturn(Optional.of(doctor));
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));

        // Act
        PrescriptionResponse response = service.getPrescription(1L, "doctor@test.com");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void getPrescription_asPatient_returnsPrescription() {
        // Arrange
        when(userRepository.findByEmail("patient@test.com")).thenReturn(Optional.of(patient));
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));

        // Act
        PrescriptionResponse response = service.getPrescription(1L, "patient@test.com");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
    }

    @Test
    void getPrescription_unauthorizedAccess_throwsException() {
        // Arrange
        User otherUser = User.builder()
                .id(999L)
                .email("other@test.com")
                .role(Role.PATIENT)
                .build();

        when(userRepository.findByEmail("other@test.com")).thenReturn(Optional.of(otherUser));
        when(prescriptionRepository.findById(1L)).thenReturn(Optional.of(prescription));

        // Act & Assert
        assertThatThrownBy(() -> service.getPrescription(1L, "other@test.com"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("You don't have permission to view this prescription");
    }
}


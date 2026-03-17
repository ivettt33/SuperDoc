package com.superdoc.api.repository;

import com.superdoc.api.enumerate.PrescriptionStatus;
import com.superdoc.api.enumerate.Role;
import com.superdoc.api.persistence.entities.PrescriptionEntity;
import com.superdoc.api.persistence.entities.UserEntity;
import com.superdoc.api.persistence.entities.PatientProfileEntity;
import com.superdoc.api.persistence.repo.PrescriptionRepository;
import com.superdoc.api.persistence.repo.UserRepository;
import com.superdoc.api.persistence.repo.PatientProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class PrescriptionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PrescriptionRepository prescriptionRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientProfileRepository patientProfileRepository;

    private UserEntity doctor;
    private UserEntity patient;
    private PatientProfileEntity patientProfile;
    private PrescriptionEntity prescription1;
    private PrescriptionEntity prescription2;

    @BeforeEach
    void setUp() {
        // Create and persist doctor
        doctor = new UserEntity();
        doctor.setEmail("doctor@test.com");
        doctor.setPasswordHash("hash");
        doctor.setRole(Role.DOCTOR);
        doctor = entityManager.persistFlushFind(doctor);

        // Create and persist patient
        patient = new UserEntity();
        patient.setEmail("patient@test.com");
        patient.setPasswordHash("hash");
        patient.setRole(Role.PATIENT);
        patient = entityManager.persistFlushFind(patient);

        // Create and persist patient profile
        patientProfile = new PatientProfileEntity();
        patientProfile.setFirstName("John");
        patientProfile.setLastName("Doe");
        patientProfile.setUser(patient);
        patientProfile = entityManager.persistFlushFind(patientProfile);
        patient.setPatientProfile(patientProfile);
        patient = entityManager.merge(patient);

        // Create prescription 1 - DRAFT
        prescription1 = new PrescriptionEntity();
        prescription1.setDoctor(doctor);
        prescription1.setPatient(patient);
        prescription1.setMedicationName("Aspirin");
        prescription1.setDosage("100mg");
        prescription1.setFrequency("Once daily");
        prescription1.setDuration("7 days");
        prescription1.setStatus(PrescriptionStatus.DRAFT);
        prescription1.setValidUntil(LocalDate.now().plusDays(30));
        prescription1.setIssuedAt(Instant.now());
        prescription1.setCreatedAt(Instant.now());
        prescription1.setUpdatedAt(Instant.now());
        prescription1 = entityManager.persistFlushFind(prescription1);

        // Create prescription 2 - ACTIVE
        prescription2 = new PrescriptionEntity();
        prescription2.setDoctor(doctor);
        prescription2.setPatient(patient);
        prescription2.setMedicationName("Ibuprofen");
        prescription2.setDosage("200mg");
        prescription2.setFrequency("Twice daily");
        prescription2.setDuration("10 days");
        prescription2.setStatus(PrescriptionStatus.ACTIVE);
        prescription2.setValidUntil(LocalDate.now().plusDays(20));
        prescription2.setIssuedAt(Instant.now());
        prescription2.setCreatedAt(Instant.now());
        prescription2.setUpdatedAt(Instant.now());
        prescription2 = entityManager.persistFlushFind(prescription2);

        entityManager.clear();
    }

    @Test
    void findByDoctorId_returnsPrescriptionsForDoctor() {
        // Act
        List<PrescriptionEntity> prescriptions = prescriptionRepository.findByDoctor_IdOrderByCreatedAtDesc(doctor.getId());

        // Assert
        assertThat(prescriptions).hasSize(2);
        assertThat(prescriptions).extracting(PrescriptionEntity::getMedicationName)
            .containsExactlyInAnyOrder("Aspirin", "Ibuprofen");
    }

    @Test
    void findByPatientId_returnsPrescriptionsForPatient() {
        // Act
        List<PrescriptionEntity> prescriptions = prescriptionRepository.findByPatient_IdOrderByCreatedAtDesc(patient.getId());

        // Assert
        assertThat(prescriptions).hasSize(2);
        assertThat(prescriptions).extracting(PrescriptionEntity::getMedicationName)
            .containsExactlyInAnyOrder("Aspirin", "Ibuprofen");
    }

    @Test
    void findByPatientEmail_returnsPrescriptionsForPatientEmail() {
        // Act
        List<PrescriptionEntity> prescriptions = prescriptionRepository.findByPatientEmail("patient@test.com");

        // Assert
        assertThat(prescriptions).hasSize(2);
        assertThat(prescriptions).extracting(PrescriptionEntity::getPatient)
            .extracting(UserEntity::getEmail)
            .containsOnly("patient@test.com");
    }

    @Test
    void findByDoctorIdAndStatus_returnsFilteredPrescriptions() {
        // Act
        List<PrescriptionEntity> draftPrescriptions = prescriptionRepository.findByDoctor_IdAndStatusOrderByCreatedAtDesc(
            doctor.getId(), PrescriptionStatus.DRAFT);
        List<PrescriptionEntity> activePrescriptions = prescriptionRepository.findByDoctor_IdAndStatusOrderByCreatedAtDesc(
            doctor.getId(), PrescriptionStatus.ACTIVE);

        // Assert
        assertThat(draftPrescriptions).hasSize(1);
        assertThat(draftPrescriptions.get(0).getMedicationName()).isEqualTo("Aspirin");
        assertThat(draftPrescriptions.get(0).getStatus()).isEqualTo(PrescriptionStatus.DRAFT);

        assertThat(activePrescriptions).hasSize(1);
        assertThat(activePrescriptions.get(0).getMedicationName()).isEqualTo("Ibuprofen");
        assertThat(activePrescriptions.get(0).getStatus()).isEqualTo(PrescriptionStatus.ACTIVE);
    }

    @Test
    void findByIdWithRelations_loadsRelations() {
        // Act
        Optional<PrescriptionEntity> found = prescriptionRepository.findById(prescription1.getId());

        // Assert
        assertThat(found).isPresent();
        PrescriptionEntity p = found.get();
        assertThat(p.getDoctor()).isNotNull();
        assertThat(p.getPatient()).isNotNull();
        assertThat(p.getDoctor().getEmail()).isEqualTo("doctor@test.com");
        assertThat(p.getPatient().getEmail()).isEqualTo("patient@test.com");
    }

    @Test
    void findExpiredPrescriptions_returnsExpiredActivePrescriptions() {
        // Arrange - Create an expired prescription
        PrescriptionEntity expired = new PrescriptionEntity();
        expired.setDoctor(doctor);
        expired.setPatient(patient);
        expired.setMedicationName("Expired Medication");
        expired.setDosage("50mg");
        expired.setFrequency("Once daily");
        expired.setDuration("5 days");
        expired.setStatus(PrescriptionStatus.ACTIVE);
        expired.setValidUntil(LocalDate.now().minusDays(1)); 
        expired.setIssuedAt(Instant.now());
        expired.setCreatedAt(Instant.now());
        expired.setUpdatedAt(Instant.now());
        entityManager.persistFlushFind(expired);

        // Act
        List<PrescriptionEntity> expiredPrescriptions = prescriptionRepository.findByStatusAndValidUntilBefore(PrescriptionStatus.ACTIVE, LocalDate.now());

        // Assert
        assertThat(expiredPrescriptions).hasSize(1);
        assertThat(expiredPrescriptions.get(0).getMedicationName()).isEqualTo("Expired Medication");
        assertThat(expiredPrescriptions.get(0).getStatus()).isEqualTo(PrescriptionStatus.ACTIVE);
        assertThat(expiredPrescriptions.get(0).getValidUntil()).isBefore(LocalDate.now());
    }

    @Test
    void save_newPrescription_persistsCorrectly() {
        // Arrange
        PrescriptionEntity newPrescription = new PrescriptionEntity();
        newPrescription.setDoctor(doctor);
        newPrescription.setPatient(patient);
        newPrescription.setMedicationName("New Medication");
        newPrescription.setDosage("150mg");
        newPrescription.setFrequency("Three times daily");
        newPrescription.setDuration("14 days");
        newPrescription.setStatus(PrescriptionStatus.DRAFT);
        newPrescription.setValidUntil(LocalDate.now().plusDays(15));
        newPrescription.setIssuedAt(Instant.now());
        newPrescription.setCreatedAt(Instant.now());
        newPrescription.setUpdatedAt(Instant.now());

        // Act
        PrescriptionEntity saved = prescriptionRepository.save(newPrescription);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Optional<PrescriptionEntity> found = prescriptionRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getMedicationName()).isEqualTo("New Medication");
        assertThat(found.get().getDoctor().getId()).isEqualTo(doctor.getId());
        assertThat(found.get().getPatient().getId()).isEqualTo(patient.getId());
    }

    @Test
    void delete_removesPrescription() {
        // Arrange
        Long id = prescription1.getId();

        // Act
        prescriptionRepository.delete(prescription1);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Optional<PrescriptionEntity> found = prescriptionRepository.findById(id);
        assertThat(found).isEmpty();
    }
}


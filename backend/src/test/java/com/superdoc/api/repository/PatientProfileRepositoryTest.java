package com.superdoc.api.repository;

import com.superdoc.api.enumerate.Role;
import com.superdoc.api.persistence.entities.PatientProfileEntity;
import com.superdoc.api.persistence.entities.UserEntity;
import com.superdoc.api.persistence.repo.PatientProfileRepository;
import com.superdoc.api.persistence.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
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
class PatientProfileRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PatientProfileRepository patientProfileRepository;

    @Autowired
    private UserRepository userRepository;

    private UserEntity patientUser;
    private PatientProfileEntity profile;

    @BeforeEach
    void setUp() {
        // Create user
        patientUser = new UserEntity();
        patientUser.setEmail("patient@test.com");
        patientUser.setPasswordHash("hash");
        patientUser.setRole(Role.PATIENT);
        patientUser = entityManager.persistFlushFind(patientUser);

        // Create profile
        profile = new PatientProfileEntity();
        profile.setFirstName("John");
        profile.setLastName("Doe");
        profile.setDateOfBirth(LocalDate.of(1990, 1, 1));
        profile.setGender("Male");
        profile.setConditions("None");
        profile.setInsuranceNumber("INS123");
        profile.setUser(patientUser);
        profile = entityManager.persistFlushFind(profile);

        patientUser.setPatientProfile(profile);
        entityManager.merge(patientUser);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findById_existingId_returnsProfile() {
        // Act
        Optional<PatientProfileEntity> found = patientProfileRepository.findById(profile.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("John");
        assertThat(found.get().getLastName()).isEqualTo("Doe");
        assertThat(found.get().getUser().getEmail()).isEqualTo("patient@test.com");
    }

    @Test
    void findById_nonExistentId_returnsEmpty() {
        // Act
        Optional<PatientProfileEntity> found = patientProfileRepository.findById(999L);

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    void findAll_returnsAllProfiles() {
        // Arrange - Create another profile
        UserEntity user2 = new UserEntity();
        user2.setEmail("patient2@test.com");
        user2.setPasswordHash("hash");
        user2.setRole(Role.PATIENT);
        user2 = entityManager.persistFlushFind(user2);

        PatientProfileEntity profile2 = new PatientProfileEntity();
        profile2.setFirstName("Jane");
        profile2.setLastName("Smith");
        profile2.setDateOfBirth(LocalDate.of(1995, 5, 15));
        profile2.setUser(user2);
        entityManager.persistFlushFind(profile2);

        entityManager.clear();

        // Act
        List<PatientProfileEntity> allProfiles = patientProfileRepository.findAll();

        // Assert
        assertThat(allProfiles).hasSize(2);
        assertThat(allProfiles).extracting(PatientProfileEntity::getFirstName)
            .containsExactlyInAnyOrder("John", "Jane");
    }

    @Test
    void save_newProfile_persistsCorrectly() {
        // Arrange
        UserEntity newUser = new UserEntity();
        newUser.setEmail("newpatient@test.com");
        newUser.setPasswordHash("hash");
        newUser.setRole(Role.PATIENT);
        newUser = entityManager.persistFlushFind(newUser);

        PatientProfileEntity newProfile = new PatientProfileEntity();
        newProfile.setFirstName("New");
        newProfile.setLastName("Patient");
        newProfile.setDateOfBirth(LocalDate.of(2000, 1, 1));
        newProfile.setUser(newUser);

        // Act
        PatientProfileEntity saved = patientProfileRepository.save(newProfile);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Optional<PatientProfileEntity> found = patientProfileRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("New");
        assertThat(found.get().getLastName()).isEqualTo("Patient");
        assertThat(found.get().getUser().getEmail()).isEqualTo("newpatient@test.com");
    }

    @Test
    void save_updateProfile_updatesCorrectly() {
        // Arrange
        profile.setFirstName("Updated");
        profile.setLastName("Name");
        profile.setConditions("Diabetes");

        // Act
        PatientProfileEntity updated = patientProfileRepository.save(profile);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Optional<PatientProfileEntity> found = patientProfileRepository.findById(updated.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getFirstName()).isEqualTo("Updated");
        assertThat(found.get().getLastName()).isEqualTo("Name");
        assertThat(found.get().getConditions()).isEqualTo("Diabetes");
    }

    @Test
    void delete_removesProfile() {
        // Arrange
        Long id = profile.getId();

        // Act
        patientProfileRepository.delete(profile);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Optional<PatientProfileEntity> found = patientProfileRepository.findById(id);
        assertThat(found).isEmpty();
    }

    @Test
    void findByUser_relationshipWorks() {
        // Act
        Optional<PatientProfileEntity> found = patientProfileRepository.findById(profile.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getUser()).isNotNull();
        assertThat(found.get().getUser().getId()).isEqualTo(patientUser.getId());
        assertThat(found.get().getUser().getEmail()).isEqualTo("patient@test.com");
    }
}


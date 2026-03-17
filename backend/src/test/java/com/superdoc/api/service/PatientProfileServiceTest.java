package com.superdoc.api.service;

import com.superdoc.api.enumerate.Role;
import com.superdoc.api.model.dto.ProfileDtos.*;
import com.superdoc.api.BLL.domain.Patient;
import com.superdoc.api.BLL.domain.User;
import com.superdoc.api.BLL.IRepositories.IPatientProfileRepository;
import com.superdoc.api.BLL.IRepositories.IUserRepository;
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
class PatientProfileServiceTest {

    @Mock private IPatientProfileRepository patientProfileRepository;
    @Mock private IUserRepository userRepository;

    @InjectMocks private PatientProfileService service;

    private User user;
    private Patient profile;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("patient@test.com")
                .role(Role.PATIENT)
                .build();

        profile = Patient.builder()
                .id(100L)
                .firstName("John")
                .lastName("Doe")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .gender("Male")
                .user(user)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void createProfile_newProfile_createsProfile() {
        // Arrange
        User newUser = User.builder()
                .id(2L)
                .email("newpatient@test.com")
                .role(Role.PATIENT)
                .build();

        CreatePatientProfileRequest request = new CreatePatientProfileRequest(
            "Jane",
            "Smith",
            LocalDate.of(1995, 5, 15),
            "Female",
            "Diabetes",
            "INS123456",
            null
        );

        when(userRepository.findByEmail("newpatient@test.com")).thenReturn(Optional.of(newUser));
        when(patientProfileRepository.findAll()).thenReturn(List.of()); // No existing profile
        when(patientProfileRepository.save(any(Patient.class))).thenAnswer(invocation -> {
            Patient p = invocation.getArgument(0);
            p.setId(200L);
            return p;
        });

        // Act
        PatientProfileResponse result = service.createProfile("newpatient@test.com", request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.firstName()).isEqualTo("Jane");
        assertThat(result.lastName()).isEqualTo("Smith");
        assertThat(result.dateOfBirth()).isEqualTo(LocalDate.of(1995, 5, 15));
        assertThat(result.gender()).isEqualTo("Female");
        assertThat(result.conditions()).isEqualTo("Diabetes");
        assertThat(result.insuranceNumber()).isEqualTo("INS123456");

        verify(patientProfileRepository).save(any(Patient.class));
    }

    @Test
    void updateProfile_existingProfile_updatesProfile() {
        // Arrange
        UpdatePatientProfileRequest request = new UpdatePatientProfileRequest(
            "John Updated",
            "Doe Updated",
            LocalDate.of(1990, 1, 1),
            "Male",
            "Hypertension",
            "INS789",
            null
        );

        when(userRepository.findByEmail("patient@test.com")).thenReturn(Optional.of(user));
        when(patientProfileRepository.findAll()).thenReturn(List.of(profile)); // Existing profile
        when(patientProfileRepository.save(any(Patient.class))).thenReturn(profile);

        // Act
        PatientProfileResponse result = service.updateProfile("patient@test.com", request);

        // Assert
        assertThat(result).isNotNull();
        verify(patientProfileRepository).save(argThat(p -> 
            p.getFirstName().equals("John Updated") &&
            p.getLastName().equals("Doe Updated")
        ));
    }

    @Test
    void createProfile_userNotFound_throwsException() {
        // Arrange
        CreatePatientProfileRequest request = new CreatePatientProfileRequest(
            "John", "Doe", LocalDate.of(1990, 1, 1), null, null, null, null
        );

        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.createProfile("unknown@test.com", request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("User not found");
    }

    @Test
    void createProfile_missingFirstName_throwsException() {
        // Arrange
        CreatePatientProfileRequest request = new CreatePatientProfileRequest(
            "", "Doe", LocalDate.of(1990, 1, 1), null, null, null, null
        );

        when(userRepository.findByEmail("patient@test.com")).thenReturn(Optional.of(user));
        when(patientProfileRepository.findAll()).thenReturn(List.of());

        // Act & Assert
        assertThatThrownBy(() -> service.createProfile("patient@test.com", request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("First name is required");
    }

    @Test
    void createProfile_futureDateOfBirth_throwsException() {
        // Arrange
        CreatePatientProfileRequest request = new CreatePatientProfileRequest(
            "John", "Doe", LocalDate.now().plusDays(1), null, null, null, null
        );

        when(userRepository.findByEmail("patient@test.com")).thenReturn(Optional.of(user));
        when(patientProfileRepository.findAll()).thenReturn(List.of());

        // Act & Assert
        assertThatThrownBy(() -> service.createProfile("patient@test.com", request))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Date of birth must be in the past");
    }

    @Test
    void getProfileByEmail_validEmail_returnsProfile() {
        // Arrange
        when(userRepository.findByEmail("patient@test.com")).thenReturn(Optional.of(user));
        when(patientProfileRepository.findAll()).thenReturn(List.of(profile));

        // Act
        PatientProfileResponse result = service.getProfileByEmail("patient@test.com");

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.firstName()).isEqualTo("John");
    }

    @Test
    void getProfileByEmail_noProfile_throwsException() {
        // Arrange
        User userWithoutProfile = User.builder()
                .id(2L)
                .email("noprofile@test.com")
                .role(Role.PATIENT)
                .build();

        when(userRepository.findByEmail("noprofile@test.com")).thenReturn(Optional.of(userWithoutProfile));
        when(patientProfileRepository.findAll()).thenReturn(List.of()); // No profile

        // Act & Assert
        assertThatThrownBy(() -> service.getProfileByEmail("noprofile@test.com"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Patient profile not found");
    }

    @Test
    void hasProfile_withProfile_returnsTrue() {
        // Arrange
        when(userRepository.findByEmail("patient@test.com")).thenReturn(Optional.of(user));
        when(patientProfileRepository.findAll()).thenReturn(List.of(profile));

        // Act
        boolean result = service.hasProfile("patient@test.com");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void hasProfile_withoutProfile_returnsFalse() {
        // Arrange
        User userWithoutProfile = User.builder()
                .email("noprofile@test.com")
                .role(Role.PATIENT)
                .build();
        when(userRepository.findByEmail("noprofile@test.com")).thenReturn(Optional.of(userWithoutProfile));
        when(patientProfileRepository.findAll()).thenReturn(List.of()); // No profile

        // Act
        boolean result = service.hasProfile("noprofile@test.com");

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void getProfileById_validId_returnsProfile() {
        // Arrange
        when(patientProfileRepository.findById(100L)).thenReturn(Optional.of(profile));

        // Act
        PatientProfileResponse result = service.getProfileById(100L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(100L);
    }

    @Test
    void getProfileById_invalidId_throwsException() {
        // Arrange
        when(patientProfileRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.getProfileById(999L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Patient profile not found");
    }

    @Test
    void isOwnProfile_matchingEmail_returnsTrue() {
        // Arrange
        when(patientProfileRepository.findById(100L)).thenReturn(Optional.of(profile));

        // Act
        boolean result = service.isOwnProfile(100L, "patient@test.com");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void isOwnProfile_differentEmail_returnsFalse() {
        // Arrange
        when(patientProfileRepository.findById(100L)).thenReturn(Optional.of(profile));

        // Act
        boolean result = service.isOwnProfile(100L, "other@test.com");

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void getAllPatients_returnsListOfPatients() {
        // Arrange
        User user2 = User.builder()
                .email("jane@test.com")
                .build();
        Patient profile2 = Patient.builder()
                .id(101L)
                .firstName("Jane")
                .lastName("Smith")
                .user(user2)
                .build();

        when(patientProfileRepository.findAll()).thenReturn(List.of(profile, profile2));

        // Act
        List<PatientListResponse> result = service.getAllPatients();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0).firstName()).isEqualTo("John");
        assertThat(result.get(1).firstName()).isEqualTo("Jane");
        assertThat(result.get(0).profileId()).isEqualTo(100L);
        assertThat(result.get(1).profileId()).isEqualTo(101L);
    }
}

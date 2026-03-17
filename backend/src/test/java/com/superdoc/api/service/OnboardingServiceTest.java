package com.superdoc.api.service;

import com.superdoc.api.enumerate.Role;
import com.superdoc.api.model.dto.OnboardingDtos.*;
import com.superdoc.api.BLL.domain.User;
import com.superdoc.api.BLL.domain.Doctor;
import com.superdoc.api.BLL.domain.Patient;
import com.superdoc.api.BLL.IRepositories.IUserRepository;
import com.superdoc.api.BLL.IRepositories.IDoctorProfileRepository;
import com.superdoc.api.BLL.IRepositories.IPatientProfileRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @Mock private IUserRepository users;
    @Mock private IDoctorProfileRepository doctorProfileRepository;
    @Mock private IPatientProfileRepository patientProfileRepository;
    @Mock private DoctorProfileService doctorProfileService;
    @Mock private PatientProfileService patientProfileService;

    @InjectMocks private OnboardingService service;

    @Test
    void updateUserRole_existingUser_updatesRole() {
        // Arrange
        String email = "user@example.com";
        User user = User.builder()
                .email(email)
                .role(Role.PATIENT)
                .build();
        
        RoleSelectionRequest request = new RoleSelectionRequest(Role.DOCTOR);
        when(users.findByEmail(email)).thenReturn(Optional.of(user));

        // Act
        service.updateUserRole(email, request);

        // Assert
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(users).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo(Role.DOCTOR);
    }

    @Test
    void updateUserRole_userNotFound_throwsException() {
        // Arrange
        String email = "nonexistent@example.com";
        RoleSelectionRequest request = new RoleSelectionRequest(Role.DOCTOR);
        when(users.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.updateUserRole(email, request))
                .isInstanceOf(RuntimeException.class);
        
        verify(users, never()).save(any());
    }

    @Test
    void onboardDoctor_newProfile_createsAndSaves() {
        // Arrange
        String email = "doctor@example.com";

        DoctorOnboardRequest req = doctorRequest("John", "Doe");

        // Act
        service.onboardDoctor(email, req);

        // Assert
        verify(doctorProfileService).createProfile(eq(email), any());
    }

    @Test
    void onboardDoctor_existingProfile_updatesAndSaves() {
        // Arrange
        String email = "doctor@example.com";

        DoctorOnboardRequest req = doctorRequest("NewFirst", "NewLast");

        // Act
        service.onboardDoctor(email, req);

        // Assert
        verify(doctorProfileService).createProfile(eq(email), any());
    }

    @Test
    void onboardDoctor_userNotFound_throwsException() {
        // Arrange
        String email = "missing@example.com";
        DoctorOnboardRequest req = doctorRequest("John", "Doe");
        when(doctorProfileService.createProfile(eq(email), any()))
                .thenThrow(new IllegalArgumentException("User not found"));

        // Act & Assert
        assertThatThrownBy(() -> service.onboardDoctor(email, req))
                .isInstanceOf(IllegalArgumentException.class);

        verify(doctorProfileService).createProfile(eq(email), any());
    }

    @Test
    void onboardPatient_newProfile_createsAndSaves() {
        // Arrange
        String email = "patient@example.com";

        PatientOnboardRequest req = new PatientOnboardRequest(
                "Jane",
                "Smith",
                LocalDate.of(1990, 5, 15),
                "Female",
                "None",
                "INS-123",
                "photo.jpg"
        );

        // Act
        service.onboardPatient(email, req);

        // Assert
        verify(patientProfileService).createProfile(eq(email), any());
    }

    @Test
    void onboardPatient_existingProfile_updatesAndSaves() {
        // Arrange
        String email = "patient@example.com";

        PatientOnboardRequest req = new PatientOnboardRequest(
                "NewFirst",
                "NewLast",
                LocalDate.of(1995, 12, 31),
                "Male",
                "Diabetes",
                "INS-999",
                "newphoto.jpg"
        );

        // Act
        service.onboardPatient(email, req);

        // Assert
        verify(patientProfileService).createProfile(eq(email), any());
    }

    @Test
    void onboardPatient_userNotFound_throwsException() {
        // Arrange
        String email = "missing@example.com";
        PatientOnboardRequest req = new PatientOnboardRequest(
                "Jane",
                "Smith",
                LocalDate.of(1990, 5, 15),
                "Female",
                "None",
                "INS-123",
                "photo.jpg"
        );
        when(patientProfileService.createProfile(eq(email), any()))
                .thenThrow(new IllegalArgumentException("User not found"));

        // Act & Assert
        assertThatThrownBy(() -> service.onboardPatient(email, req))
                .isInstanceOf(IllegalArgumentException.class);

        verify(patientProfileService).createProfile(eq(email), any());
    }

    @Test
    void getOnboardingSummary_doctorWithProfile_returnsCorrectData() {
        // Arrange
        String email = "doctor@example.com";
        
        User user = User.builder()
                .email(email)
                .role(Role.DOCTOR)
                .id(1L)
                .build();
        
        Doctor doctorProfile = Doctor.builder()
                .firstName("John")
                .lastName("Doe")
                .specialization("Cardiology")
                .bio("Experienced")
                .licenseNumber("LIC-123")
                .clinicName("City Clinic")
                .yearsOfExperience(10)
                .profilePhotoUrl("photo.jpg")
                .user(user)
                .build();
        
        when(users.findByEmail(email)).thenReturn(Optional.of(user));
        when(doctorProfileRepository.findAll()).thenReturn(List.of(doctorProfile));
        when(patientProfileRepository.findAll()).thenReturn(List.of());

        // Act
        OnboardingSummaryResponse response = service.getOnboardingSummary(email);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.firstName()).isEqualTo("John");
        assertThat(response.lastName()).isEqualTo("Doe");
        assertThat(response.role()).isEqualTo(Role.DOCTOR);
        assertThat(response.doctorProfile()).isNotNull();
        assertThat(response.patientProfile()).isNull();
    }

    @Test
    void getOnboardingSummary_patientWithProfile_returnsCorrectData() {
        // Arrange
        String email = "patient@example.com";
        
        User user = User.builder()
                .email(email)
                .role(Role.PATIENT)
                .id(1L)
                .build();
        
        Patient patientProfile = Patient.builder()
                .firstName("Jane")
                .lastName("Smith")
                .dateOfBirth(LocalDate.of(1990, 5, 15))
                .gender("Female")
                .conditions("None")
                .insuranceNumber("INS-123")
                .profilePicture("photo.jpg")
                .user(user)
                .build();
        
        when(users.findByEmail(email)).thenReturn(Optional.of(user));
        when(doctorProfileRepository.findAll()).thenReturn(List.of());
        when(patientProfileRepository.findAll()).thenReturn(List.of(patientProfile));

        // Act
        OnboardingSummaryResponse response = service.getOnboardingSummary(email);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.firstName()).isEqualTo("Jane");
        assertThat(response.lastName()).isEqualTo("Smith");
        assertThat(response.role()).isEqualTo(Role.PATIENT);
        assertThat(response.patientProfile()).isNotNull();
        assertThat(response.doctorProfile()).isNull();
    }

    @Test
    void getOnboardingSummary_doctorWithoutProfile_returnsEmptyData() {
        // Arrange
        String email = "newdoctor@example.com";
        
        User user = User.builder()
                .email(email)
                .role(Role.DOCTOR)
                .id(1L)
                .build();
        
        when(users.findByEmail(email)).thenReturn(Optional.of(user));
        when(doctorProfileRepository.findAll()).thenReturn(List.of()); // No profile
        when(patientProfileRepository.findAll()).thenReturn(List.of());

        // Act
        OnboardingSummaryResponse response = service.getOnboardingSummary(email);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.firstName()).isEmpty();
        assertThat(response.lastName()).isEmpty();
        assertThat(response.role()).isEqualTo(Role.DOCTOR);
        assertThat(response.doctorProfile()).isNull();
        assertThat(response.patientProfile()).isNull();
    }

    @Test
    void getOnboardingSummary_patientWithoutProfile_returnsEmptyData() {
        // Arrange
        String email = "newpatient@example.com";
        
        User user = User.builder()
                .email(email)
                .role(Role.PATIENT)
                .id(1L)
                .build();
        
        when(users.findByEmail(email)).thenReturn(Optional.of(user));
        when(doctorProfileRepository.findAll()).thenReturn(List.of());
        when(patientProfileRepository.findAll()).thenReturn(List.of()); // No profile

        // Act
        OnboardingSummaryResponse response = service.getOnboardingSummary(email);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.firstName()).isEmpty();
        assertThat(response.lastName()).isEmpty();
        assertThat(response.role()).isEqualTo(Role.PATIENT);
        assertThat(response.doctorProfile()).isNull();
        assertThat(response.patientProfile()).isNull();
    }

    @Test
    void getOnboardingSummary_userNotFound_throwsException() {
        // Arrange
        String email = "nonexistent@example.com";
        when(users.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.getOnboardingSummary(email))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    private DoctorOnboardRequest doctorRequest(String firstName, String lastName) {
        return new DoctorOnboardRequest(
                firstName,
                lastName,
                "Cardiology",
                "Experienced specialist",
                "LIC-12345",
                "City Clinic",
                8,
                null,
                java.time.LocalTime.of(9, 0),
                java.time.LocalTime.of(17, 0),
                false
        );
    }
}

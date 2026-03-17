package com.superdoc.api.service;

import com.superdoc.api.model.dto.ProfileDtos.*;
import com.superdoc.api.BLL.domain.Doctor;
import com.superdoc.api.BLL.domain.User;
import com.superdoc.api.BLL.IRepositories.IDoctorProfileRepository;
import com.superdoc.api.BLL.IRepositories.IUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoctorProfileServiceTest {

    @Mock
    private IDoctorProfileRepository doctorProfileRepository;

    @Mock
    private IUserRepository userRepository;

    @InjectMocks
    private DoctorProfileService service;

    @Test
    void createProfile_newProfile_createsAndSaves() {
        // Arrange
        String email = "doctor@example.com";
        CreateDoctorProfileRequest request = doctorCreateRequest("John", "Doe");
        
        User user = User.builder()
                .id(1L)
                .email(email)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(doctorProfileRepository.findAll()).thenReturn(List.of()); // No existing profile
        when(doctorProfileRepository.save(any(Doctor.class))).thenAnswer(invocation -> {
            Doctor d = invocation.getArgument(0);
            d.setId(1L);
            return d;
        });

        // Act
        DoctorProfileResponse result = service.createProfile(email, request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.firstName()).isEqualTo("John");
        assertThat(result.lastName()).isEqualTo("Doe");
        
        ArgumentCaptor<Doctor> captor = ArgumentCaptor.forClass(Doctor.class);
        verify(doctorProfileRepository).save(captor.capture());
        Doctor captured = captor.getValue();
        assertThat(captured.getUser()).isEqualTo(user);
        assertThat(captured.getFirstName()).isEqualTo("John");
        assertThat(captured.getLastName()).isEqualTo("Doe");
        assertThat(captured.getSpecialization()).isEqualTo(request.specialization());
    }

    @Test
    void updateProfile_existingProfile_updatesAndSaves() {
        // Arrange
        String email = "doctor@example.com";
        UpdateDoctorProfileRequest request = doctorUpdateRequest("Jane", "Smith");
        
        User user = User.builder()
                .id(1L)
                .email(email)
                .build();

        Doctor existingProfile = Doctor.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .user(user)
                .specialization("Old specialization")
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(doctorProfileRepository.findAll()).thenReturn(List.of(existingProfile)); // Existing profile
        when(doctorProfileRepository.save(any(Doctor.class))).thenReturn(existingProfile);

        // Act
        DoctorProfileResponse result = service.updateProfile(email, request);

        // Assert
        assertThat(result).isNotNull();
        verify(doctorProfileRepository).save(argThat(d -> 
            d.getFirstName().equals("Jane") &&
            d.getLastName().equals("Smith")
        ));
    }

    @Test
    void createProfile_userNotFound_throws() {
        // Arrange
        String email = "nonexistent@example.com";
        CreateDoctorProfileRequest request = doctorCreateRequest("John", "Doe");
        
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.createProfile(email, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
        
        verify(doctorProfileRepository, never()).save(any());
    }

    @Test
    void getProfileByEmail_existingProfile_returnsProfile() {
        // Arrange
        String email = "doctor@example.com";
        
        User user = User.builder()
                .id(1L)
                .email(email)
                .build();

        Doctor profile = Doctor.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .user(user)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(doctorProfileRepository.findAll()).thenReturn(List.of(profile));

        // Act
        DoctorProfileResponse result = service.getProfileByEmail(email);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.firstName()).isEqualTo("John");
        assertThat(result.lastName()).isEqualTo("Doe");
        verify(userRepository).findByEmail(email);
    }

    @Test
    void getProfileByEmail_userNotFound_throws() {
        // Arrange
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.getProfileByEmail(email))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
        
        verify(userRepository).findByEmail(email);
    }

    @Test
    void getProfileByEmail_profileNotFound_throws() {
        // Arrange
        String email = "patient@example.com";
        
        User user = User.builder()
                .id(1L)
                .email(email)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(doctorProfileRepository.findAll()).thenReturn(List.of()); // No profile

        // Act & Assert
        assertThatThrownBy(() -> service.getProfileByEmail(email))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Doctor profile not found");
        
        verify(userRepository).findByEmail(email);
    }

    @Test
    void hasProfile_userWithProfile_returnsTrue() {
        // Arrange
        String email = "doctor@example.com";
        
        User user = User.builder()
                .id(1L)
                .email(email)
                .build();

        Doctor profile = Doctor.builder()
                .id(1L)
                .user(user)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(doctorProfileRepository.findAll()).thenReturn(List.of(profile));

        // Act
        boolean result = service.hasProfile(email);

        // Assert
        assertThat(result).isTrue();
        verify(userRepository).findByEmail(email);
    }

    @Test
    void hasProfile_userWithoutProfile_returnsFalse() {
        // Arrange
        String email = "newdoctor@example.com";
        
        User user = User.builder()
                .id(1L)
                .email(email)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(doctorProfileRepository.findAll()).thenReturn(List.of()); // No profile

        // Act
        boolean result = service.hasProfile(email);

        // Assert
        assertThat(result).isFalse();
        verify(userRepository).findByEmail(email);
    }

    @Test
    void hasProfile_userNotFound_throws() {
        // Arrange
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.hasProfile(email))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
        
        verify(userRepository).findByEmail(email);
    }

    private CreateDoctorProfileRequest doctorCreateRequest(String firstName, String lastName) {
        return new CreateDoctorProfileRequest(
                firstName,
                lastName,
                "Cardiology",
                "Experienced cardiologist",
                "DOC-12345",
                "City Clinic",
                12,
                "https://cdn.superdoc.test/" + firstName.toLowerCase() + ".jpg",
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                false
        );
    }

    private UpdateDoctorProfileRequest doctorUpdateRequest(String firstName, String lastName) {
        return new UpdateDoctorProfileRequest(
                firstName,
                lastName,
                "Cardiology",
                "Experienced cardiologist",
                "DOC-12345",
                "City Clinic",
                12,
                "https://cdn.superdoc.test/" + firstName.toLowerCase() + ".jpg",
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                false
        );
    }
}

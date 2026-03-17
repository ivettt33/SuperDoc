package com.superdoc.api.service;

import com.superdoc.api.enumerate.AppointmentStatus;
import com.superdoc.api.model.dto.AppointmentDtos.*;
import com.superdoc.api.BLL.domain.Appointment;
import com.superdoc.api.BLL.domain.Doctor;
import com.superdoc.api.BLL.domain.Patient;
import com.superdoc.api.BLL.domain.User;
import com.superdoc.api.BLL.IRepositories.IAppointmentRepository;
import com.superdoc.api.BLL.IRepositories.IDoctorProfileRepository;
import com.superdoc.api.BLL.IRepositories.IPatientProfileRepository;
import com.superdoc.api.BLL.IRepositories.IUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private IAppointmentRepository appointmentRepository;

    @Mock
    private IDoctorProfileRepository doctorProfileRepository;

    @Mock
    private IPatientProfileRepository patientProfileRepository;

    @Mock
    private IUserRepository userRepository;

    @InjectMocks
    private AppointmentService appointmentService;

    private User patientUser;
    private Patient patientProfile;
    private Doctor doctorProfile;
    private Appointment existingAppointment;

    @BeforeEach
    void setUp() {
        // Setup patient user and profile
        patientUser = User.builder()
                .id(1L)
                .email("patient@test.com")
                .build();

        patientProfile = Patient.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .user(patientUser)
                .build();

        // Setup doctor profile
        User doctorUser = User.builder()
                .id(2L)
                .email("doctor@test.com")
                .build();

        doctorProfile = Doctor.builder()
                .id(1L)
                .firstName("Dr. Jane")
                .lastName("Smith")
                .specialization("Cardiology")
                .openingHours(LocalTime.of(9, 0))
                .closingHours(LocalTime.of(17, 0))
                .isAbsent(false)
                .user(doctorUser)
                .build();

        // Setup existing appointment for conflict tests
        existingAppointment = Appointment.builder()
                .id(1L)
                .doctor(doctorProfile)
                .patient(patientProfile)
                .appointmentDateTime(LocalDateTime.of(2024, 12, 25, 10, 0))
                .status(AppointmentStatus.SCHEDULED)
                .build();
    }

    @Test
    void createAppointment_validRequest_createsAppointment() {
        // Arrange
        LocalDateTime futureDateTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                1L,
                futureDateTime,
                "Regular checkup"
        );

        when(userRepository.findByEmail("patient@test.com")).thenReturn(Optional.of(patientUser));
        when(patientProfileRepository.findAll()).thenReturn(List.of(patientProfile));
        when(doctorProfileRepository.findById(1L)).thenReturn(Optional.of(doctorProfile));
        when(patientProfileRepository.findAll()).thenReturn(List.of(patientProfile));
        when(appointmentRepository.findByDoctorIdAndAppointmentDateTimeAndStatusNot(
                eq(1L), eq(futureDateTime), any(AppointmentStatus.class)))
                .thenReturn(Optional.empty());
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> {
            Appointment apt = invocation.getArgument(0);
            apt.setId(100L);
            return apt;
        });

        // Act
        AppointmentResponse response = appointmentService.createAppointment("patient@test.com", request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.doctorId()).isEqualTo(1L);
        assertThat(response.patientId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(response.notes()).isEqualTo("Regular checkup");

        verify(appointmentRepository).save(argThat(apt ->
                apt.getDoctor() != null &&
                apt.getPatient() != null &&
                apt.getAppointmentDateTime().equals(futureDateTime) &&
                apt.getStatus() == AppointmentStatus.SCHEDULED &&
                apt.getNotes().equals("Regular checkup")
        ));
    }

    @Test
    void createAppointment_patientNotFound_throwsException() {
        // Arrange
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                1L,
                LocalDateTime.now().plusDays(1),
                null
        );

        when(userRepository.findByEmail("patient@test.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.createAppointment("patient@test.com", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Patient not found");

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void createAppointment_patientProfileNotFound_throwsException() {
        // Arrange
        when(patientProfileRepository.findAll()).thenReturn(List.of()); // No profile
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                1L,
                LocalDateTime.now().plusDays(1),
                null
        );

        when(userRepository.findByEmail("patient@test.com")).thenReturn(Optional.of(patientUser));

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.createAppointment("patient@test.com", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Patient profile not found");

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void createAppointment_doctorNotFound_throwsException() {
        // Arrange
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                999L,
                LocalDateTime.now().plusDays(1),
                null
        );

        when(userRepository.findByEmail("patient@test.com")).thenReturn(Optional.of(patientUser));
        when(patientProfileRepository.findAll()).thenReturn(List.of(patientProfile));
        when(doctorProfileRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.createAppointment("patient@test.com", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Doctor not found");

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void createAppointment_pastDateTime_throwsException() {
        // Arrange
        LocalDateTime pastDateTime = LocalDateTime.now().minusDays(1);
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                1L,
                pastDateTime,
                null
        );

        when(userRepository.findByEmail("patient@test.com")).thenReturn(Optional.of(patientUser));
        when(patientProfileRepository.findAll()).thenReturn(List.of(patientProfile));
        when(doctorProfileRepository.findById(1L)).thenReturn(Optional.of(doctorProfile));

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.createAppointment("patient@test.com", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Appointment time must be in the future");

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void createAppointment_conflictingAppointment_throwsException() {
        // Arrange
        LocalDateTime conflictingDateTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        CreateAppointmentRequest request = new CreateAppointmentRequest(
                1L,
                conflictingDateTime,
                null
        );

        Appointment conflicting = Appointment.builder()
                .id(2L)
                .doctor(doctorProfile)
                .patient(patientProfile)
                .appointmentDateTime(conflictingDateTime)
                .status(AppointmentStatus.SCHEDULED)
                .build();

        when(userRepository.findByEmail("patient@test.com")).thenReturn(Optional.of(patientUser));
        when(patientProfileRepository.findAll()).thenReturn(List.of(patientProfile));
        when(doctorProfileRepository.findById(1L)).thenReturn(Optional.of(doctorProfile));
        when(appointmentRepository.findByDoctorIdAndAppointmentDateTimeAndStatusNot(
                eq(1L), eq(conflictingDateTime), any(AppointmentStatus.class)))
                .thenReturn(Optional.of(conflicting));

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.createAppointment("patient@test.com", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Doctor already has an appointment at this time");

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void updateAppointment_validUpdate_updatesAppointment() {
        // Arrange
        Long appointmentId = 100L;
        LocalDateTime newDateTime = LocalDateTime.now().plusDays(2).withHour(14).withMinute(0);
        UpdateAppointmentRequest request = new UpdateAppointmentRequest(
                newDateTime,
                AppointmentStatus.CONFIRMED,
                "Updated notes"
        );

        User doctorUser = User.builder()
                .id(2L)
                .email("doctor@test.com")
                .build();
        doctorProfile = Doctor.builder()
                .id(doctorProfile.getId())
                .user(doctorUser)
                .build();

        existingAppointment.setId(appointmentId);

        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(existingAppointment));
        when(userRepository.findByEmail("doctor@test.com")).thenReturn(Optional.of(doctorUser));
        when(doctorProfileRepository.findAll()).thenReturn(List.of(doctorProfile));
        when(appointmentRepository.findByDoctorIdAndAppointmentDateTimeAndStatusNot(
                eq(1L), eq(newDateTime), any(AppointmentStatus.class)))
                .thenReturn(Optional.empty());
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        AppointmentResponse response = appointmentService.updateAppointment(
                appointmentId, "doctor@test.com", request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(AppointmentStatus.CONFIRMED);
        assertThat(response.notes()).isEqualTo("Updated notes");

        verify(appointmentRepository).save(argThat(apt ->
                apt.getAppointmentDateTime().equals(newDateTime) &&
                apt.getStatus() == AppointmentStatus.CONFIRMED &&
                apt.getNotes().equals("Updated notes")
        ));
    }

    @Test
    void updateAppointment_unauthorizedUser_throwsException() {
        // Arrange
        Long appointmentId = 100L;
        UpdateAppointmentRequest request = new UpdateAppointmentRequest(
                null,
                AppointmentStatus.CONFIRMED,
                null
        );

        User unauthorizedUser = User.builder()
                .id(99L)
                .email("unauthorized@test.com")
                .build();

        existingAppointment.setId(appointmentId);

        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(existingAppointment));
        when(userRepository.findByEmail("unauthorized@test.com"))
                .thenReturn(Optional.of(unauthorizedUser));

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.updateAppointment(
                appointmentId, "unauthorized@test.com", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("You don't have permission");

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void cancelAppointment_validRequest_cancelsAppointment() {
        // Arrange
        Long appointmentId = 100L;
        existingAppointment.setId(appointmentId);
        existingAppointment.setAppointmentDateTime(LocalDateTime.now().plusDays(1)); // Future appointment

        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(existingAppointment));
        when(userRepository.findByEmail("patient@test.com"))
                .thenReturn(Optional.of(patientUser));
        when(patientProfileRepository.findAll()).thenReturn(List.of(patientProfile));
        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        appointmentService.cancelAppointment(appointmentId, "patient@test.com");

        // Assert
        verify(appointmentRepository).save(argThat(apt ->
                apt.getStatus() == AppointmentStatus.CANCELLED
        ));
    }

    @Test
    void cancelAppointment_pastAppointment_throwsException() {
        // Arrange
        Long appointmentId = 100L;
        existingAppointment.setId(appointmentId);
        existingAppointment.setAppointmentDateTime(LocalDateTime.now().minusDays(1)); // Past appointment

        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(existingAppointment));

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.cancelAppointment(appointmentId, "patient@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot cancel an appointment that has already passed");

        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void cancelAppointment_alreadyCancelled_throwsException() {
        // Arrange
        Long appointmentId = 100L;
        existingAppointment.setId(appointmentId);
        existingAppointment.setAppointmentDateTime(LocalDateTime.now().plusDays(1)); // Future appointment
        existingAppointment.setStatus(AppointmentStatus.CANCELLED); // Already cancelled

        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(existingAppointment));

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.cancelAppointment(appointmentId, "patient@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Appointment is already cancelled");

        verify(appointmentRepository, never()).save(any());
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void getMyAppointments_patientUser_returnsPatientAppointments() {
        // Arrange
        List<Appointment> appointments = List.of(existingAppointment);

        when(userRepository.findByEmail("patient@test.com"))
                .thenReturn(Optional.of(patientUser));
        when(patientProfileRepository.findAll()).thenReturn(List.of(patientProfile));
        when(appointmentRepository.findByPatient_Id(1L))
                .thenReturn(appointments);

        // Act
        List<AppointmentResponse> responses = appointmentService.getMyAppointments("patient@test.com");

        // Assert
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).patientId()).isEqualTo(1L);
        assertThat(responses.get(0).doctorId()).isEqualTo(1L);
    }

    @Test
    void getMyAppointments_doctorUser_returnsDoctorAppointments() {
        // Arrange
        User doctorUser = User.builder()
                .id(2L)
                .email("doctor@test.com")
                .build();
        doctorProfile = Doctor.builder()
                .id(doctorProfile.getId())
                .user(doctorUser)
                .build();

        List<Appointment> appointments = List.of(existingAppointment);

        when(userRepository.findByEmail("doctor@test.com"))
                .thenReturn(Optional.of(doctorUser));
        when(doctorProfileRepository.findAll()).thenReturn(List.of(doctorProfile));
        when(appointmentRepository.findByDoctor_Id(1L))
                .thenReturn(appointments);

        // Act
        List<AppointmentResponse> responses = appointmentService.getMyAppointments("doctor@test.com");

        // Assert
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).doctorId()).isEqualTo(1L);
    }

    @Test
    void getMyAppointments_userNotOnboarded_returnsEmptyList() {
        // Arrange
        User userWithoutProfile = User.builder()
                .id(3L)
                .email("newuser@test.com")
                .build();

        when(userRepository.findByEmail("newuser@test.com"))
                .thenReturn(Optional.of(userWithoutProfile));
        when(doctorProfileRepository.findAll()).thenReturn(List.of());
        when(patientProfileRepository.findAll()).thenReturn(List.of());

        // Act
        List<AppointmentResponse> responses = appointmentService.getMyAppointments("newuser@test.com");

        // Assert
        assertThat(responses).isEmpty();
        verify(appointmentRepository, never()).findByPatient_Id(any());
        verify(appointmentRepository, never()).findByDoctor_Id(any());
    }

    @Test
    void getAvailableTimeSlots_validDate_returnsAvailableSlots() {
        // Arrange
        LocalDate date = LocalDate.now().plusDays(1);
        List<Appointment> existingAppointments = List.of();

        when(doctorProfileRepository.findById(1L)).thenReturn(Optional.of(doctorProfile));
        when(appointmentRepository.findByDoctor_IdAndAppointmentDateTimeBetween(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(existingAppointments);

        // Act
        List<AvailableTimeSlot> slots = appointmentService.getAvailableTimeSlots(1L, date);

        // Assert
        assertThat(slots).isNotEmpty();
        assertThat(slots).anyMatch(slot -> slot.available());
    }

    @Test
    void getAvailableTimeSlots_doctorAbsent_returnsUnavailableSlots() {
        // Arrange
        LocalDate date = LocalDate.now().plusDays(1);
        doctorProfile.setIsAbsent(true);

        when(doctorProfileRepository.findById(1L)).thenReturn(Optional.of(doctorProfile));
        when(appointmentRepository.findByDoctor_IdAndAppointmentDateTimeBetween(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        // Act
        List<AvailableTimeSlot> slots = appointmentService.getAvailableTimeSlots(1L, date);

        // Assert
        assertThat(slots).isNotEmpty();
        assertThat(slots).noneMatch(slot -> slot.available());
    }

    @Test
    void getAvailableTimeSlots_bookedSlots_markedAsUnavailable() {
        // Arrange
        LocalDate date = LocalDate.now().plusDays(1);
        LocalTime bookedTime = LocalTime.of(10, 0);
        Appointment bookedAppointment = new Appointment();
        bookedAppointment.setAppointmentDateTime(LocalDateTime.of(date, bookedTime));
        bookedAppointment.setStatus(AppointmentStatus.SCHEDULED);

        when(doctorProfileRepository.findById(1L)).thenReturn(Optional.of(doctorProfile));
        when(appointmentRepository.findByDoctor_IdAndAppointmentDateTimeBetween(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(bookedAppointment));

        // Act
        List<AvailableTimeSlot> slots = appointmentService.getAvailableTimeSlots(1L, date);

        // Assert
        assertThat(slots).isNotEmpty();
        assertThat(slots).anyMatch(slot -> 
                slot.time().equals("10:00") && !slot.available()
        );
    }

    @Test
    void getAvailableTimeSlots_cancelledAppointment_doesNotBlockSlot() {
        // Arrange
        LocalDate date = LocalDate.now().plusDays(1);
        LocalTime cancelledTime = LocalTime.of(11, 0);
        Appointment cancelledAppointment = new Appointment();
        cancelledAppointment.setAppointmentDateTime(LocalDateTime.of(date, cancelledTime));
        cancelledAppointment.setStatus(AppointmentStatus.CANCELLED);

        when(doctorProfileRepository.findById(1L)).thenReturn(Optional.of(doctorProfile));
        when(appointmentRepository.findByDoctor_IdAndAppointmentDateTimeBetween(
                eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(cancelledAppointment));

        // Act
        List<AvailableTimeSlot> slots = appointmentService.getAvailableTimeSlots(1L, date);

        // Assert
        assertThat(slots).isNotEmpty();
        // The cancelled appointment should not block the slot
        assertThat(slots).anyMatch(slot -> 
                slot.time().equals("11:00") && slot.available()
        );
    }

    @Test
    void getAppointment_validRequest_returnsAppointment() {
        // Arrange
        Long appointmentId = 100L;
        existingAppointment.setId(appointmentId);

        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(existingAppointment));
        when(userRepository.findByEmail("patient@test.com"))
                .thenReturn(Optional.of(patientUser));
        when(patientProfileRepository.findAll()).thenReturn(List.of(patientProfile));
        when(doctorProfileRepository.findAll()).thenReturn(List.of(doctorProfile));

        // Act
        AppointmentResponse response = appointmentService.getAppointment(appointmentId, "patient@test.com");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.doctorId()).isEqualTo(1L);
        assertThat(response.patientId()).isEqualTo(1L);
    }

    @Test
    void getAppointment_unauthorizedUser_throwsException() {
        // Arrange
        Long appointmentId = 100L;
        existingAppointment.setId(appointmentId);

        User unauthorizedUser = User.builder()
                .id(99L)
                .email("unauthorized@test.com")
                .build();

        when(appointmentRepository.findById(appointmentId))
                .thenReturn(Optional.of(existingAppointment));
        when(userRepository.findByEmail("unauthorized@test.com"))
                .thenReturn(Optional.of(unauthorizedUser));

        // Act & Assert
        assertThatThrownBy(() -> appointmentService.getAppointment(appointmentId, "unauthorized@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("You don't have permission");
    }
}


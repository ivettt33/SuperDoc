package com.superdoc.api.service;

import com.superdoc.api.BLL.domain.Appointment;
import com.superdoc.api.BLL.domain.Doctor;
import com.superdoc.api.BLL.domain.Patient;
import com.superdoc.api.BLL.domain.User;
import com.superdoc.api.enumerate.AppointmentStatus;
import com.superdoc.api.model.dto.AppointmentDtos.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.superdoc.api.BLL.IRepositories.IAppointmentRepository;
import com.superdoc.api.BLL.IRepositories.IDoctorProfileRepository;
import com.superdoc.api.BLL.IRepositories.IPatientProfileRepository;
import com.superdoc.api.BLL.IRepositories.IUserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AppointmentService {

    private final IAppointmentRepository appointmentRepository;
    private final IDoctorProfileRepository doctorProfileRepository;
    private final IPatientProfileRepository patientProfileRepository;
    private final IUserRepository userRepository;

    public AppointmentResponse createAppointment(String patientEmail, CreateAppointmentRequest request) {
        // Get patient from email
        User patientUser = userRepository.findByEmail(patientEmail)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found"));
        
        Patient patient = patientProfileRepository.findAll().stream()
                .filter(pat -> pat.getUser() != null && pat.getUser().getId().equals(patientUser.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Patient profile not found"));

        // Get doctor
        Doctor doctor = doctorProfileRepository.findById(request.doctorId())
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));

        // Validate appointment time is in the future
        if (request.appointmentDateTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Appointment time must be in the future");
        }

        // Check for conflicts (same doctor, same time, not cancelled)
        appointmentRepository.findByDoctorIdAndAppointmentDateTimeAndStatusNot(
                doctor.getId(), 
                request.appointmentDateTime(), 
                AppointmentStatus.CANCELLED
        ).ifPresent(existing -> {
            throw new IllegalArgumentException("Doctor already has an appointment at this time");
        });

        // Create appointment
        Appointment appointment = Appointment.builder()
                .doctor(doctor)
                .patient(patient)
                .appointmentDateTime(request.appointmentDateTime())
                .status(AppointmentStatus.SCHEDULED)
                .notes(request.notes())
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .build();

        Appointment saved = appointmentRepository.save(appointment);
        return toResponse(saved);
    }

    public AppointmentResponse updateAppointment(Long appointmentId, String userEmail, UpdateAppointmentRequest request) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        // Verify user has permission (either doctor or patient of this appointment)
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        Doctor userDoctor = doctorProfileRepository.findAll().stream()
                .filter(doc -> doc.getUser() != null && doc.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElse(null);
        boolean isDoctor = userDoctor != null && userDoctor.getId().equals(appointment.getDoctor().getId());
        
        Patient userPatient = patientProfileRepository.findAll().stream()
                .filter(pat -> pat.getUser() != null && pat.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElse(null);
        boolean isPatient = userPatient != null && userPatient.getId().equals(appointment.getPatient().getId());

        if (!isDoctor && !isPatient) {
            throw new IllegalArgumentException("You don't have permission to update this appointment");
        }

        // Update fields
        if (request.appointmentDateTime() != null) {
            if (request.appointmentDateTime().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Appointment time must be in the future");
            }
            
            // Check for conflicts (excluding current appointment)
            appointmentRepository.findByDoctorIdAndAppointmentDateTimeAndStatusNot(
                appointment.getDoctor().getId(), 
                request.appointmentDateTime(),
                AppointmentStatus.CANCELLED
            ).ifPresent(existing -> {
                if (!existing.getId().equals(appointmentId)) {
                    throw new IllegalArgumentException("Doctor already has an appointment at this time");
                }
            });
            
            appointment.setAppointmentDateTime(request.appointmentDateTime());
        }

        if (request.status() != null) {
            appointment.setStatus(request.status());
        }

        if (request.notes() != null) {
            appointment.setNotes(request.notes());
        }

        Appointment updated = appointmentRepository.save(appointment);
        return toResponse(updated);
    }

    public List<AppointmentResponse> getMyAppointments(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Doctor userDoctor = doctorProfileRepository.findAll().stream()
                .filter(doc -> doc.getUser() != null && doc.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElse(null);
        
        Patient userPatient = patientProfileRepository.findAll().stream()
                .filter(pat -> pat.getUser() != null && pat.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElse(null);

        // If user hasn't completed onboarding, return empty list
        if (userDoctor == null && userPatient == null) {
            return List.of();
        }

        List<Appointment> appointments;
        if (userDoctor != null) {
            appointments = appointmentRepository.findByDoctor_Id(userDoctor.getId());
        } else if (userPatient != null) {
            appointments = appointmentRepository.findByPatient_Id(userPatient.getId());
        } else {
            return List.of();
        }

        return appointments.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public AppointmentResponse getAppointment(Long appointmentId, String userEmail) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        // Verify user has permission
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        Doctor userDoctor = doctorProfileRepository.findAll().stream()
                .filter(doc -> doc.getUser() != null && doc.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElse(null);
        boolean isDoctor = userDoctor != null && userDoctor.getId().equals(appointment.getDoctor().getId());
        
        Patient userPatient = patientProfileRepository.findAll().stream()
                .filter(pat -> pat.getUser() != null && pat.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElse(null);
        boolean isPatient = userPatient != null && userPatient.getId().equals(appointment.getPatient().getId());

        if (!isDoctor && !isPatient) {
            throw new IllegalArgumentException("You don't have permission to view this appointment");
        }

        return toResponse(appointment);
    }

    public void cancelAppointment(Long appointmentId, String userEmail) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        // Check if the appointment has already passed
        if (appointment.getAppointmentDateTime().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Cannot cancel an appointment that has already passed");
        }

        // Check if appointment is already cancelled
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new IllegalArgumentException("Appointment is already cancelled");
        }

        // Verify user has permission
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        Doctor userDoctor = doctorProfileRepository.findAll().stream()
                .filter(doc -> doc.getUser() != null && doc.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElse(null);
        boolean isDoctor = userDoctor != null && userDoctor.getId().equals(appointment.getDoctor().getId());
        
        Patient userPatient = patientProfileRepository.findAll().stream()
                .filter(pat -> pat.getUser() != null && pat.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElse(null);
        boolean isPatient = userPatient != null && userPatient.getId().equals(appointment.getPatient().getId());

        if (!isDoctor && !isPatient) {
            throw new IllegalArgumentException("You don't have permission to cancel this appointment");
        }

        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
    }

    public List<AvailableTimeSlot> getAvailableTimeSlots(Long doctorId, LocalDate date) {
        Doctor doctor = doctorProfileRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.atTime(23, 59, 59);
        
        List<Appointment> existingAppointments = appointmentRepository.findByDoctor_IdAndAppointmentDateTimeBetween(
            doctorId, dayStart, dayEnd
        );

        Set<LocalTime> bookedTimes = existingAppointments.stream()
            .filter(apt -> apt.getStatus() != AppointmentStatus.CANCELLED)
            .map(apt -> apt.getAppointmentDateTime().toLocalTime())
            .collect(Collectors.toSet());

        List<AvailableTimeSlot> slots = new ArrayList<>();
        LocalTime startTime = doctor.getOpeningHours() != null ? doctor.getOpeningHours() : LocalTime.of(9, 0);
        LocalTime endTime = doctor.getClosingHours() != null ? doctor.getClosingHours() : LocalTime.of(17, 0);
        LocalTime currentTime = startTime;
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        while (currentTime.isBefore(endTime) || currentTime.equals(endTime)) {
            String timeString = currentTime.format(timeFormatter);
            boolean available = !doctor.getIsAbsent() && 
                               !bookedTimes.contains(currentTime) && 
                               !isTimeInPast(date, currentTime);
            
            slots.add(new AvailableTimeSlot(timeString, available));
            currentTime = currentTime.plusMinutes(30);
        }

        return slots;
    }

    private boolean isTimeInPast(LocalDate date, LocalTime time) {
        LocalDateTime dateTime = LocalDateTime.of(date, time);
        return dateTime.isBefore(LocalDateTime.now());
    }

    private AppointmentResponse toResponse(Appointment appointment) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getDoctor().getId(),
                appointment.getDoctor().getFirstName() + " " + appointment.getDoctor().getLastName(),
                appointment.getDoctor().getSpecialization(),
                appointment.getPatient().getId(),
                appointment.getPatient().getFirstName() + " " + appointment.getPatient().getLastName(),
                appointment.getAppointmentDateTime(),
                appointment.getStatus(),
                appointment.getNotes(),
                appointment.getCreatedAt(),
                appointment.getUpdatedAt()
        );
    }
}


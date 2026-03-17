package com.superdoc.api.model.dto;

import com.superdoc.api.enumerate.AppointmentStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public class AppointmentDtos {
    
    public record CreateAppointmentRequest(
        @NotNull Long doctorId,
        @NotNull @Future LocalDateTime appointmentDateTime,
        String notes
    ) {}
    
    public record UpdateAppointmentRequest(
        LocalDateTime appointmentDateTime,
        AppointmentStatus status,
        String notes
    ) {}
    
    public record AppointmentResponse(
        Long id,
        Long doctorId,
        String doctorName,
        String doctorSpecialization,
        Long patientId,
        String patientName,
        LocalDateTime appointmentDateTime,
        AppointmentStatus status,
        String notes,
        java.time.Instant createdAt,
        java.time.Instant updatedAt
    ) {}
    
    public record AvailableTimeSlot(
        String time,
        boolean available
    ) {}
}



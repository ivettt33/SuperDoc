package com.superdoc.api.BLL.domain;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import com.superdoc.api.enumerate.AppointmentStatus;
import java.time.Instant;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Appointment {
    private Long id;
    private Doctor doctor;
    private Patient patient;
    private LocalDateTime appointmentDateTime;
    private AppointmentStatus status;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}

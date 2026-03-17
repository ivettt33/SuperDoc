package com.superdoc.api.model.dto;

import com.superdoc.api.enumerate.PrescriptionStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.Instant;

public class PrescriptionDtos {
    
    public record CreatePrescriptionRequest(
        @NotNull Long patientId,
        @NotBlank @Size(max = 255) String medicationName,
        @NotBlank @Size(max = 100) String dosage,
        @NotBlank @Size(max = 100) String frequency,
        @NotBlank @Size(max = 100) String duration,
        @Size(max = 1000) String instructions,
        @NotNull @Future LocalDate validUntil
    ) {}
    
    public record UpdatePrescriptionRequest(
        @Size(max = 255) String medicationName,
        @Size(max = 100) String dosage,
        @Size(max = 100) String frequency,
        @Size(max = 100) String duration,
        @Size(max = 1000) String instructions,
        @Future LocalDate validUntil
    ) {}
    
    public record PrescriptionResponse(
        Long id,
        Long patientId,
        String patientName,
        Long doctorId,
        String doctorName,
        String medicationName,
        String dosage,
        String frequency,
        String duration,
        String instructions,
        PrescriptionStatus status,
        Instant issuedAt,
        LocalDate validUntil,
        Instant createdAt,
        Instant updatedAt
    ) {}
}


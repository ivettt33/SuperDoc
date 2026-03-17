package com.superdoc.api.BLL.domain;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import com.superdoc.api.enumerate.PrescriptionStatus;
import java.time.Instant;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Prescription {
    private Long id;
    private User patient;
    private User doctor;
    private String medicationName;
    private String dosage;
    private String frequency;
    private String duration;
    private String instructions;
    private PrescriptionStatus status;
    private Instant issuedAt;
    private LocalDate validUntil;
    private Instant createdAt;
    private Instant updatedAt;
}

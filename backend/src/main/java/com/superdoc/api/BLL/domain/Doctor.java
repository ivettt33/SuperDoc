package com.superdoc.api.BLL.domain;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import java.time.Instant;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Doctor {
    private Long id;
    private User user;
    private String firstName;
    private String lastName;
    private String specialization;
    private String bio;
    private String licenseNumber;
    private String clinicName;
    private Integer yearsOfExperience;
    private String location;
    private String profilePhotoUrl;
    private LocalTime openingHours;
    private LocalTime closingHours;
    private Boolean isAbsent;
    private Instant createdAt;
    private Instant updatedAt;
}

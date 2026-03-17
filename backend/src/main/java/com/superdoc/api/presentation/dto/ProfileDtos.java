package com.superdoc.api.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

public class ProfileDtos {
    
    // Patient Profile DTOs
    public record CreatePatientProfileRequest(
        @NotBlank @Size(max = 255) String firstName,
        @NotBlank @Size(max = 255) String lastName,
        @NotNull @Past LocalDate dateOfBirth,
        @Size(max = 50) String gender,
        String conditions,
        @Size(max = 255) String insuranceNumber,
        @Size(max = 500) String profilePicture
    ) {}
    
    public record UpdatePatientProfileRequest(
        @Size(max = 255) String firstName,
        @Size(max = 255) String lastName,
        @Past LocalDate dateOfBirth,
        @Size(max = 50) String gender,
        String conditions,
        @Size(max = 255) String insuranceNumber,
        @Size(max = 500) String profilePicture
    ) {}
    
    public record PatientProfileResponse(
        Long id,
        Long userId,
        String email,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        String gender,
        String conditions,
        String insuranceNumber,
        String profilePicture,
        Instant createdAt,
        Instant updatedAt
    ) {}
    
    // Doctor Profile DTOs
    public record CreateDoctorProfileRequest(
        @NotBlank @Size(max = 255) String firstName,
        @NotBlank @Size(max = 255) String lastName,
        @NotBlank @Size(max = 255) String specialization,
        String bio,
        @NotBlank @Size(max = 255) String licenseNumber,
        @NotBlank @Size(max = 255) String clinicName,
        @NotNull @Min(0) @Max(50) Integer yearsOfExperience,
        @Size(max = 500) String profilePhotoUrl,
        LocalTime openingHours,
        LocalTime closingHours,
        Boolean isAbsent
    ) {}
    
    public record UpdateDoctorProfileRequest(
        @Size(max = 255) String firstName,
        @Size(max = 255) String lastName,
        @Size(max = 255) String specialization,
        String bio,
        @Size(max = 255) String licenseNumber,
        @Size(max = 255) String clinicName,
        @Min(0) @Max(50) Integer yearsOfExperience,
        @Size(max = 500) String profilePhotoUrl,
        LocalTime openingHours,
        LocalTime closingHours,
        Boolean isAbsent
    ) {}
    
    public record DoctorProfileResponse(
        Long id,
        Long userId,
        String email,
        String firstName,
        String lastName,
        String specialization,
        String bio,
        String licenseNumber,
        String clinicName,
        Integer yearsOfExperience,
        String location,
        String profilePhotoUrl,
        LocalTime openingHours,
        LocalTime closingHours,
        Boolean isAbsent,
        Instant createdAt,
        Instant updatedAt
    ) {}
    
    public record DoctorListResponse(
        Long id,
        String firstName,
        String lastName,
        String specialization,
        String clinicName,
        String profilePhotoUrl
    ) {}
    
    public record PatientListResponse(
        Long profileId,
        String firstName,
        String lastName,
        String email
    ) {}
}


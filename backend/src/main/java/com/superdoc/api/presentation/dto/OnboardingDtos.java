package com.superdoc.api.model.dto;

import com.superdoc.api.enumerate.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.time.LocalDate;
import java.time.LocalTime;

public class OnboardingDtos {
    public record RoleSelectionRequest(@NotNull Role role) {}
    
    public record DoctorOnboardRequest(@NotBlank String firstName,
                                       @NotBlank String lastName,
                                       @NotBlank String specialization,
                                       String bio,
                                       @NotBlank String licenseNumber,
                                       @NotBlank String clinicName,
                                       @NotNull @Min(0) @Max(50) Integer yearsOfExperience,
                                       String profilePhotoUrl,
                                       LocalTime openingHours,
                                       LocalTime closingHours,
                                       Boolean isAbsent) {}
    
    public record PatientOnboardRequest(@NotBlank String firstName,
                                        @NotBlank String lastName,
                                        @NotNull LocalDate dateOfBirth,
                                        String gender,
                                        String conditions,
                                        String insuranceNumber,
                                        String profilePicture) {}
    
    public record DoctorProfileSummary(String specialization, String bio, String licenseNumber, 
                                       String clinicName, Integer yearsOfExperience, String profilePhotoUrl) {}
    
    public record PatientProfileSummary(String firstName, String lastName, LocalDate dateOfBirth, 
                                        String gender, String conditions, String insuranceNumber, 
                                        String profilePicture) {}
    
    public record OnboardingSummaryResponse(String firstName, String lastName, Role role, 
                                           DoctorProfileSummary doctorProfile, 
                                           PatientProfileSummary patientProfile) {}
}

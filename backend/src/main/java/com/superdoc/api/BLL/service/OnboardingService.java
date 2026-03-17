package com.superdoc.api.service;

import com.superdoc.api.BLL.domain.User;
import com.superdoc.api.BLL.domain.Doctor;
import com.superdoc.api.BLL.domain.Patient;
import com.superdoc.api.enumerate.Role;
import com.superdoc.api.model.dto.OnboardingDtos.*;
import com.superdoc.api.model.dto.ProfileDtos.CreateDoctorProfileRequest;
import com.superdoc.api.model.dto.ProfileDtos.CreatePatientProfileRequest;
import com.superdoc.api.BLL.IRepositories.IUserRepository;
import com.superdoc.api.BLL.IRepositories.IDoctorProfileRepository;
import com.superdoc.api.BLL.IRepositories.IPatientProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;

@RequiredArgsConstructor
@Service
@Transactional
public class OnboardingService {
    private final IUserRepository users;
    private final IDoctorProfileRepository doctorProfileRepository;
    private final IPatientProfileRepository patientProfileRepository;
    private final DoctorProfileService doctorProfileService;
    private final PatientProfileService patientProfileService;

    public void updateUserRole(String email, RoleSelectionRequest req) {
        var u = users.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        u.setRole(req.role());
        users.save(u);
    }

    public void onboardDoctor(String email, DoctorOnboardRequest req) {
        CreateDoctorProfileRequest createReq = new CreateDoctorProfileRequest(
            req.firstName(),
            req.lastName(),
            req.specialization(),
            req.bio(),
            req.licenseNumber(),
            req.clinicName(),
            req.yearsOfExperience(),
            req.profilePhotoUrl(),
            req.openingHours(),
            req.closingHours(),
            req.isAbsent()
        );
        doctorProfileService.createProfile(email, createReq);
    }


    public void onboardPatient(String email, PatientOnboardRequest req) {
        CreatePatientProfileRequest createReq = new CreatePatientProfileRequest(
            req.firstName(),
            req.lastName(),
            req.dateOfBirth(),
            req.gender(),
            req.conditions(),
            req.insuranceNumber(),
            req.profilePicture()
        );
        patientProfileService.createProfile(email, createReq);
    }

   
    public OnboardingSummaryResponse getOnboardingSummary(String email) {
        var u = users.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        DoctorProfileSummary doctorProfile = null;
        PatientProfileSummary patientProfile = null;
        
        // Find doctor profile for this user
        Doctor doctor = doctorProfileRepository.findAll().stream()
                .filter(doc -> doc.getUser() != null && doc.getUser().getId().equals(u.getId()))
                .findFirst()
                .orElse(null);
        
        if (doctor != null) {
            doctorProfile = new DoctorProfileSummary(
                doctor.getSpecialization(),
                doctor.getBio(),
                doctor.getLicenseNumber(),
                doctor.getClinicName(),
                doctor.getYearsOfExperience(),
                doctor.getProfilePhotoUrl()
            );
        }
        
        // Find patient profile for this user
        Patient patient = patientProfileRepository.findAll().stream()
                .filter(pat -> pat.getUser() != null && pat.getUser().getId().equals(u.getId()))
                .findFirst()
                .orElse(null);
        
        if (patient != null) {
            patientProfile = new PatientProfileSummary(
                patient.getFirstName(),
                patient.getLastName(),
                patient.getDateOfBirth(),
                patient.getGender(),
                patient.getConditions(),
                patient.getInsuranceNumber(),
                patient.getProfilePicture()
            );
        }
        
        return new OnboardingSummaryResponse(
                extractFirstName(u),
                extractLastName(u),
                u.getRole(),
                doctorProfile,
                patientProfile
        );
    }
    

    private String extractFirstName(User user) {
        Doctor doctor = doctorProfileRepository.findAll().stream()
                .filter(doc -> doc.getUser() != null && doc.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElse(null);
        if (doctor != null) {
            return doctor.getFirstName();
        }
        
        Patient patient = patientProfileRepository.findAll().stream()
                .filter(pat -> pat.getUser() != null && pat.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElse(null);
        if (patient != null) {
            return patient.getFirstName();
        }
        return "";
    }
    
   
    private String extractLastName(User user) {
        Doctor doctor = doctorProfileRepository.findAll().stream()
                .filter(doc -> doc.getUser() != null && doc.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElse(null);
        if (doctor != null) {
            return doctor.getLastName();
        }
        
        Patient patient = patientProfileRepository.findAll().stream()
                .filter(pat -> pat.getUser() != null && pat.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElse(null);
        if (patient != null) {
            return patient.getLastName();
        }
        return "";
    }
}

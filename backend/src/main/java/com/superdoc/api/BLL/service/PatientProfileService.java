package com.superdoc.api.service;

import com.superdoc.api.BLL.domain.Patient;
import com.superdoc.api.BLL.domain.User;
import com.superdoc.api.model.dto.ProfileDtos.CreatePatientProfileRequest;
import com.superdoc.api.model.dto.ProfileDtos.UpdatePatientProfileRequest;
import com.superdoc.api.model.dto.ProfileDtos.PatientProfileResponse;
import com.superdoc.api.model.dto.ProfileDtos.PatientListResponse;
import com.superdoc.api.BLL.IRepositories.IPatientProfileRepository;
import com.superdoc.api.BLL.IRepositories.IUserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PatientProfileService {

    private final IPatientProfileRepository patientProfileRepository;
    private final IUserRepository userRepository;

    public PatientProfileResponse createProfile(String email, CreatePatientProfileRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Patient profile = patientProfileRepository.findAll().stream()
                .filter(pat -> pat.getUser() != null && pat.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElse(null);
        
        if (profile == null) {
            profile = Patient.builder()
                    .user(user)
                    .build();
        }

        profile.setFirstName(requireText(req.firstName(), "First name"));
        profile.setLastName(requireText(req.lastName(), "Last name"));
        profile.setDateOfBirth(requireValidDate(req.dateOfBirth()));
        profile.setGender(trimToNull(req.gender()));
        profile.setConditions(trimToNull(req.conditions()));
        profile.setInsuranceNumber(trimToNull(req.insuranceNumber()));
        profile.setProfilePicture(trimToNull(req.profilePicture()));
        
        if (profile.getCreatedAt() == null) {
            profile.setCreatedAt(java.time.Instant.now());
        }
        profile.setUpdatedAt(java.time.Instant.now());

        Patient saved = patientProfileRepository.save(profile);
        return toResponse(saved);
    }
    
    public PatientProfileResponse updateProfile(String email, UpdatePatientProfileRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Patient profile = patientProfileRepository.findAll().stream()
                .filter(pat -> pat.getUser() != null && pat.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Patient profile not found"));

        if (req.firstName() != null) {
            profile.setFirstName(requireText(req.firstName(), "First name"));
        }
        if (req.lastName() != null) {
            profile.setLastName(requireText(req.lastName(), "Last name"));
        }
        if (req.dateOfBirth() != null) {
            profile.setDateOfBirth(requireValidDate(req.dateOfBirth()));
        }
        if (req.gender() != null) {
            profile.setGender(trimToNull(req.gender()));
        }
        if (req.conditions() != null) {
            profile.setConditions(trimToNull(req.conditions()));
        }
        if (req.insuranceNumber() != null) {
            profile.setInsuranceNumber(trimToNull(req.insuranceNumber()));
        }
        if (req.profilePicture() != null) {
            profile.setProfilePicture(trimToNull(req.profilePicture()));
        }
        
        profile.setUpdatedAt(java.time.Instant.now());

        Patient saved = patientProfileRepository.save(profile);
        return toResponse(saved);
    }

    public PatientProfileResponse getProfileByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        Patient profile = patientProfileRepository.findAll().stream()
                .filter(pat -> pat.getUser() != null && pat.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Patient profile not found"));
        
        return toResponse(profile);
    }

    public boolean hasProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return patientProfileRepository.findAll().stream()
                .anyMatch(pat -> pat.getUser() != null && pat.getUser().getId().equals(user.getId()));
    }

    public PatientProfileResponse getProfileById(Long id) {
        Patient profile = patientProfileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Patient profile not found"));
        return toResponse(profile);
    }

    /**
     * Check if a patient profile belongs to the authenticated user
     * Used for authorization checks in @PreAuthorize
     */
    public boolean isOwnProfile(Long profileId, String email) {
        if (email == null) return false;
        return patientProfileRepository.findById(profileId)
                .map(profile -> profile.getUser() != null && profile.getUser().getEmail().equals(email))
                .orElse(false);
    }

    public List<PatientListResponse> getAllPatients() {
        return patientProfileRepository.findAll().stream()
                .filter(profile -> profile.getUser() != null)
                .map(profile -> new PatientListResponse(
                        profile.getId(), // This is the profileId
                        profile.getFirstName() != null ? profile.getFirstName() : "",
                        profile.getLastName() != null ? profile.getLastName() : "",
                        profile.getUser().getEmail()
                ))
                .collect(Collectors.toList());
    }
    
    private PatientProfileResponse toResponse(Patient profile) {
        return new PatientProfileResponse(
                profile.getId(),
                profile.getUser() != null ? profile.getUser().getId() : null,
                profile.getUser() != null ? profile.getUser().getEmail() : null,
                profile.getFirstName(),
                profile.getLastName(),
                profile.getDateOfBirth(),
                profile.getGender(),
                profile.getConditions(),
                profile.getInsuranceNumber(),
                profile.getProfilePicture(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

    private String requireText(String value, String label) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(label + " is required");
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private LocalDate requireValidDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date of birth is required");
        }
        if (date.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Date of birth must be in the past");
        }
        return date;
    }
}


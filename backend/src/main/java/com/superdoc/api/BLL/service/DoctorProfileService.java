package com.superdoc.api.service;

import com.superdoc.api.BLL.domain.Doctor;
import com.superdoc.api.BLL.domain.User;
import com.superdoc.api.model.dto.ProfileDtos.CreateDoctorProfileRequest;
import com.superdoc.api.model.dto.ProfileDtos.UpdateDoctorProfileRequest;
import com.superdoc.api.model.dto.ProfileDtos.DoctorProfileResponse;
import com.superdoc.api.model.dto.ProfileDtos.DoctorListResponse;
import com.superdoc.api.BLL.IRepositories.IDoctorProfileRepository;
import com.superdoc.api.BLL.IRepositories.IUserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DoctorProfileService {

    private final IDoctorProfileRepository doctorProfileRepository;
    private final IUserRepository userRepository;

    public DoctorProfileResponse createProfile(String email, CreateDoctorProfileRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Doctor profile = doctorProfileRepository.findAll().stream()
                .filter(doc -> doc.getUser() != null && doc.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElse(null);
        
        if (profile == null) {
            profile = Doctor.builder()
                    .user(user)
                    .build();
        }

        profile.setFirstName(requireText(req.firstName(), "First name"));
        profile.setLastName(requireText(req.lastName(), "Last name"));
        profile.setSpecialization(requireText(req.specialization(), "Specialization"));
        profile.setBio(trimToNull(req.bio()));
        profile.setLicenseNumber(requireText(req.licenseNumber(), "License number"));
        profile.setClinicName(requireText(req.clinicName(), "Clinic name"));
        profile.setLocation("Unknown Location"); // Default location if not provided
        profile.setYearsOfExperience(requireYearsOfExperience(req.yearsOfExperience()));
        profile.setProfilePhotoUrl(trimToNull(req.profilePhotoUrl()));
        
        if (req.openingHours() != null) {
            profile.setOpeningHours(req.openingHours());
        } else {
            profile.setOpeningHours(LocalTime.of(9, 0));
        }
        
        if (req.closingHours() != null) {
            profile.setClosingHours(req.closingHours());
        } else {
            profile.setClosingHours(LocalTime.of(17, 0));
        }
        
        if (req.isAbsent() != null) {
            profile.setIsAbsent(req.isAbsent());
        } else {
            profile.setIsAbsent(false);
        }
        
        if (profile.getCreatedAt() == null) {
            profile.setCreatedAt(java.time.Instant.now());
        }
        profile.setUpdatedAt(java.time.Instant.now());

        Doctor saved = doctorProfileRepository.save(profile);
        return toResponse(saved);
    }
    
    public DoctorProfileResponse updateProfile(String email, UpdateDoctorProfileRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Doctor profile = doctorProfileRepository.findAll().stream()
                .filter(doc -> doc.getUser() != null && doc.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Doctor profile not found"));

        if (req.firstName() != null) {
            profile.setFirstName(requireText(req.firstName(), "First name"));
        }
        if (req.lastName() != null) {
            profile.setLastName(requireText(req.lastName(), "Last name"));
        }
        if (req.specialization() != null) {
            profile.setSpecialization(requireText(req.specialization(), "Specialization"));
        }
        if (req.bio() != null) {
            profile.setBio(trimToNull(req.bio()));
        }
        if (req.licenseNumber() != null) {
            profile.setLicenseNumber(requireText(req.licenseNumber(), "License number"));
        }
        if (req.clinicName() != null) {
            profile.setClinicName(requireText(req.clinicName(), "Clinic name"));
        }
        if (req.yearsOfExperience() != null) {
            profile.setYearsOfExperience(requireYearsOfExperience(req.yearsOfExperience()));
        }
        if (req.profilePhotoUrl() != null) {
            profile.setProfilePhotoUrl(trimToNull(req.profilePhotoUrl()));
        }
        if (req.openingHours() != null) {
            profile.setOpeningHours(req.openingHours());
        }
        if (req.closingHours() != null) {
            profile.setClosingHours(req.closingHours());
        }
        if (req.isAbsent() != null) {
            profile.setIsAbsent(req.isAbsent());
        }
        
        profile.setUpdatedAt(java.time.Instant.now());

        Doctor saved = doctorProfileRepository.save(profile);
        return toResponse(saved);
    }

    public DoctorProfileResponse getProfileByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        Doctor profile = doctorProfileRepository.findAll().stream()
                .filter(doc -> doc.getUser() != null && doc.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Doctor profile not found"));
        
        return toResponse(profile);
    }

    public boolean hasProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return doctorProfileRepository.findAll().stream()
                .anyMatch(doc -> doc.getUser() != null && doc.getUser().getId().equals(user.getId()));
    }

    public List<DoctorListResponse> getAllDoctors() {
        return doctorProfileRepository.findAll().stream()
                .map(this::toListResponse)
                .collect(java.util.stream.Collectors.toList());
    }
    
    private DoctorProfileResponse toResponse(Doctor doctor) {
        return new DoctorProfileResponse(
                doctor.getId(),
                doctor.getUser() != null ? doctor.getUser().getId() : null,
                doctor.getUser() != null ? doctor.getUser().getEmail() : null,
                doctor.getFirstName(),
                doctor.getLastName(),
                doctor.getSpecialization(),
                doctor.getBio(),
                doctor.getLicenseNumber(),
                doctor.getClinicName(),
                doctor.getYearsOfExperience(),
                doctor.getLocation(),
                doctor.getProfilePhotoUrl(),
                doctor.getOpeningHours(),
                doctor.getClosingHours(),
                doctor.getIsAbsent(),
                doctor.getCreatedAt(),
                doctor.getUpdatedAt()
        );
    }
    
    private DoctorListResponse toListResponse(Doctor doctor) {
        return new DoctorListResponse(
                doctor.getId(),
                doctor.getFirstName(),
                doctor.getLastName(),
                doctor.getSpecialization(),
                doctor.getClinicName(),
                doctor.getProfilePhotoUrl()
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

    private Integer requireYearsOfExperience(Integer years) {
        if (years == null) {
            throw new IllegalArgumentException("Years of experience is required");
        }
        if (years < 0 || years > 50) {
            throw new IllegalArgumentException("Years of experience must be between 0 and 50");
        }
        return years;
    }
}

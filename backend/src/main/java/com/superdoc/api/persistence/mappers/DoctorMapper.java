package com.superdoc.api.persistence.mappers;

import com.superdoc.api.BLL.domain.Doctor;
import com.superdoc.api.BLL.domain.User;
import com.superdoc.api.persistence.entities.DoctorProfileEntity;
import com.superdoc.api.persistence.entities.UserEntity;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DoctorMapper {
    
    private final UserMapper userMapper;
    
    public Doctor toDomain(DoctorProfileEntity entity) {
        if (entity == null) {
            return null;
        }
        User user = entity.getUser() != null ? userMapper.toDomain(entity.getUser()) : null;
        return Doctor.builder()
                .id(entity.getId())
                .user(user)
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .specialization(entity.getSpecialization())
                .bio(entity.getBio())
                .licenseNumber(entity.getLicenseNumber())
                .clinicName(entity.getClinicName())
                .yearsOfExperience(entity.getYearsOfExperience())
                .location(entity.getLocation())
                .profilePhotoUrl(entity.getProfilePhotoUrl())
                .openingHours(entity.getOpeningHours())
                .closingHours(entity.getClosingHours())
                .isAbsent(entity.getIsAbsent())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
    
    public DoctorProfileEntity toEntity(Doctor domain) {
        if (domain == null) {
            return null;
        }
        UserEntity user = domain.getUser() != null ? userMapper.toEntity(domain.getUser()) : null;
        DoctorProfileEntity entity = new DoctorProfileEntity();
        entity.setId(domain.getId());
        entity.setUser(user);
        entity.setFirstName(domain.getFirstName());
        entity.setLastName(domain.getLastName());
        entity.setSpecialization(domain.getSpecialization());
        entity.setBio(domain.getBio());
        entity.setLicenseNumber(domain.getLicenseNumber());
        entity.setClinicName(domain.getClinicName());
        entity.setYearsOfExperience(domain.getYearsOfExperience());
        entity.setLocation(domain.getLocation());
        entity.setProfilePhotoUrl(domain.getProfilePhotoUrl());
        entity.setOpeningHours(domain.getOpeningHours());
        entity.setClosingHours(domain.getClosingHours());
        entity.setIsAbsent(domain.getIsAbsent());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}

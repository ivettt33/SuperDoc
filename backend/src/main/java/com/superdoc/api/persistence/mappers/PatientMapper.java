package com.superdoc.api.persistence.mappers;

import com.superdoc.api.BLL.domain.Patient;
import com.superdoc.api.BLL.domain.User;
import com.superdoc.api.persistence.entities.PatientProfileEntity;
import com.superdoc.api.persistence.entities.UserEntity;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PatientMapper {
    
    private final UserMapper userMapper;
    
    public Patient toDomain(PatientProfileEntity entity) {
        if (entity == null) {
            return null;
        }
        User user = entity.getUser() != null ? userMapper.toDomain(entity.getUser()) : null;
        return Patient.builder()
                .id(entity.getId())
                .user(user)
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .dateOfBirth(entity.getDateOfBirth())
                .gender(entity.getGender())
                .conditions(entity.getConditions())
                .insuranceNumber(entity.getInsuranceNumber())
                .profilePicture(entity.getProfilePicture())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
    
    public PatientProfileEntity toEntity(Patient domain) {
        if (domain == null) {
            return null;
        }
        UserEntity user = domain.getUser() != null ? userMapper.toEntity(domain.getUser()) : null;
        PatientProfileEntity entity = new PatientProfileEntity();
        entity.setId(domain.getId());
        entity.setUser(user);
        entity.setFirstName(domain.getFirstName());
        entity.setLastName(domain.getLastName());
        entity.setDateOfBirth(domain.getDateOfBirth());
        entity.setGender(domain.getGender());
        entity.setConditions(domain.getConditions());
        entity.setInsuranceNumber(domain.getInsuranceNumber());
        entity.setProfilePicture(domain.getProfilePicture());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}

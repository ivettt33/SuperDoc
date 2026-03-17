package com.superdoc.api.persistence.mappers;

import com.superdoc.api.BLL.domain.Prescription;
import com.superdoc.api.BLL.domain.User;
import com.superdoc.api.persistence.entities.PrescriptionEntity;
import com.superdoc.api.persistence.entities.UserEntity;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PrescriptionMapper {
    
    private final UserMapper userMapper;
    
    public Prescription toDomain(PrescriptionEntity entity) {
        if (entity == null) {
            return null;
        }
        User patient = entity.getPatient() != null ? userMapper.toDomain(entity.getPatient()) : null;
        User doctor = entity.getDoctor() != null ? userMapper.toDomain(entity.getDoctor()) : null;
        return Prescription.builder()
                .id(entity.getId())
                .patient(patient)
                .doctor(doctor)
                .medicationName(entity.getMedicationName())
                .dosage(entity.getDosage())
                .frequency(entity.getFrequency())
                .duration(entity.getDuration())
                .instructions(entity.getInstructions())
                .status(entity.getStatus())
                .issuedAt(entity.getIssuedAt())
                .validUntil(entity.getValidUntil())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
    
    public PrescriptionEntity toEntity(Prescription domain) {
        if (domain == null) {
            return null;
        }
        UserEntity patient = domain.getPatient() != null ? userMapper.toEntity(domain.getPatient()) : null;
        UserEntity doctor = domain.getDoctor() != null ? userMapper.toEntity(domain.getDoctor()) : null;
        PrescriptionEntity entity = new PrescriptionEntity();
        entity.setId(domain.getId());
        entity.setPatient(patient);
        entity.setDoctor(doctor);
        entity.setMedicationName(domain.getMedicationName());
        entity.setDosage(domain.getDosage());
        entity.setFrequency(domain.getFrequency());
        entity.setDuration(domain.getDuration());
        entity.setInstructions(domain.getInstructions());
        entity.setStatus(domain.getStatus());
        entity.setIssuedAt(domain.getIssuedAt());
        entity.setValidUntil(domain.getValidUntil());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}

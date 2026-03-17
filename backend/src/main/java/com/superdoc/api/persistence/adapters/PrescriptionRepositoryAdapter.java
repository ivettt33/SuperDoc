package com.superdoc.api.persistence.adapters;

import com.superdoc.api.BLL.IRepositories.IPrescriptionRepository;
import com.superdoc.api.BLL.domain.Prescription;
import com.superdoc.api.enumerate.PrescriptionStatus;
import com.superdoc.api.persistence.entities.PrescriptionEntity;
import com.superdoc.api.persistence.mappers.PrescriptionMapper;
import com.superdoc.api.persistence.repo.PrescriptionRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class PrescriptionRepositoryAdapter implements IPrescriptionRepository {
    
    private final PrescriptionRepository jpaRepository;
    private final PrescriptionMapper mapper;
    
    public PrescriptionRepositoryAdapter(PrescriptionRepository jpaRepository, PrescriptionMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    public Prescription save(Prescription prescription) {
        PrescriptionEntity entity = mapper.toEntity(prescription);
        PrescriptionEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
    
    @Override
    public Optional<Prescription> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }
    
    @Override
    public List<Prescription> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Prescription> findByDoctor_IdOrderByCreatedAtDesc(Long doctorId) {
        return jpaRepository.findByDoctor_IdOrderByCreatedAtDesc(doctorId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Prescription> findByPatient_IdOrderByCreatedAtDesc(Long patientId) {
        return jpaRepository.findByPatient_IdOrderByCreatedAtDesc(patientId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Prescription> findByPatientEmail(String patientEmail) {
        return jpaRepository.findByPatientEmail(patientEmail).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Prescription> findByDoctor_IdAndStatusOrderByCreatedAtDesc(Long doctorId, PrescriptionStatus status) {
        return jpaRepository.findByDoctor_IdAndStatusOrderByCreatedAtDesc(doctorId, status).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Prescription> findByPatient_IdAndStatusOrderByCreatedAtDesc(Long patientId, PrescriptionStatus status) {
        return jpaRepository.findByPatient_IdAndStatusOrderByCreatedAtDesc(patientId, status).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Prescription> findByStatusAndValidUntilBefore(PrescriptionStatus status, LocalDate today) {
        return jpaRepository.findByStatusAndValidUntilBefore(status, today).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}

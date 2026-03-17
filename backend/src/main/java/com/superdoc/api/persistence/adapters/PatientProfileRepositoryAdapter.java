package com.superdoc.api.persistence.adapters;

import com.superdoc.api.BLL.IRepositories.IPatientProfileRepository;
import com.superdoc.api.BLL.domain.Patient;
import com.superdoc.api.persistence.entities.PatientProfileEntity;
import com.superdoc.api.persistence.mappers.PatientMapper;
import com.superdoc.api.persistence.repo.PatientProfileRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class PatientProfileRepositoryAdapter implements IPatientProfileRepository {
    
    private final PatientProfileRepository jpaRepository;
    private final PatientMapper mapper;
    
    public PatientProfileRepositoryAdapter(PatientProfileRepository jpaRepository, PatientMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    public Patient save(Patient patient) {
        PatientProfileEntity entity = mapper.toEntity(patient);
        PatientProfileEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
    
    @Override
    public Optional<Patient> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }
    
    @Override
    public List<Patient> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}

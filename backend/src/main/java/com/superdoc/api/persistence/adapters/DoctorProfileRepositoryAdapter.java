package com.superdoc.api.persistence.adapters;

import com.superdoc.api.BLL.IRepositories.IDoctorProfileRepository;
import com.superdoc.api.BLL.domain.Doctor;
import com.superdoc.api.persistence.entities.DoctorProfileEntity;
import com.superdoc.api.persistence.mappers.DoctorMapper;
import com.superdoc.api.persistence.repo.DoctorProfileRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class DoctorProfileRepositoryAdapter implements IDoctorProfileRepository {
    
    private final DoctorProfileRepository jpaRepository;
    private final DoctorMapper mapper;
    
    public DoctorProfileRepositoryAdapter(DoctorProfileRepository jpaRepository, DoctorMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    public Doctor save(Doctor doctor) {
        DoctorProfileEntity entity = mapper.toEntity(doctor);
        DoctorProfileEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
    
    @Override
    public Optional<Doctor> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }
    
    @Override
    public List<Doctor> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}

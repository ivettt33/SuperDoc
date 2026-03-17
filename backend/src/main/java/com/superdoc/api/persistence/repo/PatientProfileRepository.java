package com.superdoc.api.persistence.repo;

import com.superdoc.api.persistence.entities.PatientProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientProfileRepository extends JpaRepository<PatientProfileEntity, Long> {}

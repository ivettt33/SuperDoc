package com.superdoc.api.persistence.repo;

import com.superdoc.api.persistence.entities.DoctorProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorProfileRepository extends JpaRepository<DoctorProfileEntity, Long> {}

package com.superdoc.api.persistence.repo;

import com.superdoc.api.enumerate.PrescriptionStatus;
import com.superdoc.api.persistence.entities.PrescriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PrescriptionRepository extends JpaRepository<PrescriptionEntity, Long> {
    
    // Pure Spring Data JPA method naming - use underscore to access nested properties (@ManyToOne relationships)
    List<PrescriptionEntity> findByDoctor_IdOrderByCreatedAtDesc(Long doctorId);
    
    List<PrescriptionEntity> findByPatient_IdOrderByCreatedAtDesc(Long patientId);
    
    // Need query for nested field (patient.email) 
    @Query("SELECT DISTINCT p FROM PrescriptionEntity p WHERE p.patient.email = :patientEmail ORDER BY p.createdAt DESC")
    List<PrescriptionEntity> findByPatientEmail(@Param("patientEmail") String patientEmail);
    
    List<PrescriptionEntity> findByDoctor_IdAndStatusOrderByCreatedAtDesc(Long doctorId, PrescriptionStatus status);
    
    List<PrescriptionEntity> findByPatient_IdAndStatusOrderByCreatedAtDesc(Long patientId, PrescriptionStatus status);
    
    Optional<PrescriptionEntity> findById(Long id);
    
    // Need query for complex condition (status = ACTIVE AND validUntil < today)
    @Query("SELECT p FROM PrescriptionEntity p WHERE p.status = :status AND p.validUntil < :today")
    List<PrescriptionEntity> findByStatusAndValidUntilBefore(@Param("status") PrescriptionStatus status, @Param("today") LocalDate today);
}


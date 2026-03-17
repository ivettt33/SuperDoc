package com.superdoc.api.persistence.repo;

import com.superdoc.api.enumerate.AppointmentStatus;
import com.superdoc.api.persistence.entities.AppointmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<AppointmentEntity, Long> {
    
    // Pure Spring Data JPA method naming - use underscore to access nested properties (@ManyToOne relationships)
    List<AppointmentEntity> findByDoctor_Id(Long doctorId);
    
    List<AppointmentEntity> findByPatient_Id(Long patientId);
    
    List<AppointmentEntity> findByDoctor_IdAndStatus(Long doctorId, AppointmentStatus status);
    
    List<AppointmentEntity> findByPatient_IdAndStatus(Long patientId, AppointmentStatus status);
    
    List<AppointmentEntity> findByDoctor_IdAndAppointmentDateTimeBetween(Long doctorId, LocalDateTime start, LocalDateTime end);
    
    List<AppointmentEntity> findByPatient_IdAndAppointmentDateTimeBetween(Long patientId, LocalDateTime start, LocalDateTime end);
    
    // For conflict check - need query because of "not equals" condition
    @Query("SELECT a FROM AppointmentEntity a WHERE a.doctor.id = :doctorId AND a.appointmentDateTime = :dateTime AND a.status != :status")
    Optional<AppointmentEntity> findByDoctorIdAndAppointmentDateTimeAndStatusNot(
        @Param("doctorId") Long doctorId,
        @Param("dateTime") LocalDateTime dateTime,
        @Param("status") AppointmentStatus status
    );
    
    Optional<AppointmentEntity> findById(Long id);
}


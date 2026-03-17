package com.superdoc.api.BLL.IRepositories;

import com.superdoc.api.BLL.domain.Prescription;
import com.superdoc.api.enumerate.PrescriptionStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IPrescriptionRepository {
    Prescription save(Prescription prescription);
    Optional<Prescription> findById(Long id);
    List<Prescription> findAll();
    List<Prescription> findByDoctor_IdOrderByCreatedAtDesc(Long doctorId);
    List<Prescription> findByPatient_IdOrderByCreatedAtDesc(Long patientId);
    List<Prescription> findByPatientEmail(String patientEmail);
    List<Prescription> findByDoctor_IdAndStatusOrderByCreatedAtDesc(Long doctorId, PrescriptionStatus status);
    List<Prescription> findByPatient_IdAndStatusOrderByCreatedAtDesc(Long patientId, PrescriptionStatus status);
    List<Prescription> findByStatusAndValidUntilBefore(PrescriptionStatus status, LocalDate today);
}

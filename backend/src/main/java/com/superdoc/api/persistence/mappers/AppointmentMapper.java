package com.superdoc.api.persistence.mappers;

import com.superdoc.api.BLL.domain.Appointment;
import com.superdoc.api.BLL.domain.Doctor;
import com.superdoc.api.BLL.domain.Patient;
import com.superdoc.api.persistence.entities.AppointmentEntity;
import com.superdoc.api.persistence.entities.DoctorProfileEntity;
import com.superdoc.api.persistence.entities.PatientProfileEntity;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AppointmentMapper {
    
    private final DoctorMapper doctorMapper;
    private final PatientMapper patientMapper;
    
    public Appointment toDomain(AppointmentEntity entity) {
        if (entity == null) {
            return null;
        }
        Doctor doctor = entity.getDoctor() != null ? doctorMapper.toDomain(entity.getDoctor()) : null;
        Patient patient = entity.getPatient() != null ? patientMapper.toDomain(entity.getPatient()) : null;
        return Appointment.builder()
                .id(entity.getId())
                .doctor(doctor)
                .patient(patient)
                .appointmentDateTime(entity.getAppointmentDateTime())
                .status(entity.getStatus())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
    
    public AppointmentEntity toEntity(Appointment domain) {
        if (domain == null) {
            return null;
        }
        DoctorProfileEntity doctor = domain.getDoctor() != null ? doctorMapper.toEntity(domain.getDoctor()) : null;
        PatientProfileEntity patient = domain.getPatient() != null ? patientMapper.toEntity(domain.getPatient()) : null;
        AppointmentEntity entity = new AppointmentEntity();
        entity.setId(domain.getId());
        entity.setDoctor(doctor);
        entity.setPatient(patient);
        entity.setAppointmentDateTime(domain.getAppointmentDateTime());
        entity.setStatus(domain.getStatus());
        entity.setNotes(domain.getNotes());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }
}

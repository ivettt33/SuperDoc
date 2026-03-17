package com.superdoc.api.persistence.adapters;

import com.superdoc.api.BLL.IRepositories.IAppointmentRepository;
import com.superdoc.api.BLL.domain.Appointment;
import com.superdoc.api.enumerate.AppointmentStatus;
import com.superdoc.api.persistence.entities.AppointmentEntity;
import com.superdoc.api.persistence.mappers.AppointmentMapper;
import com.superdoc.api.persistence.repo.AppointmentRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class AppointmentRepositoryAdapter implements IAppointmentRepository {
    
    private final AppointmentRepository jpaRepository;
    private final AppointmentMapper mapper;
    
    public AppointmentRepositoryAdapter(AppointmentRepository jpaRepository, AppointmentMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    public Appointment save(Appointment appointment) {
        AppointmentEntity entity = mapper.toEntity(appointment);
        AppointmentEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
    
    @Override
    public Optional<Appointment> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }
    
    @Override
    public List<Appointment> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Appointment> findByDoctor_Id(Long doctorId) {
        return jpaRepository.findByDoctor_Id(doctorId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Appointment> findByPatient_Id(Long patientId) {
        return jpaRepository.findByPatient_Id(patientId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Appointment> findByDoctor_IdAndStatus(Long doctorId, AppointmentStatus status) {
        return jpaRepository.findByDoctor_IdAndStatus(doctorId, status).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Appointment> findByPatient_IdAndStatus(Long patientId, AppointmentStatus status) {
        return jpaRepository.findByPatient_IdAndStatus(patientId, status).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Appointment> findByDoctor_IdAndAppointmentDateTimeBetween(Long doctorId, LocalDateTime start, LocalDateTime end) {
        return jpaRepository.findByDoctor_IdAndAppointmentDateTimeBetween(doctorId, start, end).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Appointment> findByPatient_IdAndAppointmentDateTimeBetween(Long patientId, LocalDateTime start, LocalDateTime end) {
        return jpaRepository.findByPatient_IdAndAppointmentDateTimeBetween(patientId, start, end).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<Appointment> findByDoctorIdAndAppointmentDateTimeAndStatusNot(Long doctorId, LocalDateTime dateTime, AppointmentStatus status) {
        return jpaRepository.findByDoctorIdAndAppointmentDateTimeAndStatusNot(doctorId, dateTime, status)
                .map(mapper::toDomain);
    }
}

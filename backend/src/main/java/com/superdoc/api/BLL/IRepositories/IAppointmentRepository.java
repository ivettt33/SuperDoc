package com.superdoc.api.BLL.IRepositories;

import com.superdoc.api.BLL.domain.Appointment;
import com.superdoc.api.enumerate.AppointmentStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface IAppointmentRepository {
    Appointment save(Appointment appointment);
    Optional<Appointment> findById(Long id);
    List<Appointment> findAll();
    List<Appointment> findByDoctor_Id(Long doctorId);
    List<Appointment> findByPatient_Id(Long patientId);
    List<Appointment> findByDoctor_IdAndStatus(Long doctorId, AppointmentStatus status);
    List<Appointment> findByPatient_IdAndStatus(Long patientId, AppointmentStatus status);
    List<Appointment> findByDoctor_IdAndAppointmentDateTimeBetween(Long doctorId, LocalDateTime start, LocalDateTime end);
    List<Appointment> findByPatient_IdAndAppointmentDateTimeBetween(Long patientId, LocalDateTime start, LocalDateTime end);
    Optional<Appointment> findByDoctorIdAndAppointmentDateTimeAndStatusNot(Long doctorId, LocalDateTime dateTime, AppointmentStatus status);
}

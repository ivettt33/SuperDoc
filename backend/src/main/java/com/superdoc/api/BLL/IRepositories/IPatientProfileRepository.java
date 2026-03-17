package com.superdoc.api.BLL.IRepositories;

import com.superdoc.api.BLL.domain.Patient;
import java.util.List;
import java.util.Optional;

public interface IPatientProfileRepository {
    Patient save(Patient patient);
    Optional<Patient> findById(Long id);
    List<Patient> findAll();
}

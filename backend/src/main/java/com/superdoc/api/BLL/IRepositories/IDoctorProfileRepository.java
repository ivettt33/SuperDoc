package com.superdoc.api.BLL.IRepositories;

import com.superdoc.api.BLL.domain.Doctor;
import java.util.List;
import java.util.Optional;

public interface IDoctorProfileRepository {
    Doctor save(Doctor doctor);
    Optional<Doctor> findById(Long id);
    List<Doctor> findAll();
}

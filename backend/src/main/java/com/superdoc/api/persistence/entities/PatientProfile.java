package com.superdoc.api.persistence.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.Instant;
import lombok.Data;

@Data
@Entity @Table(name="patient_profiles")
public class PatientProfile {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", nullable=false, unique=true)
    @JsonIgnore
    private UserEntity user;

    @Column(nullable=false) private String firstName;
    @Column(nullable=false) private String lastName;
    private LocalDate dateOfBirth;
    private String gender;
    private String conditions;
    private String insuranceNumber;
    private String profilePicture;
    
    @Column
    private Instant createdAt = Instant.now();

    @Column
    private Instant updatedAt = Instant.now();
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    // getters/setters
}

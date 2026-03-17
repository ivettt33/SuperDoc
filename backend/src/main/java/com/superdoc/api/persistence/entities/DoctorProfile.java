package com.superdoc.api.persistence.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.Instant;
import java.time.LocalTime;

@Data
@Entity
@Table(name = "doctor_profiles")
public class DoctorProfile {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY) 
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", nullable=false, unique=true)
    @JsonIgnore
    private UserEntity user;

    @Column(nullable=false) 
    private String firstName;
    
    @Column(nullable=false) 
    private String lastName;
    
    @Column(name = "specialization")
    private String specialization;
    
    @Column
    private String bio;
    
    @Column(name = "license_number")
    private String licenseNumber;
    
    @Column(name = "clinic_name")
    private String clinicName;
    
    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;
    
    @Column
    private String location;
    
    @Column(name = "profile_photo_url")
    private String profilePhotoUrl;
    
    @Column(name = "opening_hours")
    private LocalTime openingHours = LocalTime.of(9, 0);
    
    @Column(name = "closing_hours")
    private LocalTime closingHours = LocalTime.of(17, 0);
    
    @Column(name = "is_absent", nullable = false)
    private Boolean isAbsent = false;

    @Column
    private Instant createdAt = Instant.now();

    @Column
    private Instant updatedAt = Instant.now();
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}

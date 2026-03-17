package com.superdoc.api.BLL.domain;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import java.time.Instant;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Patient {
    private Long id;
    private User user;
    private String firstName;
    private String lastName;
    private LocalDate dateOfBirth;
    private String gender;
    private String conditions;
    private String insuranceNumber;
    private String profilePicture;
    private Instant createdAt;
    private Instant updatedAt;
}

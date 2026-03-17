package com.superdoc.api.BLL.domain;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import java.time.Instant;
import com.superdoc.api.enumerate.Role;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User {
    private Long id;
    private String email;
    private String passwordHash;
    private Role role;
    private Instant createdAt;
    private Instant updatedAt;
    private String passwordResetToken;
    private Instant passwordResetExpiresAt;
}
package com.superdoc.api.persistence.mappers;

import com.superdoc.api.BLL.domain.User;
import com.superdoc.api.persistence.entities.UserEntity;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserMapper {
    
    public User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        return User.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .passwordHash(entity.getPasswordHash())
                .role(entity.getRole())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getCreatedAt()) // UserEntity doesn't have updatedAt, use createdAt
                .passwordResetToken(entity.getPasswordResetToken())
                .passwordResetExpiresAt(entity.getPasswordResetExpiresAt())
                .build();
    }
    
    public UserEntity toEntity(User domain) {
        if (domain == null) {
            return null;
        }
        UserEntity entity = UserEntity.builder()
                .id(domain.getId())
                .email(domain.getEmail())
                .passwordHash(domain.getPasswordHash())
                .role(domain.getRole())
                .createdAt(domain.getCreatedAt())
                .passwordResetToken(domain.getPasswordResetToken())
                .passwordResetExpiresAt(domain.getPasswordResetExpiresAt())
                .build();
        return entity;
    }
}

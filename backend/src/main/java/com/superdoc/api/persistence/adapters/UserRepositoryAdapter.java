package com.superdoc.api.persistence.adapters;

import com.superdoc.api.BLL.IRepositories.IUserRepository;
import com.superdoc.api.BLL.domain.User;
import com.superdoc.api.persistence.entities.UserEntity;
import com.superdoc.api.persistence.mappers.UserMapper;
import com.superdoc.api.persistence.repo.UserRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class UserRepositoryAdapter implements IUserRepository {
    
    private final UserRepository jpaRepository;
    private final UserMapper mapper;
    
    public UserRepositoryAdapter(UserRepository jpaRepository, UserMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }
    
    @Override
    public User save(User user) {
        UserEntity entity = mapper.toEntity(user);
        UserEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }
    
    @Override
    public Optional<User> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }
    
    @Override
    public List<User> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email)
                .map(mapper::toDomain);
    }
    
    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }
    
    @Override
    public Optional<User> findByPasswordResetToken(String token) {
        return jpaRepository.findByPasswordResetToken(token)
                .map(mapper::toDomain);
    }
}

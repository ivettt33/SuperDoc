package com.superdoc.api.repository;

import com.superdoc.api.enumerate.Role;
import com.superdoc.api.persistence.entities.UserEntity;
import com.superdoc.api.persistence.repo.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.flyway.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
        user.setEmail("test@example.com");
        user.setPasswordHash("hashedPassword");
        user.setRole(Role.DOCTOR);
        user = entityManager.persistFlushFind(user);
        entityManager.clear();
    }

    @Test
    void findByEmail_existingEmail_returnsUser() {
        // Act
        Optional<UserEntity> found = userRepository.findByEmail("test@example.com");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@example.com");
        assertThat(found.get().getRole()).isEqualTo(Role.DOCTOR);
    }

    @Test
    void findByEmail_nonExistentEmail_returnsEmpty() {
        // Act
        Optional<UserEntity> found = userRepository.findByEmail("nonexistent@example.com");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    void existsByEmail_existingEmail_returnsTrue() {
        // Act
        boolean exists = userRepository.existsByEmail("test@example.com");

        // Assert
        assertThat(exists).isTrue();
    }

    @Test
    void existsByEmail_nonExistentEmail_returnsFalse() {
        // Act
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        // Assert
        assertThat(exists).isFalse();
    }

    @Test
    void findByPasswordResetToken_existingToken_returnsUser() {
        // Arrange
        String token = "reset-token-123";
        user.setPasswordResetToken(token);
        user.setPasswordResetExpiresAt(Instant.now().plusSeconds(3600));
        entityManager.merge(user);
        entityManager.flush();
        entityManager.clear();

        // Act
        Optional<UserEntity> found = userRepository.findByPasswordResetToken(token);

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getPasswordResetToken()).isEqualTo(token);
    }

    @Test
    void findByPasswordResetToken_nonExistentToken_returnsEmpty() {
        // Act
        Optional<UserEntity> found = userRepository.findByPasswordResetToken("invalid-token");

        // Assert
        assertThat(found).isEmpty();
    }

    @Test
    void save_newUser_persistsCorrectly() {
        // Arrange
        UserEntity newUser = new UserEntity();
        newUser.setEmail("newuser@example.com");
        newUser.setPasswordHash("hash");
        newUser.setRole(Role.PATIENT);

        // Act
        UserEntity saved = userRepository.save(newUser);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Optional<UserEntity> found = userRepository.findById(saved.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("newuser@example.com");
        assertThat(found.get().getRole()).isEqualTo(Role.PATIENT);
    }

    @Test
    void save_updateUser_updatesCorrectly() {
        // Arrange
        user.setEmail("updated@example.com");
        user.setRole(Role.PATIENT);

        // Act
        UserEntity updated = userRepository.save(user);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Optional<UserEntity> found = userRepository.findById(updated.getId());
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("updated@example.com");
        assertThat(found.get().getRole()).isEqualTo(Role.PATIENT);
    }

    @Test
    void delete_removesUser() {
        // Arrange
        Long id = user.getId();

        // Act
        userRepository.delete(user);
        entityManager.flush();
        entityManager.clear();

        // Assert
        Optional<UserEntity> found = userRepository.findById(id);
        assertThat(found).isEmpty();
    }

    @Test
    void findByEmail_caseSensitive() {
        // Arrange
        user.setEmail("CaseSensitive@Example.com");
        entityManager.merge(user);
        entityManager.flush();
        entityManager.clear();

        // Act & Assert
        assertThat(userRepository.findByEmail("CaseSensitive@Example.com")).isPresent();
        assertThat(userRepository.findByEmail("casesensitive@example.com")).isEmpty();
    }
}


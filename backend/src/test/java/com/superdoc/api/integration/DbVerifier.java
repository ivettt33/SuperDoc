package com.superdoc.api.integration;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Function;

import jakarta.persistence.NoResultException;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Helper class for database verification in integration tests.
 * Uses EntityManager to ensure we read directly from the database,
 * bypassing JPA's first-level cache to verify actual persistence.
 */
@Component
public class DbVerifier {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Prepares EntityManager for fresh database read by flushing and clearing the persistence context.
     * This ensures that subsequent find operations read from the database, not from JPA's in-memory cache.
     * Call this before any EntityManager.find() operations in tests.
     */
    @Transactional
    public void prepareForVerification() {
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * Verifies an entity exists in the database by fetching it directly with EntityManager.
     * Automatically flushes and clears the persistence context before fetching.
     * This proves that the entity was physically written to the database.
     */
    @Transactional
    public <T> T findEntity(Class<T> entityClass, Long id) {
        prepareForVerification();
        return entityManager.find(entityClass, id);
    }

    /**
     * Gets the total count of entities in the database (including soft-deleted ones).
     * Useful for verifying that failed CREATE operations did not persist any data.
     * Automatically flushes and clears the persistence context before counting.
     */
    @Transactional
    public <T> long countEntities(Class<T> entityClass) {
        prepareForVerification();
        String entityName = entityClass.getSimpleName();
        return entityManager.createQuery(
                "SELECT COUNT(e) FROM " + entityName + " e",
                Long.class
        ).getSingleResult();
    }

    /**
     * Verifies an entity does NOT exist in the database.
     * Useful for verifying hard deletes or ensuring failed operations didn't persist.
     */
    @Transactional
    public <T> void verifyEntityDoesNotExist(Class<T> entityClass, Long id) {
        prepareForVerification();
        T entity = entityManager.find(entityClass, id);
        assertThat(entity).isNull();
    }

    /**
     * Verifies the count of entities matches the expected value.
     * Useful for ensuring no partial persistence occurred during failed operations.
     */
    @Transactional
    public <T> void verifyEntityCount(Class<T> entityClass, long expectedCount) {
        long actualCount = countEntities(entityClass);
        assertThat(actualCount).isEqualTo(expectedCount);
    }

    /**
     * Finds an entity and verifies a specific field value.
     * Useful for verifying updates persisted correctly.
     */
    @Transactional
    public <T> T verifyEntityField(Class<T> entityClass, Long id,
                                   Function<T, Object> fieldExtractor, Object expectedValue) {
        T entity = findEntity(entityClass, id);
        assertThat(entity).isNotNull();
        assertThat(fieldExtractor.apply(entity)).isEqualTo(expectedValue);
        return entity;
    }

    /**
     * Verifies an entity exists and is not null.
     * Throws assertion error if entity is not found.
     */
    @Transactional
    public <T> T verifyEntityExists(Class<T> entityClass, Long id) {
        T entity = findEntity(entityClass, id);
        assertThat(entity).isNotNull();
        return entity;
    }

    /**
     * Finds an entity by a field value using JPQL query.
     * Useful for finding entities by non-ID fields like email.
     * Automatically flushes and clears the persistence context before querying.
     */
    @Transactional
    public <T> T findEntityByField(Class<T> entityClass, String fieldName, Object fieldValue) {
        prepareForVerification();
        String entityName = entityClass.getSimpleName();
        String queryString = "SELECT e FROM " + entityName + " e WHERE e." + fieldName + " = :value";
        try {
            return entityManager.createQuery(queryString, entityClass)
                    .setParameter("value", fieldValue)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}

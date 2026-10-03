package com.votacao.entrypoint.persistence;

import com.votacao.application.gateway.TimeProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class JpaTimeProvider implements TimeProvider {

    private final EntityManagerFactory factory;

    public JpaTimeProvider(EntityManagerFactory factory) {
        this.factory = factory;
    }

    @Override
    public LocalDateTime now() {
        try (EntityManager entityManager = factory.createEntityManager()) {
            return (LocalDateTime) entityManager
                    .createNativeQuery("SELECT CURRENT_TIMESTAMP", LocalDateTime.class)
                    .getSingleResult();
        }
    }

}

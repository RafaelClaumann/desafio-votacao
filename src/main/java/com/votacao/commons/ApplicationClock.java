package com.votacao.commons;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ApplicationClock {

    private static EntityManagerFactory factory;

    public ApplicationClock(EntityManagerFactory factory) {
        ApplicationClock.factory = factory;
    }

    public static LocalDateTime now() {
        try (EntityManager entityManager = factory.createEntityManager()) {
            return (LocalDateTime) entityManager
                    .createNativeQuery("SELECT CURRENT_TIMESTAMP", LocalDateTime.class)
                    .getSingleResult();
        }
    }

}

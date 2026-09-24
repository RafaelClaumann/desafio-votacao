package com.votacao.entrypoint.persistence;

import com.votacao.entrypoint.mapper.PautaMapperImpl;
import com.votacao.entrypoint.mapper.SessaoMapperImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import({SessaoRepositoryAdapter.class, SessaoMapperImpl.class, PautaMapperImpl.class})
@DisplayName("SessaoRepositoryAdapter (integration)")
class SessaoRepositoryAdapterITTest {

    @Autowired
    private SessaoRepositoryAdapter adapter;

    @Test
    @DisplayName("now should return the current timestamp from the database clock")
    void now_shouldReturnCurrentDatabaseTimestamp() {
        LocalDateTime dbNow = adapter.now();

        assertNotNull(dbNow);
        assertTrue(dbNow.isBefore(LocalDateTime.now().plusMinutes(1)));
        assertTrue(dbNow.isAfter(LocalDateTime.now().minusMinutes(1)));
    }

}
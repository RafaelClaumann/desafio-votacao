package com.votacao.entrypoint.persistence;

import com.votacao.application.model.Pauta;
import com.votacao.application.model.Sessao;
import com.votacao.application.model.exception.DuplicatedSessaoException;
import com.votacao.entrypoint.mapper.PautaMapperImpl;
import com.votacao.entrypoint.mapper.SessaoMapperImpl;
import com.votacao.entrypoint.persistence.entity.PautaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import({SessaoRepositoryAdapter.class, SessaoMapperImpl.class, PautaMapperImpl.class, JpaTimeProvider.class})
@DisplayName("SessaoRepositoryAdapter (integration)")
class SessaoRepositoryAdapterITTest {

    @Autowired
    private SessaoRepositoryAdapter adapter;

    @Autowired
    private JpaTimeProvider timeProvider;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("now should return the current timestamp from the database clock")
    void now_shouldReturnCurrentDatabaseTimestamp() {
        LocalDateTime dbNow = timeProvider.now();

        assertNotNull(dbNow);
        assertTrue(dbNow.isBefore(LocalDateTime.now().plusMinutes(1)));
        assertTrue(dbNow.isAfter(LocalDateTime.now().minusMinutes(1)));
    }

    @Test
    @DisplayName("save should reject a second session for the same pauta")
    void save_shouldReject_whenSecondSessionForSamePauta() {
        Long idPauta = persistPauta("[SESS] Sessao única por pauta");
        Pauta pauta = new Pauta(idPauta, "[SESS] Sessao única por pauta", 10L);

        adapter.save(Sessao.registrar(pauta, LocalDateTime.now(), LocalDateTime.now().plusMinutes(10L)));

        DuplicatedSessaoException exception = assertThrows(
                DuplicatedSessaoException.class,
                () -> adapter.save(Sessao.registrar(pauta, LocalDateTime.now(), LocalDateTime.now().plusMinutes(10L)))
        );

        assertEquals("Já existe uma sessão para a pauta: " + idPauta, exception.getMessage());
        assertNotNull(exception.getCause());
        assertInstanceOf(DataIntegrityViolationException.class, exception.getCause());
    }

    private Long persistPauta(String titulo) {
        PautaEntity pautaEntity = new PautaEntity();
        pautaEntity.setTitulo(titulo);
        pautaEntity.setTempoVotacaoMinutos(10L);
        entityManager.persist(pautaEntity);
        return pautaEntity.getId();
    }

}

package com.votacao.entrypoint.persistence;

import com.votacao.application.model.Pauta;
import com.votacao.application.model.Sessao;
import com.votacao.application.model.Voto;
import com.votacao.application.model.exception.DuplicatedVoteException;
import com.votacao.entrypoint.mapper.SessaoMapperImpl;
import com.votacao.entrypoint.mapper.VotoMapperImpl;
import com.votacao.entrypoint.persistence.entity.PautaEntity;
import com.votacao.entrypoint.persistence.entity.SessaoEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DataJpaTest
@Import({VotoRepositoryAdapter.class, VotoMapperImpl.class, SessaoMapperImpl.class})
@DisplayName("VotoRepositoryAdapter (integration)")
class VotoRepositoryAdapterITTest {

    @Autowired
    private VotoRepositoryAdapter adapter;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SessaoMapperImpl sessaoMapper;

    @Test
    @DisplayName("save should allow the same documento in different sessions")
    void save_shouldAllowSameDocumentoInDifferentSessions() {
        Sessao firstSession = persistSession("[PF7] Pauta um");
        Sessao secondSession = persistSession("[PF7] Pauta dois");
        String documento = "12345678909";

        Voto firstVote = adapter.save(Voto.registrar(firstSession, documento, Voto.Escolha.SIM));
        Voto secondVote = adapter.save(Voto.registrar(firstSession, documento, Voto.Escolha.NAO));

        assertNotNull(firstVote.id());
        assertNotNull(secondVote.id());
        assertEquals(1, adapter.countBySessaoIdAndEscolha(firstSession.id(), Voto.Escolha.SIM));
        assertEquals(1, adapter.countBySessaoIdAndEscolha(secondSession.id(), Voto.Escolha.NAO));
    }

    @Test
    @DisplayName("save should reject a second vote of the same documento in the same session")
    void save_shouldReject_whenDocumentoAlreadyVotedInSession() {
        Sessao session = persistSession("[PF7] Pauta tres");
        String documento = "12345678909";

        adapter.save(Voto.registrar(session, documento, Voto.Escolha.SIM));

        DuplicatedVoteException exception = assertThrows(
                DuplicatedVoteException.class,
                () -> adapter.save(Voto.registrar(session, documento, Voto.Escolha.NAO))
        );

        assertEquals(
                "O documento " + documento + " já votou na sessão " + session.id(),
                exception.getMessage()
        );
    }

    private Sessao persistSession(String tituloPauta) {
        PautaEntity pautaEntity = new PautaEntity();
        pautaEntity.setTitulo(tituloPauta);
        pautaEntity.setTempoVotacaoMinutos(10L);
        entityManager.persist(pautaEntity);

        SessaoEntity sessaoEntity = new SessaoEntity();
        sessaoEntity.setPauta(pautaEntity);
        sessaoEntity.setStartedAt(LocalDateTime.now());
        sessaoEntity.setExpiresAt(LocalDateTime.now().plusMinutes(10L));
        entityManager.persist(sessaoEntity);

        Pauta pauta = new Pauta(pautaEntity.getId(), pautaEntity.getTitulo(), pautaEntity.getTempoVotacaoMinutos());
        return new Sessao(
                sessaoEntity.getId(),
                pauta,
                sessaoEntity.getStartedAt(),
                sessaoEntity.getExpiresAt()
        );
    }

}

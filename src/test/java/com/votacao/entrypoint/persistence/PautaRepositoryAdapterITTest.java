package com.votacao.entrypoint.persistence;

import com.votacao.application.model.Pauta;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import(PautaRepositoryAdapter.class)
class PautaRepositoryAdapterITTest {

    @Autowired
    private PautaRepositoryAdapter adapter;

    @Test
    void save_shouldReturnPautaWithGeneratedId() {
        String title = "Discuss the new voting rules";
        Pauta pauta = new Pauta(null, title, 10L);

        Pauta saved = adapter.save(pauta);

        assertNull(pauta.id());
        assertNotNull(saved.id());
        assertEquals(title, pauta.titulo());
        assertEquals(title, saved.titulo());
    }

    @Test
    void getPautas_shouldReturnPautas() {
        Pauta saved00 = adapter.save(new Pauta(null, "[00] Discuss the new voting rules", 10L));
        Pauta saved01 = adapter.save(new Pauta(null, "[01] Discuss the new voting rules", 10L));

        List<Pauta> pautas = adapter.getPautas();

        assertEquals(2, pautas.size());
        assertEquals(saved00, pautas.getFirst());
        assertTrue(pautas.containsAll(List.of(saved00, saved01)));
    }

}

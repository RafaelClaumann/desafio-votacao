package com.votacao.entrypoint.persistence;

import com.votacao.application.model.Pauta;
import com.votacao.application.model.exception.DuplicatedPautaException;
import com.votacao.entrypoint.mapper.PautaMapperImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import({PautaRepositoryAdapter.class, PautaMapperImpl.class})
@DisplayName("PautaRepositoryAdapter (integration)")
class PautaRepositoryAdapterITTest {

    @Autowired
    private PautaRepositoryAdapter adapter;

    @Test
    @DisplayName("save should return the pauta with a generated id")
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
    @DisplayName("getPautas should return the saved pautas")
    void getPautas_shouldReturnPautas() {
        Pauta saved00 = adapter.save(new Pauta(null, "[00] Discuss the new voting rules", 10L));
        Pauta saved01 = adapter.save(new Pauta(null, "[01] Discuss the new voting rules", 10L));

        List<Pauta> pautas = adapter.getPautas();

        assertEquals(2, pautas.size());
        assertEquals(saved00, pautas.getFirst());
        assertTrue(pautas.containsAll(List.of(saved00, saved01)));
    }

    @Test
    @DisplayName("save should reject a title that differs only by case")
    void save_shouldReject_whenTitleDiffersOnlyByCase() {
        String title = "Discuss the new voting rules";
        adapter.save(new Pauta(null, title, 10L));

        DuplicatedPautaException exception = assertThrows(
                DuplicatedPautaException.class,
                () -> adapter.save(new Pauta(null, title.toLowerCase(), 10L))
        );

        assertEquals("Já existe uma pauta com o título: " + title.toLowerCase(), exception.getMessage());
    }

}

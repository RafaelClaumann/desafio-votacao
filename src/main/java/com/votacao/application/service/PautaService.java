package com.votacao.application.service;

import com.votacao.application.gateway.PautaRepository;
import com.votacao.application.model.Pauta;
import com.votacao.application.model.exception.DuplicatedPautaException;
import com.votacao.application.model.exception.PautaNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PautaService {

    private final PautaRepository repository;

    public PautaService(PautaRepository repository) {
        this.repository = repository;
    }

    public Pauta savePauta(final Pauta pauta) {
        if (repository.existsByTituloIgnoreCase(pauta.titulo())) {
            throw new DuplicatedPautaException(pauta.titulo());
        }
        return repository.save(pauta);
    }

    public Pauta getPautaById(Long idPauta) {
        return repository.findById(idPauta).orElseThrow(() -> new PautaNotFoundException(idPauta));
    }

    public List<Pauta> getPautas() {
        return repository.getPautas();
    }

}

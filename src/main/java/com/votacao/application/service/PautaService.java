package com.votacao.application.service;

import com.votacao.application.gateway.PautaRepository;
import com.votacao.application.model.Pauta;
import org.springframework.stereotype.Service;

@Service
public class PautaService {

    private final PautaRepository repository;

    public PautaService(PautaRepository repository) {
        this.repository = repository;
    }

    public Pauta savePauta(final Pauta pauta) {
        return repository.save(pauta);
    }

}

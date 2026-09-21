package com.votacao.entrypoint.persistence;

import com.votacao.application.gateway.PautaRepository;
import com.votacao.application.model.Pauta;
import org.springframework.stereotype.Component;

@Component
public class PautaRepositoryAdapter implements PautaRepository {

    private final SpringDataPautaRepository repository;

    public PautaRepositoryAdapter(SpringDataPautaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Pauta save(Pauta pauta) {
        final PautaEntity pautaEntity = new PautaEntity(pauta.titulo(), pauta.tempoVotacaoMinutos());
        repository.save(pautaEntity);
        return pauta;
    }

}

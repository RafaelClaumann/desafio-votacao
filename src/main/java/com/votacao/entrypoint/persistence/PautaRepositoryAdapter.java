package com.votacao.entrypoint.persistence;

import com.votacao.application.gateway.PautaRepository;
import com.votacao.application.model.Pauta;
import com.votacao.entrypoint.persistence.entity.PautaEntity;
import com.votacao.entrypoint.persistence.jpa.SpringDataPautaRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PautaRepositoryAdapter implements PautaRepository {

    private final SpringDataPautaRepository repository;

    public PautaRepositoryAdapter(SpringDataPautaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Pauta save(Pauta pauta) {
        PautaEntity saved = repository.save(PautaEntity.fromDomain(pauta));
        return PautaEntity.fromEntity(saved);
    }

    @Override
    public List<Pauta> getPautas() {
        final List<PautaEntity> entities = repository.findAll();
        return entities.stream().map(PautaEntity::fromEntity).toList();
    }

    @Override
    public Pauta findById(Long pautaId) {
        return repository.findById(pautaId).map(PautaEntity::fromEntity).orElse(null);
    }

}

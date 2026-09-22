package com.votacao.entrypoint.persistence;

import com.votacao.application.gateway.VotoRepository;
import com.votacao.application.model.Voto;
import com.votacao.entrypoint.persistence.entity.VotoEntity;
import com.votacao.entrypoint.persistence.jpa.SpringDataVotoRepository;
import org.springframework.stereotype.Component;

@Component
public class VotoRepositoryAdapter implements VotoRepository {

    private final SpringDataVotoRepository repository;

    public VotoRepositoryAdapter(SpringDataVotoRepository repository) {
        this.repository = repository;
    }

    @Override
    public Voto save(Voto voto) {
        VotoEntity entity = VotoEntity.fromDomain(voto);
        VotoEntity saved = repository.save(entity);
        return VotoEntity.fromEntity(saved);
    }

}

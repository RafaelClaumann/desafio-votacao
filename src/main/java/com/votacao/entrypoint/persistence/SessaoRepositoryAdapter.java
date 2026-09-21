package com.votacao.entrypoint.persistence;

import com.votacao.application.gateway.SessaoRepository;
import com.votacao.application.model.Sessao;
import com.votacao.entrypoint.persistence.entity.SessaoEntity;
import com.votacao.entrypoint.persistence.jpa.SpringDataSessaoRepository;
import org.springframework.stereotype.Component;

@Component
public class SessaoRepositoryAdapter implements SessaoRepository {

    private final SpringDataSessaoRepository repository;

    public SessaoRepositoryAdapter(SpringDataSessaoRepository repository) {
        this.repository = repository;
    }

    @Override
    public Sessao save(Sessao sessao) {
        SessaoEntity saved = repository.save(SessaoEntity.fromDomain(sessao));
        return SessaoEntity.fromEntity(saved);
    }

}

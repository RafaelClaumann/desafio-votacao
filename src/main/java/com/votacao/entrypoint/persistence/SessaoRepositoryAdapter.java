package com.votacao.entrypoint.persistence;

import com.votacao.application.gateway.SessaoRepository;
import com.votacao.application.model.Sessao;
import com.votacao.entrypoint.persistence.entity.SessaoEntity;
import com.votacao.entrypoint.persistence.jpa.SpringDataSessaoRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class SessaoRepositoryAdapter implements SessaoRepository {

    private final SpringDataSessaoRepository repository;

    public SessaoRepositoryAdapter(SpringDataSessaoRepository repository) {
        this.repository = repository;
    }

    @Override
    public Sessao save(Sessao sessao) {
        SessaoEntity sessaoEntity = SessaoEntity.fromDomain(sessao);
        SessaoEntity saved = repository.save(sessaoEntity);
        return SessaoEntity.fromEntity(saved);
    }

    @Override
    public boolean existsByPautaId(Long pautaId) {
        return repository.existsByPautaId(pautaId);
    }

    @Override
    public List<Sessao> findAll() {
        return repository.findAll().stream().map(SessaoEntity::fromEntity).toList();
    }

    @Override
    public Optional<Sessao> findById(Long sessaoId) {
        return repository.findById(sessaoId).map(SessaoEntity::fromEntity);
    }

    @Override
    public Set<Long> findPautaIdsComSessao(List<Long> pautaIds) {
        return repository.findPautaIdsComSessao(pautaIds);
    }

}

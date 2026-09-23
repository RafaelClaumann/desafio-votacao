package com.votacao.entrypoint.persistence;

import com.votacao.application.gateway.SessaoRepository;
import com.votacao.application.model.Sessao;
import com.votacao.entrypoint.mapper.SessaoMapper;
import com.votacao.entrypoint.persistence.entity.SessaoEntity;
import com.votacao.entrypoint.persistence.jpa.SpringDataSessaoRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class SessaoRepositoryAdapter implements SessaoRepository {

    private final SpringDataSessaoRepository repository;
    private final SessaoMapper mapper;

    public SessaoRepositoryAdapter(SpringDataSessaoRepository repository, SessaoMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Sessao save(Sessao sessao) {
        SessaoEntity sessaoEntity = mapper.toEntity(sessao);
        SessaoEntity saved = repository.save(sessaoEntity);
        return mapper.toDomain(saved);
    }

    @Override
    public boolean existsByPautaId(Long pautaId) {
        return repository.existsByPautaId(pautaId);
    }

    @Override
    public List<Sessao> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Sessao> findById(Long sessaoId) {
        return repository.findById(sessaoId).map(mapper::toDomain);
    }

    @Override
    public Set<Long> findPautaIdsComSessao(List<Long> pautaIds) {
        return repository.findPautaIdsComSessao(pautaIds);
    }

}

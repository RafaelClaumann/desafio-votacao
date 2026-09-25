package com.votacao.entrypoint.persistence;

import com.votacao.application.gateway.SessaoRepository;
import com.votacao.application.model.Sessao;
import com.votacao.application.model.exception.DuplicatedSessaoException;
import com.votacao.entrypoint.mapper.SessaoMapper;
import com.votacao.entrypoint.persistence.entity.SessaoEntity;
import com.votacao.entrypoint.persistence.jpa.SpringDataSessaoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
        try {
            SessaoEntity sessaoEntity = mapper.toEntity(sessao);
            SessaoEntity saved = repository.save(sessaoEntity);
            return mapper.toDomain(saved);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicatedSessaoException(sessao.pauta().id());
        }
    }

    @Override
    public boolean existsByIdPauta(long idPauta) {
        return repository.existsByPautaId(idPauta);
    }

    @Override
    public List<Sessao> findAll() {
        return repository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Sessao> findById(long idSessao) {
        return repository.findById(idSessao).map(mapper::toDomain);
    }

    @Override
    public LocalDateTime now() {
        return repository.now();
    }

}

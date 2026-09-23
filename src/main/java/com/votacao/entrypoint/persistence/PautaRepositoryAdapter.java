package com.votacao.entrypoint.persistence;

import com.votacao.application.gateway.PautaRepository;
import com.votacao.application.model.Pauta;
import com.votacao.application.model.exception.DuplicatedPautaException;
import com.votacao.entrypoint.mapper.PautaMapper;
import com.votacao.entrypoint.persistence.entity.PautaEntity;
import com.votacao.entrypoint.persistence.jpa.SpringDataPautaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class PautaRepositoryAdapter implements PautaRepository {

    private final SpringDataPautaRepository repository;
    private final PautaMapper mapper;

    public PautaRepositoryAdapter(SpringDataPautaRepository repository, PautaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Pauta save(Pauta pauta) {
        try {
            PautaEntity saved = repository.save(mapper.toEntity(pauta));
            return mapper.toDomain(saved);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicatedPautaException(pauta.titulo());
        }
    }

    @Override
    public List<Pauta> getPautas() {
        final List<PautaEntity> entities = repository.findAll();
        return entities.stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Pauta> findById(Long pautaId) {
        return repository.findById(pautaId).map(mapper::toDomain);
    }

    @Override
    public boolean existsByTituloIgnoreCase(String titulo) {
        return repository.existsByTituloIgnoreCase(titulo);
    }

}

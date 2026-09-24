package com.votacao.entrypoint.persistence;

import com.votacao.application.gateway.VotoRepository;
import com.votacao.application.model.Voto;
import com.votacao.application.model.exception.DuplicatedVoteException;
import com.votacao.entrypoint.mapper.VotoMapper;
import com.votacao.entrypoint.persistence.entity.VotoEntity;
import com.votacao.entrypoint.persistence.jpa.SpringDataVotoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class VotoRepositoryAdapter implements VotoRepository {

    private final SpringDataVotoRepository repository;
    private final VotoMapper mapper;

    public VotoRepositoryAdapter(SpringDataVotoRepository repository, VotoMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Voto save(Voto voto) {
        try {
            VotoEntity entity = mapper.toEntity(voto);
            VotoEntity saved = repository.save(entity);
            return mapper.toDomain(saved);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicatedVoteException(voto.sessao().id(), voto.documento());
        }
    }

    @Override
    public boolean existsBySessaoIdAndDocumento(Long idSessao, String documento) {
        return repository.existsBySessaoIdAndDocumento(idSessao, documento);
    }

    @Override
    public long countBySessaoIdAndEscolha(Long sessaoId, Voto.Escolha escolha) {
        return repository.countBySessaoIdAndEscolhaVoto(sessaoId, escolha);
    }

}

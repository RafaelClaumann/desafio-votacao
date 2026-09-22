package com.votacao.entrypoint.persistence;

import com.votacao.application.gateway.VotoRepository;
import com.votacao.application.model.DuplicatedVoteException;
import com.votacao.application.model.Voto;
import com.votacao.entrypoint.persistence.entity.VotoEntity;
import com.votacao.entrypoint.persistence.jpa.SpringDataVotoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Component
public class VotoRepositoryAdapter implements VotoRepository {

    private final SpringDataVotoRepository repository;

    public VotoRepositoryAdapter(SpringDataVotoRepository repository) {
        this.repository = repository;
    }

        @Override
        public Voto save(Voto voto) {
            try {
                VotoEntity entity = VotoEntity.fromDomain(voto);
                VotoEntity saved = repository.save(entity);
                return VotoEntity.fromEntity(saved);
            } catch (DataIntegrityViolationException e) {
                throw new DuplicatedVoteException(voto.sessao().id(), voto.documento());
            }
        }

    @Override
    public boolean existsBySessaoIdAndDocumento(Long idSessao, String cpf) {
        return repository.existsBySessaoIdAndDocumento(idSessao, cpf);
    }

}

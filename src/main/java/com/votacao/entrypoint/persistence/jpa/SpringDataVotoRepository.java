package com.votacao.entrypoint.persistence.jpa;

import com.votacao.application.model.Voto;
import com.votacao.entrypoint.persistence.entity.VotoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataVotoRepository extends JpaRepository<VotoEntity, Long> {

    boolean existsBySessaoIdAndDocumento(Long idSessao, String documento);

    long countBySessaoIdAndEscolhaVoto(Long idSessao, Voto.Escolha escolha);

}

package com.votacao.application.gateway;

import com.votacao.application.model.Voto;

public interface VotoRepository {

    Voto save(Voto voto);

    boolean existsBySessaoIdAndDocumento(Long idSessao, String documento);

    long countBySessaoIdAndEscolha(Long idSessao, Voto.Escolha escolha);

}

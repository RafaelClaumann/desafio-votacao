package com.votacao.application.gateway;

import com.votacao.application.model.Voto;

public interface VotoRepository {

    Voto save(Voto voto);

    boolean existsByIdSessaoAndDocumento(Long idSessao, String documento);

    long countByIdSessaoAndEscolha(Long idSessao, Voto.Escolha escolha);

}

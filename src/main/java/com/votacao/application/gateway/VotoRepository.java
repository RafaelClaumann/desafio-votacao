package com.votacao.application.gateway;

import com.votacao.application.model.Voto;

public interface VotoRepository {

    Voto save(Voto voto);

    boolean existsByIdSessaoAndDocumento(long idSessao, String documento);

    long countByIdSessaoAndEscolha(long idSessao, Voto.Escolha escolha);

}

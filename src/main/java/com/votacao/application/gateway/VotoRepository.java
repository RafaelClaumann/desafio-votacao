package com.votacao.application.gateway;

import com.votacao.application.model.Voto;

public interface VotoRepository {

    Voto save(Voto voto);

    boolean existsBySessaoIdAndCpf(Long idSessao, String cpf);

}

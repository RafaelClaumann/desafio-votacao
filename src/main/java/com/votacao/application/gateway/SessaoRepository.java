package com.votacao.application.gateway;

import com.votacao.application.model.Sessao;

public interface SessaoRepository {

    Sessao save(Sessao sessao);

}

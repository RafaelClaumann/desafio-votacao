package com.votacao.application.gateway;

import com.votacao.application.model.Sessao;

import java.util.List;

public interface SessaoRepository {

    Sessao save(Sessao sessao);

    boolean existsByPautaId(Long pautaId);

    List<Sessao> findAll();

}

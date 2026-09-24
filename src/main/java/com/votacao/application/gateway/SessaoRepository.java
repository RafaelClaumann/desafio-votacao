package com.votacao.application.gateway;

import com.votacao.application.model.Sessao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SessaoRepository {

    Sessao save(Sessao sessao);

    boolean existsByPautaId(Long pautaId);

    List<Sessao> findAll();

    Optional<Sessao> findById(Long idSessao);

    LocalDateTime now();

}

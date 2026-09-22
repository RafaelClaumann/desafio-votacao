package com.votacao.application.gateway;

import com.votacao.application.model.Pauta;

import java.util.List;
import java.util.Optional;

public interface PautaRepository {

    Pauta save(Pauta pauta);

    List<Pauta> getPautas();

    Optional<Pauta> findById(Long pautaId);

}

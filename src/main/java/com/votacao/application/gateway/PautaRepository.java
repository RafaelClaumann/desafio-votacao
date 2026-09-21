package com.votacao.application.gateway;

import com.votacao.application.model.Pauta;

import java.util.List;

public interface PautaRepository {

    Pauta save(Pauta pauta);

    List<Pauta> getPautas();

}

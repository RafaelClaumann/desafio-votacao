package com.votacao.application.service.query;

import com.votacao.application.model.Pauta;

/**
 * Representa uma pauta com indicador de associação a uma sessão.
 *
 * @param pauta pauta consultada
 * @param hasSessao indica se a pauta já possui sessão aberta ou cadastrada
 */
public record PautaComStatus(Pauta pauta, boolean hasSessao) {
}

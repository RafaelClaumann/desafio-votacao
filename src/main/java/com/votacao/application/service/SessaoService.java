package com.votacao.application.service;

import com.votacao.application.gateway.PautaRepository;
import com.votacao.application.gateway.SessaoRepository;
import com.votacao.application.model.Pauta;
import com.votacao.application.model.Sessao;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SessaoService {

    private final SessaoRepository sessaoRepository;
    private final PautaRepository pautaRepository;

    public SessaoService(SessaoRepository sessaoRepository, PautaRepository pautaRepository1) {
        this.sessaoRepository = sessaoRepository;
        this.pautaRepository = pautaRepository1;
    }

    public Sessao saveSessao(Long pautaId) {
        Pauta pauta = pautaRepository.findById(pautaId);
        Sessao sessao = new Sessao(null, pauta, LocalDateTime.now(), LocalDateTime.now().plusMinutes(5));
        return sessaoRepository.save(sessao);
    }

}

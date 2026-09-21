package com.votacao.application.service;

import com.votacao.application.gateway.PautaRepository;
import com.votacao.application.gateway.SessaoRepository;
import com.votacao.application.model.Pauta;
import com.votacao.application.model.Sessao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SessaoService {

    private final SessaoRepository sessaoRepository;
    private final PautaRepository pautaRepository;

    public SessaoService(SessaoRepository sessaoRepository, PautaRepository pautaRepository1) {
        this.sessaoRepository = sessaoRepository;
        this.pautaRepository = pautaRepository1;
    }

    @Transactional
    public Sessao saveSessao(Long pautaId) {
        Pauta pauta = pautaRepository.findById(pautaId)
                .orElseThrow(() -> new IllegalArgumentException("Pauta not found"));

        if (sessaoRepository.existsByPautaId(pautaId)) {
            throw new IllegalArgumentException("Sessão already exists for this Pauta");
        }

        Sessao sessao = new Sessao(null, pauta, LocalDateTime.now(), LocalDateTime.now().plusMinutes(5));
        return sessaoRepository.save(sessao);
    }

}

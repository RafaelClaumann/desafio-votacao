package com.votacao.application.service;

import com.votacao.application.gateway.PautaRepository;
import com.votacao.application.gateway.SessaoRepository;
import com.votacao.application.model.Pauta;
import com.votacao.application.model.Sessao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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

        LocalDateTime now = LocalDateTime.now();
        Sessao sessao = new Sessao(
                null,
                pauta,
                now,
                now.plusMinutes(pauta.tempoVotacaoMinutos())
        );
        return sessaoRepository.save(sessao);
    }

    public List<Sessao> getSessoes() {
        return sessaoRepository.findAll();
    }

    public Sessao getOpenSessaoById(Long sessaoId) {
        Sessao sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new IllegalArgumentException("Sessão not found"));

        if (LocalDateTime.now().isAfter(sessao.expiresAt())) {
            throw new IllegalStateException("Sessão is closed");
        }

        return sessao;
    }

}

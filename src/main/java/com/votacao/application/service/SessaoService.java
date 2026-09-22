package com.votacao.application.service;

import com.votacao.application.gateway.SessaoRepository;
import com.votacao.application.model.Pauta;
import com.votacao.application.model.Sessao;
import com.votacao.application.model.SessaoIsClosedException;
import com.votacao.application.model.SessaoNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SessaoService {

    private final SessaoRepository sessaoRepository;
    private final PautaService pautaService;

    public SessaoService(SessaoRepository sessaoRepository, PautaService pautaService) {
        this.sessaoRepository = sessaoRepository;
        this.pautaService = pautaService;
    }

    @Transactional
    public Sessao saveSessao(Long pautaId) {
        Pauta pauta = pautaService.getPautaById(pautaId);

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
                .orElseThrow(() -> new SessaoNotFoundException(sessaoId));

        LocalDateTime now = LocalDateTime.now();
        if (!sessao.isOpen(now)) {
            throw new SessaoIsClosedException(sessaoId);
        }

        return sessao;
    }

}

package com.votacao.application.service;

import com.votacao.application.gateway.SessaoRepository;
import com.votacao.application.model.Pauta;
import com.votacao.application.model.Sessao;
import com.votacao.application.model.exception.DuplicatedSessaoException;
import com.votacao.application.model.exception.SessaoIsClosedException;
import com.votacao.application.model.exception.SessaoIsOpenException;
import com.votacao.application.model.exception.SessaoNotFoundException;
import com.votacao.application.service.query.SessaoComStatus;
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
            throw new DuplicatedSessaoException(pautaId);
        }

        LocalDateTime now = sessaoRepository.now();
        Sessao sessao = new Sessao(
                null,
                pauta,
                now,
                now.plusMinutes(pauta.tempoVotacaoMinutos())
        );
        return sessaoRepository.save(sessao);
    }

    public List<SessaoComStatus> getSessoesComStatus() {
        LocalDateTime now = sessaoRepository.now();
        return sessaoRepository.findAll().stream()
                .map(sessao -> new SessaoComStatus(sessao, sessao.isOpen(now)))
                .toList();
    }

    public Sessao getOpenSessaoById(Long sessaoId) {
        Sessao sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new SessaoNotFoundException(sessaoId));

        if (!sessao.isOpen(sessaoRepository.now())) {
            throw new SessaoIsClosedException(sessaoId);
        }

        return sessao;
    }

    public Sessao getClosedSessaoById(Long sessaoId) {
        Sessao sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new SessaoNotFoundException(sessaoId));

        if (sessao.isOpen(sessaoRepository.now())) {
            throw new SessaoIsOpenException(sessaoId);
        }

        return sessao;
    }

}

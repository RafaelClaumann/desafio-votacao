package com.votacao.application.service;

import com.votacao.application.gateway.SessaoRepository;
import com.votacao.application.model.Pauta;
import com.votacao.application.model.Sessao;
import com.votacao.application.model.exception.DuplicatedSessaoException;
import com.votacao.application.model.exception.SessaoIsClosedException;
import com.votacao.application.model.exception.SessaoIsOpenException;
import com.votacao.application.model.exception.SessaoNotFoundException;
import com.votacao.application.service.query.SessaoComStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SessaoService {

    private static final Logger log = LoggerFactory.getLogger(SessaoService.class);

    private final SessaoRepository sessaoRepository;
    private final PautaService pautaService;

    public SessaoService(SessaoRepository sessaoRepository, PautaService pautaService) {
        this.sessaoRepository = sessaoRepository;
        this.pautaService = pautaService;
    }

    @Transactional
    public Sessao saveSessao(long idPauta) {
        Pauta pauta = pautaService.getPautaById(idPauta);

        if (sessaoRepository.existsByIdPauta(idPauta)) {
            throw new DuplicatedSessaoException(idPauta);
        }

        LocalDateTime now = sessaoRepository.now();

        Sessao sessao = Sessao.registrar(pauta, now, now.plusMinutes(pauta.tempoVotacaoMinutos()));
        return sessaoRepository.save(sessao);
    }

    public List<SessaoComStatus> getSessoesComStatus() {
        LocalDateTime now = sessaoRepository.now();
        return sessaoRepository.findAll().stream()
                .map(sessao -> new SessaoComStatus(sessao, sessao.isOpen(now)))
                .toList();
    }

    public Sessao getOpenSessaoById(long idSessao) {
        Sessao sessao = sessaoRepository.findById(idSessao)
                .orElseThrow(() -> new SessaoNotFoundException(idSessao));

        if (!sessao.isOpen(sessaoRepository.now())) {
            throw new SessaoIsClosedException(idSessao);
        }

        log.info("Retornando Sessao aberta - idSessao: {}", idSessao);
        return sessao;
    }

    public Sessao getClosedSessaoById(long idSessao) {
        Sessao sessao = sessaoRepository.findById(idSessao)
                .orElseThrow(() -> new SessaoNotFoundException(idSessao));

        if (sessao.isOpen(sessaoRepository.now())) {
            throw new SessaoIsOpenException(idSessao);
        }

        return sessao;
    }

}

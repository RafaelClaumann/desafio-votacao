package com.votacao.application.service;

import com.votacao.application.gateway.SessaoRepository;
import com.votacao.application.model.Pauta;
import com.votacao.application.model.Sessao;
import com.votacao.application.model.exception.SessaoIsClosedException;
import com.votacao.application.model.exception.SessaoIsOpenException;
import com.votacao.application.model.exception.SessaoNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SessaoService {

    private final SessaoRepository sessaoRepository;
    private final PautaService pautaService;

    /**
     * Cria uma instância do serviço de sessões.
     *
     * @param sessaoRepository repositório de sessões
     * @param pautaService serviço de pautas
     */
    public SessaoService(SessaoRepository sessaoRepository, PautaService pautaService) {
        this.sessaoRepository = sessaoRepository;
        this.pautaService = pautaService;
    }

    /**
     * Abre uma sessão de votação para uma pauta existente.
     *
     * @param pautaId identificador da pauta
     * @return sessão criada
     */
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

    /**
     * Lista todas as sessões cadastradas.
     *
     * @return lista de sessões
     */
    public List<Sessao> getSessoes() {
        return sessaoRepository.findAll();
    }

    /**
     * Obtém uma sessão aberta por identificador.
     *
     * @param sessaoId identificador da sessão
     * @return sessão aberta
     */
    public Sessao getOpenSessaoById(Long sessaoId) {
        Sessao sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new SessaoNotFoundException(sessaoId));

        LocalDateTime now = LocalDateTime.now();
        if (!sessao.isOpen(now)) {
            throw new SessaoIsClosedException(sessaoId);
        }

        return sessao;
    }

    /**
     * Obtém uma sessão fechada por identificador.
     *
     * @param sessaoId identificador da sessão
     * @return sessão fechada
     */
    public Sessao getClosedSessaoById(Long sessaoId) {
        Sessao sessao = sessaoRepository.findById(sessaoId)
                .orElseThrow(() -> new SessaoNotFoundException(sessaoId));

        LocalDateTime now = LocalDateTime.now();
        if (sessao.isOpen(now)) {
            throw new SessaoIsOpenException(sessaoId);
        }

        return sessao;
    }

}

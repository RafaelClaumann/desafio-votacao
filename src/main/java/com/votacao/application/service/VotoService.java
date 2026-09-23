package com.votacao.application.service;

import com.votacao.application.gateway.VotoRepository;
import com.votacao.application.model.ResultadoVotacao;
import com.votacao.application.model.Sessao;
import com.votacao.application.model.Voto;
import com.votacao.application.model.exception.DuplicatedVoteException;
import com.votacao.application.service.query.ApuracaoSessao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VotoService {

    private final VotoRepository votoRepository;
    private final SessaoService sessaoService;

    /**
     * Cria uma instância do serviço de votos.
     *
     * @param votoRepository repositório de votos
     * @param sessaoService serviço de sessões
     */
    public VotoService(VotoRepository votoRepository, SessaoService sessaoService) {
        this.votoRepository = votoRepository;
        this.sessaoService = sessaoService;
    }

    /**
     * Registra um voto em uma sessão aberta, validando duplicidade por documento.
     *
     * @param idSessao identificador da sessão
     * @param documento documento do eleitor
     * @param escolhaVoto escolha do voto
     * @return voto salvo
     */
    @Transactional
    public Voto votar(Long idSessao, String documento, Voto.Escolha escolhaVoto) {
        Sessao sessao = sessaoService.getOpenSessaoById(idSessao);

        if (votoRepository.existsBySessaoIdAndDocumento(idSessao, documento)) {
            throw new DuplicatedVoteException(idSessao, documento);
        }

        Voto voto = new Voto(null, sessao, documento, escolhaVoto);
        return votoRepository.save(voto);
    }

    /**
     * Calcula a apuração dos votos de uma sessão fechada.
     *
     * @param idSessao identificador da sessão
     * @return dados da apuração da sessão
     */
    public ApuracaoSessao apurarVotosSessao(Long idSessao) {
        sessaoService.getClosedSessaoById(idSessao);
        return new ApuracaoSessao(
                idSessao,
                new ResultadoVotacao(
                        votoRepository.countBySessaoIdAndEscolha(idSessao, Voto.Escolha.SIM),
                        votoRepository.countBySessaoIdAndEscolha(idSessao, Voto.Escolha.NAO)
                )
        );
    }

}

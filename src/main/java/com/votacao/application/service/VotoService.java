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

    public VotoService(VotoRepository votoRepository, SessaoService sessaoService) {
        this.votoRepository = votoRepository;
        this.sessaoService = sessaoService;
    }

    @Transactional
    public Voto votar(Long idSessao, String documento, Voto.Escolha escolhaVoto) {
        String documentoNormalizado = normalizarDocumento(documento);
        Sessao sessao = sessaoService.getOpenSessaoById(idSessao);

        if (votoRepository.existsBySessaoIdAndDocumento(idSessao, documentoNormalizado)) {
            throw new DuplicatedVoteException(idSessao, documentoNormalizado);
        }

        Voto voto = new Voto(null, sessao, documentoNormalizado, escolhaVoto);
        return votoRepository.save(voto);
    }

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

    private static String normalizarDocumento(String documento) {
        return documento.replaceAll("\\D", "");
    }

}

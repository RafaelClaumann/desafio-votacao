package com.votacao.application.service;

import com.votacao.application.gateway.DocumentoValidator;
import com.votacao.application.gateway.VotoRepository;
import com.votacao.application.model.ResultadoVotacao;
import com.votacao.application.model.Sessao;
import com.votacao.application.model.Voto;
import com.votacao.application.model.exception.DuplicatedVoteException;
import com.votacao.application.model.exception.InvalidDocumentoException;
import com.votacao.application.service.query.ApuracaoSessao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VotoService {

    private static final Logger log = LoggerFactory.getLogger(VotoService.class);


    private final VotoRepository votoRepository;
    private final SessaoService sessaoService;
    private final DocumentoValidator documentoValidator;

    public VotoService(
            VotoRepository votoRepository,
            SessaoService sessaoService,
            DocumentoValidator documentoValidator
    ) {
        this.votoRepository = votoRepository;
        this.sessaoService = sessaoService;
        this.documentoValidator = documentoValidator;
    }

    @Transactional
    public Voto votar(Long idSessao, String documento, Voto.Escolha escolhaVoto) {
        String documentoNormalizado = normalizarDocumento(documento);

        Sessao sessao = sessaoService.getOpenSessaoById(idSessao);

        if (!documentoValidator.isValidDocumento(documentoNormalizado)) {
            throw new InvalidDocumentoException(documento);
        }

        if (votoRepository.existsBySessaoIdAndDocumento(idSessao, documentoNormalizado)) {
            throw new DuplicatedVoteException(idSessao, documentoNormalizado);
        }

        log.info("Registrando voto - idSessao: {}", idSessao);
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

package com.votacao.application.service;

import br.com.caelum.stella.format.Formatter;
import com.votacao.application.gateway.DocumentoValidator;
import com.votacao.application.gateway.TimeProvider;
import com.votacao.application.gateway.VotoEventGateway;
import com.votacao.application.gateway.VotoRepository;
import com.votacao.application.model.ResultadoVotacao;
import com.votacao.application.model.Sessao;
import com.votacao.application.model.Voto;
import com.votacao.application.model.exception.DuplicatedVoteException;
import com.votacao.application.model.exception.InvalidDocumentoException;
import com.votacao.application.service.query.ApuracaoSessao;
import com.votacao.application.service.query.VotoPublishData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VotoService {

    private static final Logger log = LoggerFactory.getLogger(VotoService.class);

    private final VotoRepository votoRepository;
    private final SessaoService sessaoService;
    private final VotoEventGateway votoEventGateway;
    private final TimeProvider timeProvider;
    private final Formatter formatter;
    private final DocumentoValidator documentoValidator;

    public VotoService(
            VotoRepository votoRepository,
            SessaoService sessaoService,
            VotoEventGateway votoEventGateway,
            TimeProvider timeProvider,
            Formatter formatter,
            DocumentoValidator documentoValidator
    ) {
        this.votoRepository = votoRepository;
        this.sessaoService = sessaoService;
        this.votoEventGateway = votoEventGateway;
        this.timeProvider = timeProvider;
        this.formatter = formatter;
        this.documentoValidator = documentoValidator;
    }

    @Transactional
    public Voto votar(long idSessao, String documento, Voto.Escolha escolhaVoto) {
        Sessao sessao = sessaoService.getOpenSessaoById(idSessao);

        String documentoNormalizado = formatter.unformat(documento);
        if (!documentoValidator.isValidDocumento(documentoNormalizado)) {
            throw new InvalidDocumentoException(documento);
        }

        if (votoRepository.existsByIdSessaoAndDocumento(idSessao, documentoNormalizado)) {
            throw new DuplicatedVoteException(idSessao, documentoNormalizado);
        }

        Voto voto = Voto.registrar(sessao, documentoNormalizado, escolhaVoto);
        Voto saved = votoRepository.save(voto);

        VotoPublishData votoData = new VotoPublishData(sessao.pauta(), sessao, saved, timeProvider.now());
        votoEventGateway.publish(votoData);
        log.info("Voto registrado - idVoto: {}, idSessao: {}", saved.id(), idSessao);

        return saved;
    }

    public ApuracaoSessao apurarVotosSessao(long idSessao) {
        Sessao sessao = sessaoService.getClosedSessaoById(idSessao);
        return new ApuracaoSessao(
                sessao.id(),
                new ResultadoVotacao(
                        votoRepository.countByIdSessaoAndEscolha(idSessao, Voto.Escolha.SIM),
                        votoRepository.countByIdSessaoAndEscolha(idSessao, Voto.Escolha.NAO)
                )
        );
    }

}

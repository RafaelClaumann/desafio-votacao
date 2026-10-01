package com.votacao.application.service;

import br.com.caelum.stella.format.Formatter;
import com.votacao.application.gateway.DocumentoValidator;
import com.votacao.application.gateway.PublishVotoGateway;
import com.votacao.application.gateway.VotoRepository;
import com.votacao.application.model.ResultadoVotacao;
import com.votacao.application.model.Sessao;
import com.votacao.application.model.Voto;
import com.votacao.application.model.exception.DuplicatedVoteException;
import com.votacao.application.model.exception.InvalidDocumentoException;
import com.votacao.application.service.query.ApuracaoSessao;
import com.votacao.application.service.query.PublishVoto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VotoService {

    private static final Logger log = LoggerFactory.getLogger(VotoService.class);


    private final VotoRepository votoRepository;
    private final SessaoService sessaoService;
    private final PublishVotoGateway publishVotoGateway;
    private final Formatter formatter;
    private final DocumentoValidator documentoValidator;

    public VotoService(
            VotoRepository votoRepository,
            SessaoService sessaoService,
            PublishVotoGateway publishVotoGateway,
            Formatter formatter,
            DocumentoValidator documentoValidator
    ) {
        this.votoRepository = votoRepository;
        this.sessaoService = sessaoService;
        this.publishVotoGateway = publishVotoGateway;
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

        publishMessage(sessao, saved);
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

    private void publishMessage(Sessao sessao, Voto voto) {
        publishVotoGateway.publishVoto(
                PublishVoto.builder()
                        .idSessao(sessao.id())
                        .idPauta(sessao.pauta().id())
                        .idVoto(voto.id())
                        .documento(voto.documento())
                        .tituloPauta(sessao.pauta().titulo())
                        .escolhaVoto(voto.escolhaVoto().name())
                        .publishedAt(sessaoService.now())
                        .build()
        );
    }

}

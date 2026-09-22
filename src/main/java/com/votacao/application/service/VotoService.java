package com.votacao.application.service;

import com.votacao.application.gateway.VotoRepository;
import com.votacao.application.model.DuplicatedVoteException;
import com.votacao.application.model.Sessao;
import com.votacao.application.model.Voto;
import org.springframework.stereotype.Service;

@Service
public class VotoService {

    private final VotoRepository votoRepository;
    private final SessaoService sessaoService;

    public VotoService(VotoRepository votoRepository, SessaoService sessaoService) {
        this.votoRepository = votoRepository;
        this.sessaoService = sessaoService;
    }

    public Voto votar(Long idSessao, String documento, String escolhaVoto) {
        Sessao sessao = sessaoService.getOpenSessaoById(idSessao);

        if (votoRepository.existsBySessaoIdAndDocumento(idSessao, documento)) {
            throw new DuplicatedVoteException(idSessao, documento);
        }

        Voto voto = new Voto(null, sessao, documento, Voto.Escolha.valueOf(escolhaVoto));
        return votoRepository.save(voto);
    }

}

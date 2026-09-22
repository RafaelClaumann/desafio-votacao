package com.votacao.application.service;

import com.votacao.application.gateway.VotoRepository;
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

        Voto voto = null;
        try {
            Sessao openSessaoById = sessaoService.getOpenSessaoById(idSessao);
            voto = new Voto(null, openSessaoById, documento, Voto.Escolha.valueOf(escolhaVoto));
        } catch (Exception e) {
            throw new IllegalStateException("Sessão is closed or not found");
        }

        return votoRepository.save(voto);
    }

}

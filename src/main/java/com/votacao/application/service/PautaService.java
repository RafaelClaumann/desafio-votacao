package com.votacao.application.service;

import com.votacao.application.gateway.PautaRepository;
import com.votacao.application.gateway.SessaoRepository;
import com.votacao.application.model.Pauta;
import com.votacao.application.model.exception.DuplicatedPautaException;
import com.votacao.application.model.exception.PautaNotFoundException;
import com.votacao.application.service.query.PautaComStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class PautaService {

    private final PautaRepository repository;
    private final SessaoRepository sessaoRepository;

    public PautaService(PautaRepository repository, SessaoRepository sessaoRepository) {
        this.repository = repository;
        this.sessaoRepository = sessaoRepository;
    }

    public Pauta savePauta(final Pauta pauta) {
        if (repository.existsByTituloIgnoreCase(pauta.titulo())) {
            throw new DuplicatedPautaException(pauta.titulo());
        }
        return repository.save(pauta);
    }

    public Pauta getPautaById(Long pautaId) {
        return repository.findById(pautaId).orElseThrow(() -> new PautaNotFoundException(pautaId));
    }

    public List<Pauta> getPautas() {
        return repository.getPautas();
    }

    public List<PautaComStatus> pautaComStatuses() {
        List<Pauta> pautas = getPautas();
        List<Long> ids = pautas.stream().map(Pauta::id).toList();
        Set<Long> pautaIdsComSessao = sessaoRepository.findPautaIdsComSessao(ids);

        return pautas.stream()
                .map(pauta -> new PautaComStatus(
                        pauta,
                        pautaIdsComSessao.contains(pauta.id()))
                )
                .toList();
    }

}

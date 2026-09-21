package com.votacao.entrypoint.persistence;

import com.votacao.entrypoint.persistence.entity.SessaoEntity;
import com.votacao.entrypoint.persistence.jpa.SpringDataSessaoRepository;
import org.springframework.stereotype.Component;

@Component
public class SessaoRepositoryAdapter {

    private final SpringDataSessaoRepository repository;

    public SessaoRepositoryAdapter(SpringDataSessaoRepository repository) {
        this.repository = repository;
    }

    public void createSession(SessaoEntity sessaoEntity) {
        repository.save(sessaoEntity);
    }

}

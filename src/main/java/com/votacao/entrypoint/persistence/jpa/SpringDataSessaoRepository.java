package com.votacao.entrypoint.persistence.jpa;

import com.votacao.entrypoint.persistence.entity.SessaoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataSessaoRepository extends JpaRepository<SessaoEntity, Long> {
}
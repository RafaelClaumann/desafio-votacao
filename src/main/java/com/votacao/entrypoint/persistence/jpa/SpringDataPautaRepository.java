package com.votacao.entrypoint.persistence.jpa;

import com.votacao.entrypoint.persistence.entity.PautaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataPautaRepository extends JpaRepository<PautaEntity, Long> {
}
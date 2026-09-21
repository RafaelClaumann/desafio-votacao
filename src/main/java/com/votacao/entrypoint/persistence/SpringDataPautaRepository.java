package com.votacao.entrypoint.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataPautaRepository extends JpaRepository<PautaEntity, Long> {
}
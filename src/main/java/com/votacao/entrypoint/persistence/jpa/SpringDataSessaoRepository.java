package com.votacao.entrypoint.persistence.jpa;

import com.votacao.entrypoint.persistence.entity.SessaoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;

public interface SpringDataSessaoRepository extends JpaRepository<SessaoEntity, Long> {

    boolean existsByPautaId(Long pautaId);

    @Query("SELECT CURRENT_TIMESTAMP")
    LocalDateTime now();

}

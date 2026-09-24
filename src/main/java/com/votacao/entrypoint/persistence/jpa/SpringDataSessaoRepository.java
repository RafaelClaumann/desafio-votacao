package com.votacao.entrypoint.persistence.jpa;

import com.votacao.entrypoint.persistence.entity.SessaoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface SpringDataSessaoRepository extends JpaRepository<SessaoEntity, Long> {

    boolean existsByPautaId(Long pautaId);

    @Query("SELECT s.pauta.id FROM SessaoEntity s WHERE s.pauta.id IN :pautaIds")
    Set<Long> findPautaIdsComSessao(@Param("pautaIds") List<Long> pautaIds);

    @Query("SELECT CURRENT_TIMESTAMP")
    LocalDateTime now();

}

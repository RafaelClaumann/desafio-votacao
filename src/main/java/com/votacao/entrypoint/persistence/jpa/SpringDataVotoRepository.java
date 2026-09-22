package com.votacao.entrypoint.persistence.jpa;

import com.votacao.entrypoint.persistence.entity.VotoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataVotoRepository extends JpaRepository<VotoEntity, Long> {

}

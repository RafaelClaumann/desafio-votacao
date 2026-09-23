package com.votacao.entrypoint.persistence.entity;

import com.votacao.application.model.Sessao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "sessoes")
public class SessaoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "pauta_id", nullable = false)
    private PautaEntity pauta;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public static SessaoEntity fromDomain(Sessao sessao) {
        SessaoEntity entity = new SessaoEntity();

        entity.id = sessao.id();
        entity.pauta = PautaEntity.fromDomain(sessao.pauta());
        entity.startedAt = sessao.startedAt();
        entity.expiresAt = sessao.expiresAt();

        return entity;
    }

    public static Sessao fromEntity(SessaoEntity entity) {
        return new Sessao(entity.id, PautaEntity.fromEntity(entity.pauta), entity.startedAt, entity.expiresAt);
    }

}

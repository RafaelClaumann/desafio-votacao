package com.votacao.entrypoint.persistence.entity;

import com.votacao.application.model.Voto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "votos",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_voto_sessao_documento",
                columnNames = {"sessao_id", "documento"}
        )
)
public class VotoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sessao_id", nullable = false)
    private SessaoEntity sessao;

    @Column(nullable = false, length = 14)
    private String documento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Voto.Escolha escolhaVoto;

}

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
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Entity
@Table(name = "votos")
public class VotoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sessao_id", nullable = false)
    private SessaoEntity sessao;

    @Column(nullable = false, unique = true, length = 14)
    private String documento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EscolhaVoto escolhaVoto;

    public static VotoEntity fromDomain(Voto voto) {
        VotoEntity votoEntity = new VotoEntity();
        votoEntity.setId(voto.id());
        votoEntity.setSessao(SessaoEntity.fromDomain(voto.sessao()));
        votoEntity.setDocumento(voto.documento());
        votoEntity.setEscolhaVoto(EscolhaVoto.valueOf(voto.escolhaVoto().name()));
        return votoEntity;
    }

    public static Voto fromEntity(VotoEntity saved) {
        return new Voto(
                saved.getId(),
                SessaoEntity.fromEntity(saved.getSessao()),
                saved.getDocumento(),
                Voto.Escolha.valueOf(saved.getEscolhaVoto().name())
        );
    }

    public enum EscolhaVoto {
        SIM,
        NAO
    }

}

package com.votacao.entrypoint.persistence.entity;

import com.votacao.application.model.Pauta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "pautas")
public class PautaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(name = "tempo_votacao_segundos", nullable = false)
    private Long tempoVotacaoMinutos = 1L;

    public PautaEntity() {
    }

    public PautaEntity(String titulo, Long tempoVotacaoMinutos) {
        this.titulo = titulo;
        this.tempoVotacaoMinutos = tempoVotacaoMinutos;
    }

    public static PautaEntity fromDomain(Pauta domain) {
        PautaEntity pautaEntity = new PautaEntity();
        pautaEntity.id = domain.id();
        pautaEntity.titulo = domain.titulo();
        pautaEntity.tempoVotacaoMinutos = domain.tempoVotacaoMinutos();
        return pautaEntity;
    }

    public static Pauta fromEntity(PautaEntity entity) {
        return new Pauta(entity.id, entity.titulo, entity.tempoVotacaoMinutos);
    }

}

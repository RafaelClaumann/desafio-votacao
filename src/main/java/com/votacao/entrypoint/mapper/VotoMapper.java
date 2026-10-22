package com.votacao.entrypoint.mapper;

import com.votacao.application.model.Voto;
import com.votacao.application.service.query.VotoPublishData;
import com.votacao.entrypoint.api.dto.VotoResponseDTO;
import com.votacao.entrypoint.kafka.VotoEvent;
import com.votacao.entrypoint.persistence.entity.VotoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = SessaoMapper.class)
public interface VotoMapper {

    Voto toDomain(VotoEntity entity);

    VotoEntity toEntity(Voto voto);

    @Mapping(target = "idSessao", source = "sessao.id")
    @Mapping(target = "idPauta", source = "sessao.pauta.id")
    @Mapping(target = "tituloPauta", source = "sessao.pauta.titulo")
    @Mapping(target = "startedAt", source = "sessao.startedAt")
    @Mapping(target = "expiresAt", source = "sessao.expiresAt")
    VotoResponseDTO toResponse(Voto voto);

    @Mapping(target = "idPauta", source = "pauta.id")
    @Mapping(target = "titulo", source = "pauta.titulo")
    @Mapping(target = "tempoVotacaoMinutos", source = "pauta.tempoVotacaoMinutos")
    @Mapping(target = "idSessao", source = "sessao.id")
    @Mapping(target = "startedAt", source = "sessao.startedAt")
    @Mapping(target = "expiresAt", source = "sessao.expiresAt")
    @Mapping(target = "idVoto", source = "voto.id")
    @Mapping(target = "documento", source = "voto.documento")
    @Mapping(target = "escolhaVoto", source = "voto.escolhaVoto")
    VotoEvent toEvent(VotoPublishData voto);

}

package com.votacao.entrypoint.mapper;

import com.votacao.application.model.Sessao;
import com.votacao.application.service.query.ApuracaoSessao;
import com.votacao.application.service.query.SessaoComStatus;
import com.votacao.entrypoint.api.dto.ResultadoVotacaoResponse;
import com.votacao.entrypoint.api.dto.SessaoResponseDTO;
import com.votacao.entrypoint.persistence.entity.SessaoEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SessaoMapper {

    SessaoEntity toEntity(Sessao sessao);

    Sessao toDomain(SessaoEntity entity);

    @Mapping(target = "id", source = "sessao.id")
    @Mapping(target = "idPauta", source = "sessao.pauta.id")
    @Mapping(target = "tituloPauta", source = "sessao.pauta.titulo")
    @Mapping(target = "startedAt", source = "sessao.startedAt")
    @Mapping(target = "expiresAt", source = "sessao.expiresAt")
    @Mapping(target = "isOpen", source = "isOpen")
    SessaoResponseDTO toDTO(SessaoComStatus sessaoComStatus);

    List<SessaoResponseDTO> toDTOList(List<SessaoComStatus> sessoesComStatus);

    @Mapping(target = "idSessao", source = "idSessao")
    @Mapping(target = "votosSim", source = "resultado.totalVotosSim")
    @Mapping(target = "votosNao", source = "resultado.totalVotosNao")
    @Mapping(target = "total", expression = "java(apuracao.resultado().totalVotos())")
    @Mapping(target = "status", expression = "java(apuracao.resultado().status())")
    ResultadoVotacaoResponse toResponse(ApuracaoSessao apuracao);

}

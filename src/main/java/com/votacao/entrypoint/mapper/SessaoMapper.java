package com.votacao.entrypoint.mapper;

import com.votacao.application.model.Sessao;
import com.votacao.application.service.query.ApuracaoSessao;
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

    @Mapping(target = "idSessao", source = "id")
    @Mapping(target = "idPauta", source = "pauta.id")
    @Mapping(target = "isOpen", expression = "java(sessao.isOpen(java.time.LocalDateTime.now()))")
    SessaoResponseDTO toDTO(Sessao sessao);

    List<SessaoResponseDTO> toDTOList(List<Sessao> sessoes);

    @Mapping(target = "idSessao", source = "idSessao")
    @Mapping(target = "votosSim", source = "resultado.totalVotosSim")
    @Mapping(target = "votosNao", source = "resultado.totalVotosNao")
    @Mapping(target = "total", expression = "java(apuracao.resultado().totalVotos())")
    @Mapping(target = "status", expression = "java(apuracao.resultado().status())")
    ResultadoVotacaoResponse toResponse(ApuracaoSessao apuracao);

}

package com.votacao.entrypoint.mapper;

import com.votacao.application.model.Sessao;
import com.votacao.entrypoint.api.dto.SessaoResponseDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SessaoResponseMapper {

    @Mapping(target = "idSessao", source = "id")
    @Mapping(target = "idPauta", source = "pauta.id")
    @Mapping(target = "isOpen", expression = "java(sessao.isOpen(java.time.LocalDateTime.now()))")
    SessaoResponseDTO toDTO(Sessao sessao);

    List<SessaoResponseDTO> toDTOList(List<Sessao> sessoes);

}
